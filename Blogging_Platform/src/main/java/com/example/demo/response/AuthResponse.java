package com.example.demo.response;

public class AuthResponse {
   private Long id;
   private String token;
   private String role;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public AuthResponse(Long id, String token) {
        this.id = id;
        this.token = token;
    }
}
