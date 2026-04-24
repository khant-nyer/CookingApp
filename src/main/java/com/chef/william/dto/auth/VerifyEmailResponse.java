package com.chef.william.dto.auth;

import com.chef.william.model.AccountStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VerifyEmailResponse {
    String email;
    boolean emailVerified;
    AccountStatus accountStatus;
    String status;
}
