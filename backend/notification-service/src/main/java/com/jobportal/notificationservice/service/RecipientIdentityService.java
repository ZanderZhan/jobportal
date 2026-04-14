package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.dto.ResolvedRecipient;
import com.jobportal.notificationservice.entity.RecipientIdentity;
import com.jobportal.notificationservice.repository.RecipientIdentityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
public class RecipientIdentityService {

    private final RecipientIdentityRepository recipientIdentityRepository;

    public RecipientIdentityService(RecipientIdentityRepository recipientIdentityRepository) {
        this.recipientIdentityRepository = recipientIdentityRepository;
    }

    public void remember(String userId, String email, String name, String role, String source) {
        if (!StringUtils.hasText(userId)) {
            return;
        }

        Instant now = Instant.now();
        RecipientIdentity identity = recipientIdentityRepository.findById(userId)
                .orElseGet(() -> {
                    RecipientIdentity created = new RecipientIdentity();
                    created.setUserId(userId);
                    created.setCreatedAt(now);
                    return created;
                });

        if (StringUtils.hasText(email)) {
            identity.setEmail(email);
        }
        if (StringUtils.hasText(name)) {
            identity.setDisplayName(name);
        }
        if (StringUtils.hasText(role)) {
            identity.setRole(role);
        }
        if (StringUtils.hasText(source)) {
            identity.setSource(source);
        }

        identity.setLastSeenAt(now);
        identity.setUpdatedAt(now);
        recipientIdentityRepository.save(identity);
    }

    @Transactional(readOnly = true)
    public ResolvedRecipient resolve(String userId, String fallbackEmail, String fallbackName, String fallbackRole) {
        if (!StringUtils.hasText(userId)) {
            return new ResolvedRecipient(null, fallbackEmail, fallbackName, fallbackRole);
        }

        Optional<RecipientIdentity> identity = recipientIdentityRepository.findById(userId);
        String email = identity.map(RecipientIdentity::getEmail).filter(StringUtils::hasText).orElse(fallbackEmail);
        String name = identity.map(RecipientIdentity::getDisplayName).filter(StringUtils::hasText).orElse(fallbackName);
        String role = identity.map(RecipientIdentity::getRole).filter(StringUtils::hasText).orElse(fallbackRole);

        return new ResolvedRecipient(userId, email, name, role);
    }

    @Transactional(readOnly = true)
    public Optional<RecipientIdentity> findByUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            return Optional.empty();
        }
        return recipientIdentityRepository.findById(userId);
    }
}
