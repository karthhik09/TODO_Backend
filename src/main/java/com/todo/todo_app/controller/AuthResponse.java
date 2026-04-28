// Auth Response DTO

package com.todo.todo_app.controller;

public class AuthResponse {
    private Long userId;
    private String name;
    private String email;
    private String token;

    // MFA fields
    private Boolean mfaRequired;
    private String tempToken;

    // Constructor for normal user login
    public AuthResponse(Long userId, String name, String email, String token) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.token = token;
    }

    // Constructor for the MFA
    public AuthResponse(Boolean mfaRequired, String tempToken) {
        this.mfaRequired = mfaRequired;
        this.tempToken = tempToken;
    }

    public Long getUserId()       { return userId; }
    public String getName()       { return name; }
    public String getEmail()      { return email; }
    public String getToken()      { return token; }
    public Boolean getMfaRequired() { return mfaRequired; }
    public String getTempToken()  { return tempToken; }
}
