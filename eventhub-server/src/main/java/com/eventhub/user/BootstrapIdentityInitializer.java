package com.eventhub.user;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@EnableConfigurationProperties(BootstrapIdentityProperties.class)
public class BootstrapIdentityInitializer implements ApplicationRunner {

    private final BootstrapIdentityProperties properties;
    private final UserIdentityMapper mapper;
    private final PasswordEncoder passwordEncoder;

    public BootstrapIdentityInitializer(
            BootstrapIdentityProperties properties, UserIdentityMapper mapper, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        createAdmin();
        createMerchant();
    }

    private void createAdmin() {
        if (!hasText(properties.adminUsername()) || !hasText(properties.adminPassword())) {
            return;
        }
        if (mapper.findByIdentifier(properties.adminUsername()) != null) {
            return;
        }
        UserRecord admin = new UserRecord(
                properties.adminUsername().trim(),
                null,
                passwordEncoder.encode(properties.adminPassword()),
                properties.adminDisplayName());
        mapper.insert(admin);
        mapper.assignRole(admin.getId(), "ADMIN");
    }

    private void createMerchant() {
        if (!hasText(properties.merchantUsername()) || !hasText(properties.merchantPassword())) {
            return;
        }
        if (mapper.findByIdentifier(properties.merchantUsername()) != null) {
            return;
        }
        UserRecord user = new UserRecord(
                properties.merchantUsername().trim(),
                null,
                passwordEncoder.encode(properties.merchantPassword()),
                properties.merchantDisplayName());
        mapper.insert(user);
        mapper.assignRole(user.getId(), "MERCHANT");
        MerchantRecord merchant = new MerchantRecord(properties.merchantName(), "本地开发演示商家");
        mapper.insertMerchant(merchant);
        mapper.bindMerchantStaff(merchant.getId(), user.getId());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
