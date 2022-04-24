package com.example.newgroceriio.Models;

// User class to keep track of registered user and their profile
public class User {
    private String email;
    private String name;
    private boolean verified; // verification status

    public User(String name, String email, boolean verified) {
        this.email = email;
        this.name = name;
        this.verified = verified;
    }


    // Getters and Setters below:
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

}