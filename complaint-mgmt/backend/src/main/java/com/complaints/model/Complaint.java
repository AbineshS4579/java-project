package com.complaints.model;

import java.util.Date;
import java.util.List;

public class Complaint {
    private int id;
    private String title;
    private String description;
    private String category;
    private String priority;
    private String status;
    private int userId;
    private String userName;        // joined
    private Integer assignedTo;
    private String assignedToName;  // joined
    private Integer departmentId;
    private String departmentName;  // joined
    private String resolution;
    private Date createdAt;
    private Date updatedAt;

    private List<StatusHistory> history;
    private List<Comment> comments;

    public Complaint() {}

    // ── Getters ──────────────────────────────
    public int     getId()              { return id; }
    public String  getTitle()           { return title; }
    public String  getDescription()     { return description; }
    public String  getCategory()        { return category; }
    public String  getPriority()        { return priority; }
    public String  getStatus()          { return status; }
    public int     getUserId()          { return userId; }
    public String  getUserName()        { return userName; }
    public Integer getAssignedTo()      { return assignedTo; }
    public String  getAssignedToName()  { return assignedToName; }
    public Integer getDepartmentId()    { return departmentId; }
    public String  getDepartmentName()  { return departmentName; }
    public String  getResolution()      { return resolution; }
    public Date    getCreatedAt()       { return createdAt; }
    public Date    getUpdatedAt()       { return updatedAt; }
    public List<StatusHistory> getHistory()  { return history; }
    public List<Comment>       getComments() { return comments; }

    // ── Setters ──────────────────────────────
    public void setId(int id)                           { this.id = id; }
    public void setTitle(String v)                      { this.title = v; }
    public void setDescription(String v)                { this.description = v; }
    public void setCategory(String v)                   { this.category = v; }
    public void setPriority(String v)                   { this.priority = v; }
    public void setStatus(String v)                     { this.status = v; }
    public void setUserId(int v)                        { this.userId = v; }
    public void setUserName(String v)                   { this.userName = v; }
    public void setAssignedTo(Integer v)                { this.assignedTo = v; }
    public void setAssignedToName(String v)             { this.assignedToName = v; }
    public void setDepartmentId(Integer v)              { this.departmentId = v; }
    public void setDepartmentName(String v)             { this.departmentName = v; }
    public void setResolution(String v)                 { this.resolution = v; }
    public void setCreatedAt(Date v)                    { this.createdAt = v; }
    public void setUpdatedAt(Date v)                    { this.updatedAt = v; }
    public void setHistory(List<StatusHistory> h)       { this.history = h; }
    public void setComments(List<Comment> c)            { this.comments = c; }
}
