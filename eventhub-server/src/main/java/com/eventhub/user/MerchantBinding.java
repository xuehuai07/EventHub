package com.eventhub.user;

public record MerchantBinding(long merchantId, String merchantName, String merchantStatus, String staffStatus) {

    public boolean active() {
        return "ACTIVE".equals(merchantStatus) && "ACTIVE".equals(staffStatus);
    }
}
