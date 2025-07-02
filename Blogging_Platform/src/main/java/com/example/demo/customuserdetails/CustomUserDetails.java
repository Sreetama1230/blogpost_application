package com.example.demo.customuserdetails;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import org.springframework.security.core.userdetails.User;

public class CustomUserDetails extends User {


    private final Long id; // Add custom fields here


    public CustomUserDetails(
            Long id,
            String username,
            String password,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(username, password, authorities);
        this.id = id;
    }




    public Long getId() {
        return this.id; // Directly return the ID field
    }
}