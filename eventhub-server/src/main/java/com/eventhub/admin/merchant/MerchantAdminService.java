package com.eventhub.admin.merchant;

import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import com.eventhub.user.MerchantRecord;
import com.eventhub.user.UserIdentityMapper;
import com.eventhub.user.UserRecord;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantAdminService {

    private final MerchantAdminMapper merchantMapper;
    private final UserIdentityMapper userMapper;

    public MerchantAdminService(MerchantAdminMapper merchantMapper, UserIdentityMapper userMapper) {
        this.merchantMapper = merchantMapper;
        this.userMapper = userMapper;
    }

    public List<MerchantView> list() {
        return merchantMapper.findAll();
    }

    @Transactional
    public MerchantView create(MerchantCreateRequest request) {
        MerchantRecord merchant = new MerchantRecord(request.name().trim(), normalize(request.description()));
        merchantMapper.insert(merchant);
        return merchantMapper.findAll().stream()
                .filter(item -> item.id() == merchant.getId())
                .findFirst()
                .orElseThrow();
    }

    @Transactional
    public void updateStatus(long merchantId, String status) {
        requireMerchant(merchantId);
        merchantMapper.updateStatus(merchantId, status);
    }

    @Transactional
    public void bindStaff(long merchantId, String identifier) {
        requireMerchant(merchantId);
        UserRecord user = userMapper.findByIdentifier(identifier.trim());
        if (user == null) {
            throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND, "待绑定用户不存在");
        }
        if (merchantMapper.countUserBinding(user.getId()) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "该用户已绑定其他商家");
        }
        merchantMapper.bindStaff(merchantId, user.getId());
        userMapper.assignRoleIfMissing(user.getId(), "MERCHANT");
    }

    private void requireMerchant(long merchantId) {
        if (merchantMapper.countById(merchantId) == 0) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_BOUND, "商家不存在");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
