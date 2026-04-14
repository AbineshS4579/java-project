package com.complaints.dao;

import com.complaints.model.*;
import com.complaints.util.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ComplaintDAO {

    private static final Logger log = LoggerFactory.getLogger(ComplaintDAO.class);

    private static final String BASE_SELECT = """
        SELECT c.*,
               u.name  AS user_name,
               a.name  AS assigned_to_name,
               d.name  AS dept_name
        FROM   complaints c
        JOIN   users       u ON u.id = c.user_id
        LEFT JOIN users    a ON a.id = c.assigned_to
        LEFT JOIN departments d ON d.id = c.department_id
        """;

    // ── Create ────────────────────────────────
    public Complaint create(int userId, String title, String desc,
                            String category, String priority) throws SQLException {
        String sql = """
            INSERT INTO complaints (user_id, title, description, category, priority)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, title);
            ps.setString(3, desc);
            ps.setString(4, category);
            ps.setString(5, priority);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int newId = rs.getInt(1);
                    addHistory(conn, newId, userId, null, "pending", "Complaint submitted.");
                    return findById(newId).orElseThrow();
                }
            }
        }
        throw new SQLException("Complaint creation failed.");
    }

    // ── Find by ID ────────────────────────────
    public Optional<Complaint> findById(int id) throws SQLException {
        String sql = BASE_SELECT + " WHERE c.id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Complaint c = map(rs);
                    c.setHistory(getHistory(id));
                    c.setComments(getComments(id, false));
                    return Optional.of(c);
                }
            }
        }
        return Optional.empty();
    }

    // ── Find by user ──────────────────────────
    public List<Complaint> findByUser(int userId) throws SQLException {
        String sql = BASE_SELECT + " WHERE c.user_id = ? ORDER BY c.created_at DESC";
        return query(sql, ps -> ps.setInt(1, userId));
    }

    // ── Find all (admin) ──────────────────────
    public List<Complaint> findAll(String status, String priority) throws SQLException {
        StringBuilder sb = new StringBuilder(BASE_SELECT + " WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (status   != null && !status.isBlank())   { sb.append(" AND c.status = ?");   params.add(status); }
        if (priority != null && !priority.isBlank())  { sb.append(" AND c.priority = ?"); params.add(priority); }
        sb.append(" ORDER BY c.created_at DESC");
        return query(sb.toString(), ps -> {
            for (int i = 0; i < params.size(); i++) ps.setString(i + 1, params.get(i).toString());
        });
    }

    // ── Update status ─────────────────────────
    public Complaint updateStatus(int complaintId, int changedBy,
                                  String newStatus, String note) throws SQLException {
        String sql = "UPDATE complaints SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection()) {
            // get old status
            String oldStatus = null;
            try (PreparedStatement ps = conn.prepareStatement("SELECT status FROM complaints WHERE id = ?")) {
                ps.setInt(1, complaintId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) oldStatus = rs.getString("status");
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, newStatus);
                ps.setInt(2, complaintId);
                ps.executeUpdate();
            }
            addHistory(conn, complaintId, changedBy, oldStatus, newStatus, note);
        }
        return findById(complaintId).orElseThrow();
    }

    // ── Assign ────────────────────────────────
    public Complaint assign(int complaintId, Integer agentId,
                            Integer deptId, int changedBy) throws SQLException {
        String sql = "UPDATE complaints SET assigned_to = ?, department_id = ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (agentId != null) ps.setInt(1, agentId); else ps.setNull(1, Types.INTEGER);
            if (deptId  != null) ps.setInt(2, deptId);  else ps.setNull(2, Types.INTEGER);
            ps.setInt(3, complaintId);
            ps.executeUpdate();
            addHistory(conn, complaintId, changedBy, null, "in_progress", "Complaint assigned.");
        }
        return updateStatus(complaintId, changedBy, "in_progress", "Assigned to agent/department.");
    }

    // ── Resolve ───────────────────────────────
    public Complaint resolve(int complaintId, int adminId, String resolution) throws SQLException {
        String sql = "UPDATE complaints SET resolution = ?, status = 'resolved' WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, resolution);
            ps.setInt(2, complaintId);
            ps.executeUpdate();
            addHistory(conn, complaintId, adminId, "in_progress", "resolved", resolution);
        }
        return findById(complaintId).orElseThrow();
    }

    // ── Archive / delete ──────────────────────
    public void archive(int id, int adminId) throws SQLException {
        String sql = "UPDATE complaints SET status = 'archived' WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            addHistory(conn, id, adminId, null, "archived", "Complaint archived.");
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM complaints WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ── Comments ──────────────────────────────
    public Comment addComment(int complaintId, int userId,
                              String body, boolean internal) throws SQLException {
        String sql = "INSERT INTO comments (complaint_id, user_id, body, is_internal) VALUES (?,?,?,?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, complaintId);
            ps.setInt(2, userId);
            ps.setString(3, body);
            ps.setBoolean(4, internal);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int cid = rs.getInt(1);
                    String q = """
                        SELECT cm.*, u.name AS user_name, u.role AS user_role
                        FROM   comments cm
                        JOIN   users u ON u.id = cm.user_id
                        WHERE  cm.id = ?
                        """;
                    try (PreparedStatement ps2 = conn.prepareStatement(q)) {
                        ps2.setInt(1, cid);
                        try (ResultSet rs2 = ps2.executeQuery()) {
                            if (rs2.next()) return mapComment(rs2);
                        }
                    }
                }
            }
        }
        throw new SQLException("Comment insert failed.");
    }

    public List<Comment> getComments(int complaintId, boolean includeInternal) throws SQLException {
        List<Comment> list = new ArrayList<>();
        String sql = """
            SELECT cm.*, u.name AS user_name, u.role AS user_role
            FROM   comments cm
            JOIN   users u ON u.id = cm.user_id
            WHERE  cm.complaint_id = ?
            """ + (includeInternal ? "" : " AND cm.is_internal = 0") + """
            ORDER BY cm.created_at ASC
            """;
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, complaintId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapComment(rs));
            }
        }
        return list;
    }

    // ── Statistics ────────────────────────────
    public java.util.Map<String, Long> getStats() throws SQLException {
        java.util.Map<String, Long> stats = new java.util.LinkedHashMap<>();
        String sql = "SELECT status, COUNT(*) AS cnt FROM complaints GROUP BY status";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) stats.put(rs.getString("status"), rs.getLong("cnt"));
        }
        return stats;
    }

    // ── Private helpers ───────────────────────
    private void addHistory(Connection conn, int complaintId, int userId,
                            String oldStatus, String newStatus, String note) throws SQLException {
        String sql = """
            INSERT INTO status_history (complaint_id, changed_by, old_status, new_status, note)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, complaintId);
            ps.setInt(2, userId);
            if (oldStatus != null) ps.setString(3, oldStatus); else ps.setNull(3, Types.VARCHAR);
            ps.setString(4, newStatus);
            if (note != null) ps.setString(5, note); else ps.setNull(5, Types.VARCHAR);
            ps.executeUpdate();
        }
    }

    private List<StatusHistory> getHistory(int complaintId) throws SQLException {
        List<StatusHistory> list = new ArrayList<>();
        String sql = """
            SELECT sh.*, u.name AS changed_by_name
            FROM   status_history sh
            JOIN   users u ON u.id = sh.changed_by
            WHERE  sh.complaint_id = ?
            ORDER BY sh.changed_at ASC
            """;
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, complaintId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StatusHistory h = new StatusHistory();
                    h.setId(rs.getInt("id"));
                    h.setComplaintId(complaintId);
                    h.setChangedBy(rs.getInt("changed_by"));
                    h.setChangedByName(rs.getString("changed_by_name"));
                    h.setOldStatus(rs.getString("old_status"));
                    h.setNewStatus(rs.getString("new_status"));
                    h.setNote(rs.getString("note"));
                    h.setChangedAt(rs.getTimestamp("changed_at"));
                    list.add(h);
                }
            }
        }
        return list;
    }

    @FunctionalInterface
    interface Binder { void bind(PreparedStatement ps) throws SQLException; }

    private List<Complaint> query(String sql, Binder binder) throws SQLException {
        List<Complaint> list = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    private Complaint map(ResultSet rs) throws SQLException {
        Complaint c = new Complaint();
        c.setId(rs.getInt("id"));
        c.setTitle(rs.getString("title"));
        c.setDescription(rs.getString("description"));
        c.setCategory(rs.getString("category"));
        c.setPriority(rs.getString("priority"));
        c.setStatus(rs.getString("status"));
        c.setUserId(rs.getInt("user_id"));
        c.setUserName(rs.getString("user_name"));
        c.setResolution(rs.getString("resolution"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        c.setUpdatedAt(rs.getTimestamp("updated_at"));

        int assignedTo = rs.getInt("assigned_to");
        if (!rs.wasNull()) {
            c.setAssignedTo(assignedTo);
            c.setAssignedToName(rs.getString("assigned_to_name"));
        }
        int deptId = rs.getInt("department_id");
        if (!rs.wasNull()) {
            c.setDepartmentId(deptId);
            c.setDepartmentName(rs.getString("dept_name"));
        }
        return c;
    }

    private Comment mapComment(ResultSet rs) throws SQLException {
        Comment c = new Comment();
        c.setId(rs.getInt("id"));
        c.setComplaintId(rs.getInt("complaint_id"));
        c.setUserId(rs.getInt("user_id"));
        c.setUserName(rs.getString("user_name"));
        c.setUserRole(rs.getString("user_role"));
        c.setBody(rs.getString("body"));
        c.setInternal(rs.getBoolean("is_internal"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        return c;
    }
}
