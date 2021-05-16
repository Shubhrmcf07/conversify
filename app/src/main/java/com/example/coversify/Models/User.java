package com.example.coversify.Models;

public class User {
    private String uid, name, phone, profileImage;

    public User(){}

    public User(String uid, String name, String phone, String profileImage) {
        this.uid = uid;
        this.name = name;
        this.phone = phone;
        this.profileImage = profileImage;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
}
