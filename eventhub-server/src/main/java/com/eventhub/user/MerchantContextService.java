package com.eventhub.user;

import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import com.eventhub.security.AuthenticatedUser;
import org.springframework.stereotype.Service;

@Service
public class MerchantContextService {

    private final UserIdentityMapper mapper;

    public MerchantContextService(UserIdentityMapper mapper) {
        this.mapper = mapper;
    }

    public MerchantBinding requireActiveMerchant(AuthenticatedUser user) {
        MerchantBinding binding = mapper.findMerchantBinding(user.id());
        if (binding == null) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_BOUND);
        }
        if (!binding.active()) {
            throw new BusinessException(ErrorCode.MERCHANT_INACTIVE);
        }
        return binding;
    }
}
