package com.airtribe.parkingstrategy;

import com.airtribe.ParkingLot;
import com.airtribe.ParkingSpot;
import com.airtribe.Vehicle;
import com.airtribe.parkingfloor.ParkingFloor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RandomSpotStrategy implements ParkingStrategy {
    @Override
    public ParkingSpot findSpot(Vehicle vehicle, ParkingLot parkingLot) {
        List<ParkingFloor> floors = new ArrayList<>(parkingLot.getFloors());
        Collections.shuffle(floors);
        for (ParkingFloor floor : floors) {
            if (floor.isUnderMaintenance()) continue;

            ParkingSpot spot = floor.getAvailableSpot(vehicle);
            if (spot != null) return spot;
        }
        return null;
    }
}
