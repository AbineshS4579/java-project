package com.complaints.controller;

import com.complaints.dao.ComplaintDAO;
import com.complaints.dao.UserDAO;
import com.complaints.model.Comment;
import com.complaints.model.Complaint;
import com.complaints.model.User;
import com.complaints.service.SessionService;
import com.complaints.util.DatabaseUtil;
import com.complaints.util.JsonUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;

public class ApiServer {

    private static final int PORT = 8080;

    private final HttpServer     server;
    private final UserDAO        userDAO      = new UserDAO();
    private final ComplaintDAO   complaintDAO = new ComplaintDAO();
    private final SessionService sessions     = new SessionService();

    public ApiServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(Executors.newFixedThreadPool(8));
        registerContexts();
    }

    public void start() { server.start(); }
    public void stop()  { server.stop(2); }

    private void registerContexts() {
        server.createContext("/api/auth/register", this::handleRegister);
        server.createContext("/api/auth/login",    this::handleLogin);
        server.createContext("/api/auth/logout",   this::handleLogout);
        server.createContext("/api/auth/me",       this::handleMe);
        server.createContext("/api/complaints/",   this::handleComplaintById);
        server.createContext("/api/complaints",    this::handleComplaints);
        server.createContext("/api/admin/complaints", this::handleAdminComplaints);
        server.createContext("/api/admin/users",      this::handleAdminUsers);
        server.createContext("/api/admin/stats",      this::handleStats);
        server.createContext("/api/departments",      this::handleDepartments);
    }

    private void cors(HttpExchange ex) {
        Headers h = ex.getResponseHeaders();
        h.set("Access-Control-Allow-Origin",  "*");
        h.set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        h.set("Access-Control-Allow-Headers", "Authorization,Content-Type,Accept");
    }

    private boolean handleOptions(HttpExchange ex) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            cors(ex);
            ex.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    private void handleRegister(HttpExchange ex) throws IOException {
        cors(ex);
        if (handleOptions(ex)) return;
        if (!"POST".equals(ex.getRequestMethod())) { send(ex, 405, JsonUtil.error(405, "Method not allowed")); return; }
        try {
            JsonObject body = parseBody(ex);
            String name  = req(body, "name");
            String email = req(body, "email").toLowerCase();
            String pass  = req(body, "password");
            if (name.isBlank() || email.isBlank() || pass.length() < 8) {
                send(ex, 400, JsonUtil.error(400, "Name, email and password (8+ chars) required.")); return;
            }
            if (userDAO.findByEmail(email).isPresent()) {
                send(ex, 409, JsonUtil.error(409, "Email already registered.")); return;
            }
            User user = userDAO.create(name, email, pass, "user");
            String token = sessions.createSession(user.getId());
            JsonObject res = JsonUtil.GSON.toJsonTree(user).getAsJsonObject();
            res.addProperty("token", token);
            send(ex, 201, JsonUtil.ok(res));
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, JsonUtil.error(500, "Registration failed: " + e.getMessage()));
        }
    }

    private void handleLogin(HttpExchange ex) throws IOException {
        cors(ex);
        if (handleOptions(ex)) return;
        if (!"POST".equals(ex.getRequestMethod())) { send(ex, 405, JsonUtil.error(405, "Method not allowed")); return; }
        try {
            JsonObject body = parseBody(ex);
            String email = req(body, "email").toLowerCase();
            String pass  = req(body, "password");
            Optional<User> opt = userDAO.authenticate(email, pass);
            if (opt.isEmpty()) { send(ex, 401, JsonUtil.error(401, "Invalid credentials.")); return; }
            User user  = opt.get();
            String tok = sessions.createSession(user.getId());
            JsonObject res = JsonUtil.GSON.toJsonTree(user).getAsJsonObject();
            res.addProperty("token", tok);
            send(ex, 200, JsonUtil.ok(res));
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, JsonUtil.error(500, "Login failed: " + e.getMessage()));
        }
    }

    private void handleLogout(HttpExchange ex) throws IOException {
        cors(ex);
        if (handleOptions(ex)) return;
        try {
            sessions.invalidate(bearerToken(ex));
            send(ex, 200, JsonUtil.ok("Logged out."));
        } catch (Exception e) {
            send(ex, 500, JsonUtil.error(500, "Logout error."));
        }
    }

    private void handleMe(HttpExchange ex) throws IOException {
        cors(ex);
        if (handleOptions(ex)) return;
        try {
            Optional<User> user = authenticate(ex);
            if (user.isEmpty()) { send(ex, 401, JsonUtil.error(401, "Unauthorized.")); return; }
            send(ex, 200, JsonUtil.ok(user.get()));
        } catch (Exception e) {
            send(ex, 500, JsonUtil.error(500, "Error."));
        }
    }

    private void handleComplaints(HttpExchange ex) throws IOException {
        cors(ex);
        if (handleOptions(ex)) return;
        try {
            Optional<User> auth = authenticate(ex);
            if (auth.isEmpty()) { send(ex, 401, JsonUtil.error(401, "Unauthorized.")); return; }
            User user = auth.get();
            if ("GET".equals(ex.getRequestMethod())) {
                send(ex, 200, JsonUtil.ok(complaintDAO.findByUser(user.getId())));
            } else if ("POST".equals(ex.getRequestMethod())) {
                JsonObject body = parseBody(ex);
                String title    = req(body, "title");
                String desc     = req(body, "description");
                String category = body.has("category") ? body.get("category").getAsString() : "General";
                String priority = body.has("priority") ? body.get("priority").getAsString() : "medium";
                if (title.isBlank() || desc.isBlank()) {
                    send(ex, 400, JsonUtil.error(400, "Title and description required.")); return;
                }
                Complaint c = complaintDAO.create(user.getId(), title, desc, category, priority);
                send(ex, 201, JsonUtil.ok(c));
            } else {
                send(ex, 405, JsonUtil.error(405, "Method not allowed"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, JsonUtil.error(500, "Server error: " + e.getMessage()));
        }
    }

    private void handleComplaintById(HttpExchange ex) throws IOException {
        cors(ex);
        if (handleOptions(ex)) return;
        try {
            Optional<User> auth = authenticate(ex);
            if (auth.isEmpty()) { send(ex, 401, JsonUtil.error(401, "Unauthorized.")); return; }
            User user = auth.get();
            String path = ex.getRequestURI().getPath();
            String[] parts = path.split("/");
            if (parts.length < 4) { send(ex, 400, JsonUtil.error(400, "Bad path.")); return; }
            int id = Integer.parseInt(parts[3]);
            boolean isComment = parts.length == 5 && "comments".equals(parts[4]);
            boolean isStatus  = parts.length == 5 && "status".equals(parts[4]);
            boolean isAssign  = parts.length == 5 && "assign".equals(parts[4]);
            boolean isResolve = parts.length == 5 && "resolve".equals(parts[4]);
            boolean isArchive = parts.length == 5 && "archive".equals(parts[4]);
            Optional<Complaint> opt = complaintDAO.findById(id);
            if (opt.isEmpty()) { send(ex, 404, JsonUtil.error(404, "Not found.")); return; }
            Complaint c = opt.get();
            if (!user.isAdmin() && c.getUserId() != user.getId()) { send(ex, 403, JsonUtil.error(403, "Forbidden.")); return; }
            if (isComment && "POST".equals(ex.getRequestMethod())) {
                JsonObject body = parseBody(ex);
                boolean internal = user.isAdmin() && body.has("internal") && body.get("internal").getAsBoolean();
                send(ex, 201, JsonUtil.ok(complaintDAO.addComment(id, user.getId(), req(body, "body"), internal)));
            } else if (isStatus && "PUT".equals(ex.getRequestMethod())) {
                if (!user.isAdmin()) { send(ex, 403, JsonUtil.error(403, "Admin only.")); return; }
                JsonObject body = parseBody(ex);
                String note = body.has("note") ? body.get("note").getAsString() : null;
                send(ex, 200, JsonUtil.ok(complaintDAO.updateStatus(id, user.getId(), req(body, "status"), note)));
            } else if (isAssign && "PUT".equals(ex.getRequestMethod())) {
                if (!user.isAdmin()) { send(ex, 403, JsonUtil.error(403, "Admin only.")); return; }
                JsonObject body = parseBody(ex);
                Integer agentId = body.has("agentId") && !body.get("agentId").isJsonNull() ? body.get("agentId").getAsInt() : null;
                Integer deptId  = body.has("deptId")  && !body.get("deptId").isJsonNull()  ? body.get("deptId").getAsInt()  : null;
                send(ex, 200, JsonUtil.ok(complaintDAO.assign(id, agentId, deptId, user.getId())));
            } else if (isResolve && "PUT".equals(ex.getRequestMethod())) {
                if (!user.isAdmin()) { send(ex, 403, JsonUtil.error(403, "Admin only.")); return; }
                JsonObject body = parseBody(ex);
                send(ex, 200, JsonUtil.ok(complaintDAO.resolve(id, user.getId(), req(body, "resolution"))));
            } else if (isArchive && "PUT".equals(ex.getRequestMethod())) {
                if (!user.isAdmin()) { send(ex, 403, JsonUtil.error(403, "Admin only.")); return; }
                complaintDAO.archive(id, user.getId());
                send(ex, 200, JsonUtil.ok("Archived."));
            } else if ("DELETE".equals(ex.getRequestMethod())) {
                if (!user.isAdmin()) { send(ex, 403, JsonUtil.error(403, "Admin only.")); return; }
                complaintDAO.delete(id);
                send(ex, 200, JsonUtil.ok("Deleted."));
            } else if ("GET".equals(ex.getRequestMethod())) {
                send(ex, 200, JsonUtil.ok(c));
            } else {
                send(ex, 405, JsonUtil.error(405, "Method not allowed"));
            }
        } catch (NumberFormatException e) {
            send(ex, 400, JsonUtil.error(400, "Invalid ID."));
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, JsonUtil.error(500, "Server error: " + e.getMessage()));
        }
    }

    private void handleAdminComplaints(HttpExchange ex) throws IOException {
        cors(ex);
        if (handleOptions(ex)) return;
        try {
            Optional<User> auth = authenticate(ex);
            if (auth.isEmpty() || !auth.get().isAdmin()) { send(ex, 403, JsonUtil.error(403, "Admin only.")); return; }
            Map<String, String> params = parseQuery(ex.getRequestURI().getQuery());
            send(ex, 200, JsonUtil.ok(complaintDAO.findAll(params.get("status"), params.get("priority"))));
        } catch (Exception e) {
            send(ex, 500, JsonUtil.error(500, "Server error."));
        }
    }

    private void handleAdminUsers(HttpExchange ex) throws IOException {
        cors(ex);
        if (handleOptions(ex)) return;
        try {
            Optional<User> auth = authenticate(ex);
            if (auth.isEmpty() || !auth.get().isAdmin()) { send(ex, 403, JsonUtil.error(403, "Admin only.")); return; }
            send(ex, 200, JsonUtil.ok(userDAO.findAll()));
        } catch (Exception e) {
            send(ex, 500, JsonUtil.error(500, "Server error."));
        }
    }

    private void handleStats(HttpExchange ex) throws IOException {
        cors(ex);
        if (handleOptions(ex)) return;
        try {
            Optional<User> auth = authenticate(ex);
            if (auth.isEmpty() || !auth.get().isAdmin()) { send(ex, 403, JsonUtil.error(403, "Admin only.")); return; }
            send(ex, 200, JsonUtil.ok(complaintDAO.getStats()));
        } catch (Exception e) {
            send(ex, 500, JsonUtil.error(500, "Server error."));
        }
    }

    private void handleDepartments(HttpExchange ex) throws IOException {
        cors(ex);
        if (handleOptions(ex)) return;
        try {
            Optional<User> auth = authenticate(ex);
            if (auth.isEmpty()) { send(ex, 401, JsonUtil.error(401, "Unauthorized.")); return; }
            List<Map<String, Object>> depts = new ArrayList<>();
            try (var conn = DatabaseUtil.getConnection();
                 var ps   = conn.prepareStatement("SELECT * FROM departments ORDER BY name");
                 var rs   = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> d = new LinkedHashMap<>();
                    d.put("id",   rs.getInt("id"));
                    d.put("name", rs.getString("name"));
                    depts.add(d);
                }
            }
            send(ex, 200, JsonUtil.ok(depts));
        } catch (Exception e) {
            send(ex, 500, JsonUtil.error(500, "Server error."));
        }
    }

    private Optional<User> authenticate(HttpExchange ex) throws SQLException {
        return sessions.validate(bearerToken(ex));
    }

    private String bearerToken(HttpExchange ex) {
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) return auth.substring(7).trim();
        return null;
    }

    private JsonObject parseBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            if (json.isBlank()) return new JsonObject();
            return JsonParser.parseString(json).getAsJsonObject();
        }
    }

    private String req(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return "";
        return obj.get(key).getAsString().trim();
    }

    private void send(HttpExchange ex, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        Headers h = ex.getResponseHeaders();
        h.set("Content-Type", "application/json; charset=utf-8");
        h.set("Access-Control-Allow-Origin",  "*");
        h.set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        h.set("Access-Control-Allow-Headers", "Authorization,Content-Type,Accept");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null) return map;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) map.put(kv[0], kv[1]);
        }
        return map;
    }
}
