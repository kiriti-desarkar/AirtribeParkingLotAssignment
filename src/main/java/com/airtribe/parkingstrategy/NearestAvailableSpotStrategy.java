package com.airtribe.parkingstrategy;

import com.airtribe.ParkingLot;
import com.airtribe.ParkingSpot;
import com.airtribe.Vehicle;
import com.airtribe.parkingfloor.ParkingFloor;

public class NearestAvailableSpotStrategy implements ParkingStrategy {
    @Override
    public ParkingSpot findSpot(Vehicle vehicle, ParkingLot parkingLot) {
        for (ParkingFloor floor : parkingLot.getFloors()) {
            if (floor.isUnderMaintenance()) continue;
            ParkingSpot spot = floor.getAvailableSpot(vehicle);
            if (spot != null) return spot;
        }
        return null;
    }
}
