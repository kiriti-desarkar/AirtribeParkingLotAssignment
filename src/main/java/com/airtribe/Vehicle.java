package com.airtribe;

public class Vehicle {
    private final String licenseNumber; // Also referred to as registration number
    private final VehicleType type;// Essential for spot matching and fee calculation

    //No change once injected using constructor
    public Vehicle(String licenseNumber, VehicleType type) {
        this.licenseNumber = licenseNumber;
        this.type = type;
    }

    //only getters
    public String getLicenseNumber() {
        return licenseNumber;
    }

    public VehicleType getType() {
        return type;
    }

}
