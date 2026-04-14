package com.complaints.model;

import java.util.Date;

public class User {
    private int id;
    private String name;
    private String email;
    private transient String password;   // never serialised to JSON
    private String role;
    private String department;
    private boolean isActive;
    private Date createdAt;
    private Date updatedAt;

    public User() {}

    // ── Getters ──────────────────────────────
    public int     getId()         { return id; }
    public String  getName()       { return name; }
    public String  getEmail()      { return email; }
    public String  getPassword()   { return password; }
    public String  getRole()       { return role; }
    public String  getDepartment() { return department; }
    public boolean isActive()      { return isActive; }
    public Date    getCreatedAt()  { return createdAt; }
    public Date    getUpdatedAt()  { return updatedAt; }

    // ── Setters ──────────────────────────────
    public void setId(int id)                   { this.id = id; }
    public void setName(String name)            { this.name = name; }
    public void setEmail(String email)          { this.email = email; }
    public void setPassword(String password)    { this.password = password; }
    public void setRole(String role)            { this.role = role; }
    public void setDepartment(String dept)      { this.department = dept; }
    public void setActive(boolean active)       { this.isActive = active; }
    public void setCreatedAt(Date d)            { this.createdAt = d; }
    public void setUpdatedAt(Date d)            { this.updatedAt = d; }

    public boolean isAdmin() { return "admin".equalsIgnoreCase(role); }
}
