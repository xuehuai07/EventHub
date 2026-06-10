package com.eventhub.user;

public class MerchantRecord {

    private Long id;
    private final String name;
    private final String description;

    public MerchantRecord(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
