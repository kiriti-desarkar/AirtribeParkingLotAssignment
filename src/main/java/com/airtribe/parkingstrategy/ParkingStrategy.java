package com.airtribe.parkingstrategy;

import com.airtribe.ParkingLot;
import com.airtribe.ParkingSpot;
import com.airtribe.Vehicle;

public interface ParkingStrategy {
    ParkingSpot findSpot(Vehicle vehicle, ParkingLot parkingLot);
}
