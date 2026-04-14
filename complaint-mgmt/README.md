# 🗂️ Complaint Management System

A full-stack Complaint Management System with a **Java Swing desktop backend** (REST API over JDBC + MySQL) and a **React frontend** communicating with the backend.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│  Java Swing Desktop App (MainApp.java)                  │
│  ┌──────────────────────────────────────────────────┐   │
│  │  ApiServer (com.sun.net.httpserver — port 8080)  │   │
│  │  ├── /api/auth/*         AuthController          │   │
│  │  ├── /api/complaints/*   ComplaintController     │   │
│  │  └── /api/admin/*        AdminController         │   │
│  └──────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────┐   │
│  │  DAO Layer (JDBC + HikariCP)                     │   │
│  │  ├── UserDAO         ├── ComplaintDAO            │   │
│  │  └── SessionService                              │   │
│  └──────────────────────────────────────────────────┘   │
└───────────────────────┬─────────────────────────────────┘
                        │ REST / JSON (HTTP)
┌───────────────────────▼─────────────────────────────────┐
│  React Frontend (Vite — port 5173)                       │
│  ├── LoginPage / RegisterPage                            │
│  ├── UserDashboard   (submit, track, comment)            │
│  └── AdminDashboard  (manage all complaints, stats)      │
└─────────────────────────────────────────────────────────┘
                        │ JDBC / HikariCP
┌───────────────────────▼─────────────────────────────────┐
│  MySQL  (complaint_db)                                   │
│  ├── users          ├── complaints   ├── departments     │
│  ├── status_history ├── comments     └── sessions        │
└─────────────────────────────────────────────────────────┘
```

---

## Prerequisites

| Tool        | Version  |
|-------------|----------|
| Java        | 17+      |
| Maven       | 3.8+     |
| MySQL       | 8.0+     |
| Node.js     | 18+      |
| npm         | 9+       |

---

## Quick Start

### 1 — Database

```bash
# Log in to MySQL
mysql -u root -p

# Run the schema (creates DB, tables, and sample data)
source /path/to/complaint-mgmt/sql/schema.sql
```

**Sample accounts (password: `Password1!`):**

| Email            | Role  |
|------------------|-------|
| admin@demo.com   | admin |
| bob@demo.com     | user  |
| carol@demo.com   | user  |
| dave@demo.com    | admin |

---

### 2 — Backend (Java Swing + REST API)

**Configure the database connection:**

```bash
# Edit the properties file
nano backend/src/main/resources/db.properties
```

```properties
db.url=jdbc:mysql://localhost:3306/complaint_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
db.username=root
db.password=YOUR_PASSWORD_HERE
```

**Build and run:**

```bash
cd backend
mvn clean package -q
java -jar target/complaint-management-1.0.0.jar
```

The Swing launcher window will appear and the REST API will be listening at `http://localhost:8080`.

---

### 3 — Frontend (React + Vite)

```bash
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173** in your browser.

---

## REST API Reference

All endpoints require `Authorization: Bearer <token>` except auth routes.

### Authentication

| Method | Endpoint              | Body                                      | Description        |
|--------|-----------------------|-------------------------------------------|--------------------|
| POST   | /api/auth/register    | `{name, email, password}`                 | Register user      |
| POST   | /api/auth/login       | `{email, password}`                       | Login, returns token |
| POST   | /api/auth/logout      | —                                         | Invalidate session |
| GET    | /api/auth/me          | —                                         | Current user info  |

### Complaints (User)

| Method | Endpoint                          | Body / Query                       | Description              |
|--------|-----------------------------------|------------------------------------|--------------------------|
| GET    | /api/complaints                   | —                                  | My complaints            |
| POST   | /api/complaints                   | `{title, description, category, priority}` | Submit complaint |
| GET    | /api/complaints/{id}              | —                                  | Get with history+comments|
| POST   | /api/complaints/{id}/comments     | `{body, internal?}`                | Add comment              |

### Admin Endpoints

| Method | Endpoint                          | Body / Query                       | Description              |
|--------|-----------------------------------|------------------------------------|--------------------------|
| GET    | /api/admin/complaints             | `?status=&priority=`               | All complaints (filtered)|
| PUT    | /api/complaints/{id}/status       | `{status, note?}`                  | Change status            |
| PUT    | /api/complaints/{id}/assign       | `{agentId?, deptId?}`              | Assign to agent/dept     |
| PUT    | /api/complaints/{id}/resolve      | `{resolution}`                     | Resolve with notes       |
| PUT    | /api/complaints/{id}/archive      | —                                  | Archive complaint        |
| DELETE | /api/complaints/{id}              | —                                  | Delete complaint         |
| GET    | /api/admin/users                  | —                                  | List all users           |
| GET    | /api/admin/stats                  | —                                  | Status counts            |
| GET    | /api/departments                  | —                                  | List departments         |

### Response Format

```json
{
  "success": true,
  "data": { ... }
}
```

Error responses:

```json
{
  "success": false,
  "code": 401,
  "message": "Unauthorized."
}
```

---

## Database Schema

```
users            complaints         status_history
─────────        ──────────         ──────────────
id               id                 id
name             title              complaint_id  → complaints.id
email            description        changed_by    → users.id
password (bcrypt)category           old_status
role             priority           new_status
department       status             note
is_active        user_id → users    changed_at
created_at       assigned_to → users
updated_at       department_id
                 resolution
                 created_at
                 updated_at

comments         departments        sessions
────────         ───────────        ────────
id               id                 token (PK)
complaint_id     name               user_id → users
user_id          created_at         expires_at
body                                created_at
is_internal
created_at
```

---

## Project Structure

```
complaint-mgmt/
├── sql/
│   └── schema.sql                      # Full DB schema + sample data
│
├── backend/
│   ├── pom.xml                         # Maven build (Java 17, HikariCP, BCrypt, Gson)
│   └── src/main/
│       ├── java/com/complaints/
│       │   ├── model/                  # POJOs: User, Complaint, Comment, StatusHistory
│       │   ├── dao/                    # JDBC DAOs: UserDAO, ComplaintDAO
│       │   ├── service/                # SessionService (token store)
│       │   ├── controller/             # ApiServer (HTTP routing)
│       │   ├── util/                   # DatabaseUtil (HikariCP), JsonUtil
│       │   └── swing/                  # MainApp (Swing launcher + app entry point)
│       └── resources/
│           └── db.properties           # DB connection config (excluded from VCS)
│
└── frontend/
    ├── package.json
    ├── vite.config.js                  # Dev proxy → localhost:8080
    ├── index.html
    └── src/
        ├── main.jsx
        ├── App.jsx                     # Router (login ↔ register ↔ dashboard)
        ├── context/
        │   └── AuthContext.jsx         # Auth state + token management
        ├── pages/
        │   ├── LoginPage.jsx
        │   ├── RegisterPage.jsx
        │   ├── UserDashboard.jsx       # Submit, track, filter, view complaints
        │   └── AdminDashboard.jsx      # Manage all complaints, users, stats
        ├── components/
        │   ├── ui.jsx                  # Badge, Modal, Timeline, CommentList, …
        │   └── ComplaintDetail.jsx     # Shared detail modal (tabs: details/history/comments)
        └── styles/
            └── global.css             # Full design system (dark theme, CSS vars)
```

---

## Security Features

- **Passwords** hashed with BCrypt (cost factor 12)
- **SQL injection** prevented via PreparedStatements throughout
- **Sessions** are server-side tokens (256-bit random, 8h TTL) — no JWT secrets in client
- **Role-based access** enforced server-side on every endpoint
- **CORS** headers set; restrict `Allow-Origin` in production
- **Input validation** on all endpoints (blank checks, length limits)
- **No sensitive data in logs** — passwords never logged, tokens not logged

---

## Production Notes

1. Replace `Access-Control-Allow-Origin: *` with your actual frontend domain
2. Add HTTPS (use a reverse proxy like Nginx in front of the Java HTTP server)
3. Move `db.properties` outside the classpath and load via environment variable
4. Set a session cleanup cron: `SessionService.purgeExpired()` can be called on a timer
5. Consider rate-limiting the `/api/auth/login` endpoint

---

## Troubleshooting

| Problem                             | Fix                                                         |
|-------------------------------------|-------------------------------------------------------------|
| `db.properties not found`           | Ensure it's in `src/main/resources/` before building        |
| `Communications link failure`        | Check MySQL is running and credentials in `db.properties`   |
| Frontend shows CORS errors          | Confirm the Java server is running on port 8080             |
| BCrypt class not found              | Run `mvn clean package` to rebuild the fat JAR              |
| Port 8080 already in use            | Change `PORT` constant in `ApiServer.java` and `vite.config.js` |
