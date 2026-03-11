package com.chef.william.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_cognito_sub", columnNames = "cognito_sub"),
                @UniqueConstraint(name = "uk_user_email", columnNames = "email")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "cognito_sub", nullable = false, unique = true, updatable = false)
    private String cognitoSub;

    private String profileImageUrl;

}
