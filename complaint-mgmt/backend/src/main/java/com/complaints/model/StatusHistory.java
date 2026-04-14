package com.complaints.model;

import java.util.Date;

public class StatusHistory {
    private int    id;
    private int    complaintId;
    private int    changedBy;
    private String changedByName;
    private String oldStatus;
    private String newStatus;
    private String note;
    private Date   changedAt;

    public int    getId()            { return id; }
    public int    getComplaintId()   { return complaintId; }
    public int    getChangedBy()     { return changedBy; }
    public String getChangedByName() { return changedByName; }
    public String getOldStatus()     { return oldStatus; }
    public String getNewStatus()     { return newStatus; }
    public String getNote()          { return note; }
    public Date   getChangedAt()     { return changedAt; }

    public void setId(int v)               { this.id = v; }
    public void setComplaintId(int v)      { this.complaintId = v; }
    public void setChangedBy(int v)        { this.changedBy = v; }
    public void setChangedByName(String v) { this.changedByName = v; }
    public void setOldStatus(String v)     { this.oldStatus = v; }
    public void setNewStatus(String v)     { this.newStatus = v; }
    public void setNote(String v)          { this.note = v; }
    public void setChangedAt(Date v)       { this.changedAt = v; }
}
