package com.eventhub.activity.infrastructure.persistence;

import com.eventhub.activity.domain.SeatMode;
import java.time.LocalDateTime;

public class SessionRecord {

    private Long id;
    private long activityId;
    private long venueId;
    private String venueName;
    private String venueAddress;
    private SeatMode seatMode;
    private String name;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private LocalDateTime saleStartAt;
    private LocalDateTime saleEndAt;
    private String status;
    private int version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getActivityId() {
        return activityId;
    }

    public void setActivityId(long activityId) {
        this.activityId = activityId;
    }

    public long getVenueId() {
        return venueId;
    }

    public void setVenueId(long venueId) {
        this.venueId = venueId;
    }

    public String getVenueName() {
        return venueName;
    }

    public void setVenueName(String venueName) {
        this.venueName = venueName;
    }

    public String getVenueAddress() {
        return venueAddress;
    }

    public void setVenueAddress(String venueAddress) {
        this.venueAddress = venueAddress;
    }

    public SeatMode getSeatMode() {
        return seatMode;
    }

    public void setSeatMode(SeatMode seatMode) {
        this.seatMode = seatMode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public LocalDateTime getSaleStartAt() {
        return saleStartAt;
    }

    public void setSaleStartAt(LocalDateTime saleStartAt) {
        this.saleStartAt = saleStartAt;
    }

    public LocalDateTime getSaleEndAt() {
        return saleEndAt;
    }

    public void setSaleEndAt(LocalDateTime saleEndAt) {
        this.saleEndAt = saleEndAt;
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
