package com.chef.william.model.auth;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "registration_idempotency",
        uniqueConstraints = @UniqueConstraint(name = "uk_registration_idempotency_key", columnNames = "idempotency_key"))
@Getter
@Setter
public class RegistrationIdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegistrationIdempotencyStatus status;

    @Column(name = "response_user_id")
    private Long responseUserId;

    @Column(name = "response_email", length = 255)
    private String responseEmail;

    @Column(name = "response_user_name", length = 255)
    private String responseUserName;

    @Column(name = "response_profile_image_url", length = 1000)
    private String responseProfileImageUrl;

    @Column(name = "response_cognito_sub", length = 255)
    private String responseCognitoSub;

    @Column(name = "response_status", length = 80)
    private String responseStatus;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
