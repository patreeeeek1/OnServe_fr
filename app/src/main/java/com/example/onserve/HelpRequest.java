package com.example.onserve;

import com.google.firebase.firestore.PropertyName;

public class HelpRequest {
    private String docId;
    private String userEmail;
    private String userName;
    private String type;
    private String status;
    private String description;
    private String location;
    private String date;
    private String phoneNumber;
    private boolean asap;
    private long timestamp;
    private boolean emergency;
    private String scheduledDate;
    private String scheduledTime;
    private String volunteerName;

    // No-argument constructor required for Firestore
    public HelpRequest() {}

    public String getVolunteerName() { return volunteerName; }
    public void setVolunteerName(String volunteerName) { this.volunteerName = volunteerName; }

    public String getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(String scheduledDate) { this.scheduledDate = scheduledDate; }

    public String getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(String scheduledTime) { this.scheduledTime = scheduledTime; }

    public HelpRequest(String userEmail, String type, String status, String description, String location, String date, String phoneNumber, boolean asap, long timestamp, boolean emergency) {
        this.userEmail = userEmail;
        this.type = type;
        this.status = status;
        this.description = description;
        this.location = location;
        this.date = date;
        this.phoneNumber = phoneNumber;
        this.asap = asap;
        this.timestamp = timestamp;
        this.emergency = emergency;
    }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public boolean isAsap() { return asap; }
    public void setAsap(boolean asap) { this.asap = asap; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isEmergency() { return emergency; }
    public void setEmergency(boolean emergency) { this.emergency = emergency; }

    public String getDocId() { return docId; }
    public void setDocId(String docId) { this.docId = docId; }
}
