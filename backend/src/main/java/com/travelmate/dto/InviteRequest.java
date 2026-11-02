package com.travelmate.dto;

import com.travelmate.entity.TripRole;

public class InviteRequest {
    private String email;
    private TripRole role = TripRole.EDITOR;

    public InviteRequest() {}

    public InviteRequest(String email, TripRole role) {
        this.email = email;
        this.role = role;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public TripRole getRole() { return role; }
    public void setRole(TripRole role) { this.role = role; }
}
