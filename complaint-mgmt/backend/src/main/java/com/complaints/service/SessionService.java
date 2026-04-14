package com.complaints.service;

import com.complaints.model.User;
import com.complaints.util.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;
import java.util.Optional;

/**
 * Manages server-side session tokens stored in the sessions table.
 * Tokens are 256-bit random values, valid for 8 hours.
 */
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);
    private static final int    TOKEN_BYTES  = 32;           // 256 bits
    private static final long   TTL_HOURS    = 8;
    private static final SecureRandom RNG = new SecureRandom();

    // ── Create session ────────────────────────
    public String createSession(int userId) throws SQLException {
        String token = generateToken();
        Timestamp expires = new Timestamp(
            System.currentTimeMillis() + TTL_HOURS * 3_600_000L);

        String sql = "INSERT INTO sessions (token, user_id, expires_at) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setInt(2, userId);
            ps.setTimestamp(3, expires);
            ps.executeUpdate();
        }
        return token;
    }

    // ── Validate + return user ────────────────
    public Optional<User> validate(String token) throws SQLException {
        if (token == null || token.isBlank()) return Optional.empty();

        String sql = """
            SELECT u.*
            FROM   sessions s
            JOIN   users    u ON u.id = s.user_id
            WHERE  s.token = ?
              AND  s.expires_at > NOW()
              AND  u.is_active = 1
            """;
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setName(rs.getString("name"));
                u.setEmail(rs.getString("email"));
                u.setRole(rs.getString("role"));
                u.setDepartment(rs.getString("department"));
                u.setActive(rs.getBoolean("is_active"));
                return Optional.of(u);
            }
        }
    }

    // ── Destroy session ───────────────────────
    public void invalidate(String token) throws SQLException {
        if (token == null) return;
        String sql = "DELETE FROM sessions WHERE token = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.executeUpdate();
        }
    }

    // ── Purge expired sessions ────────────────
    public void purgeExpired() throws SQLException {
        String sql = "DELETE FROM sessions WHERE expires_at <= NOW()";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int deleted = ps.executeUpdate();
            if (deleted > 0) log.info("Purged {} expired sessions.", deleted);
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
