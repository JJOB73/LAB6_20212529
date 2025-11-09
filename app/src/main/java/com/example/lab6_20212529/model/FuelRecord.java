package com.example.lab6_20212529.model;

public class FuelRecord {
    private String id;
    private String recordCode;
    private String vehicleId;
    private String vehicleNickname;
    private long date;
    private double liters;
    private double mileage;
    private double totalPrice;
    private String fuelType;

    public FuelRecord() {
        // Firestore requires empty constructor
    }

    public FuelRecord(String id,
                      String recordCode,
                      String vehicleId,
                      String vehicleNickname,
                      long date,
                      double liters,
                      double mileage,
                      double totalPrice,
                      String fuelType) {
        this.id = id;
        this.recordCode = recordCode;
        this.vehicleId = vehicleId;
        this.vehicleNickname = vehicleNickname;
        this.date = date;
        this.liters = liters;
        this.mileage = mileage;
        this.totalPrice = totalPrice;
        this.fuelType = fuelType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRecordCode() {
        return recordCode;
    }

    public void setRecordCode(String recordCode) {
        this.recordCode = recordCode;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehicleNickname() {
        return vehicleNickname;
    }

    public void setVehicleNickname(String vehicleNickname) {
        this.vehicleNickname = vehicleNickname;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public double getLiters() {
        return liters;
    }

    public void setLiters(double liters) {
        this.liters = liters;
    }

    public double getMileage() {
        return mileage;
    }

    public void setMileage(double mileage) {
        this.mileage = mileage;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }
}

