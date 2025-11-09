package com.example.lab6_20212529.model;

public class Vehicle {
    private String id;
    private String nickname;
    private String plate;
    private String brandModel;
    private int year;
    private long lastInspectionDate;

    public Vehicle() {
        // Empty constructor required for Firestore
    }

    public Vehicle(String id, String nickname, String plate, String brandModel, int year, long lastInspectionDate) {
        this.id = id;
        this.nickname = nickname;
        this.plate = plate;
        this.brandModel = brandModel;
        this.year = year;
        this.lastInspectionDate = lastInspectionDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public String getBrandModel() {
        return brandModel;
    }

    public void setBrandModel(String brandModel) {
        this.brandModel = brandModel;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public long getLastInspectionDate() {
        return lastInspectionDate;
    }

    public void setLastInspectionDate(long lastInspectionDate) {
        this.lastInspectionDate = lastInspectionDate;
    }
}

