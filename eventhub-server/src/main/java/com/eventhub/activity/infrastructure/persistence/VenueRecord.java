package com.eventhub.activity.infrastructure.persistence;

import com.eventhub.activity.domain.SeatMode;

public class VenueRecord {

    private Long id;
    private long merchantId;
    private String name;
    private String city;
    private String address;
    private SeatMode seatMode;
    private int capacity;
    private String status;
    private int version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(long merchantId) {
        this.merchantId = merchantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public SeatMode getSeatMode() {
        return seatMode;
    }

    public void setSeatMode(SeatMode seatMode) {
        this.seatMode = seatMode;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
