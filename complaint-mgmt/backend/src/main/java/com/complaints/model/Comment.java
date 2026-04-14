package com.complaints.model;

import java.util.Date;

public class Comment {
    private int     id;
    private int     complaintId;
    private int     userId;
    private String  userName;
    private String  userRole;
    private String  body;
    private boolean isInternal;
    private Date    createdAt;

    public int     getId()          { return id; }
    public int     getComplaintId() { return complaintId; }
    public int     getUserId()      { return userId; }
    public String  getUserName()    { return userName; }
    public String  getUserRole()    { return userRole; }
    public String  getBody()        { return body; }
    public boolean isInternal()     { return isInternal; }
    public Date    getCreatedAt()   { return createdAt; }

    public void setId(int v)             { this.id = v; }
    public void setComplaintId(int v)    { this.complaintId = v; }
    public void setUserId(int v)         { this.userId = v; }
    public void setUserName(String v)    { this.userName = v; }
    public void setUserRole(String v)    { this.userRole = v; }
    public void setBody(String v)        { this.body = v; }
    public void setInternal(boolean v)   { this.isInternal = v; }
    public void setCreatedAt(Date v)     { this.createdAt = v; }
}
