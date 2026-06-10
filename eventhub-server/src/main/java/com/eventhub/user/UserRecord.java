package com.eventhub.user;

public class UserRecord {

    private Long id;
    private String username;
    private String phone;
    private String passwordHash;
    private String displayName;
    private String status;

    public UserRecord() {}

    public UserRecord(String username, String phone, String passwordHash, String displayName) {
        this.username = username;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public String getPhone() {
        return phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getStatus() {
        return status;
    }
}
