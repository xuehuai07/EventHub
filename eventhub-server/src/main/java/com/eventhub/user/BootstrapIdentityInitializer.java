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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
