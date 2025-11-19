package com.airtribe.parkingfloor;

import com.airtribe.ParkingSpot;
import com.airtribe.SpotType;
import com.airtribe.Vehicle;
import com.airtribe.displaypanel.FloorDisplayPanel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ParkingFloor {
    private final String floorId;
    private final Map<SpotType, Set<ParkingSpot>> spotMap;
    private final FloorDisplayPanel displayPanel;
    private volatile boolean underMaintenance;
    private final ReadWriteLock maintenanceLock = new ReentrantReadWriteLock();

    public ParkingFloor(String floorId) {
        this.floorId = floorId;
        this.spotMap = new HashMap<>();
        this.displayPanel = new FloorDisplayPanel(floorId);
        this.underMaintenance = false;
        for (SpotType type : SpotType.values()) {
            spotMap.put(type, new HashSet<>());
        }
    }

    public void addSpot(ParkingSpot spot) {
        spotMap.get(spot.getSpotType()).add(spot);
    }

    /**
     * Finds an available spot for the vehicle.
     * Thread-safe read operation.
     * 
     * @param vehicle The vehicle to find a spot for
     * @return Available ParkingSpot or null if none available
     */
    public ParkingSpot getAvailableSpot(Vehicle vehicle) {
        maintenanceLock.readLock().lock();
        try {
            if (underMaintenance) {
                return null;
            }

            for (Map.Entry<SpotType, Set<ParkingSpot>> entry : spotMap.entrySet()) {
                for (ParkingSpot spot : entry.getValue()) {
                    if (spot.canFitVehicle(vehicle)) {
                        return spot;
                    }
                }
            }
            return null;
        } finally {
            maintenanceLock.readLock().unlock();
        }
    }

    public Set<ParkingSpot> getAllSpots() {
        Set<ParkingSpot> allSpots = new HashSet<>();
        for (Set<ParkingSpot> set : spotMap.values()) {
            allSpots.addAll(set);
        }
        return allSpots;
    }

    public String getFloorId() {
        return floorId;
    }

    public boolean isUnderMaintenance() {
        return underMaintenance;
    }

    /**
     * Sets the maintenance status of the floor.
     * Thread-safe write operation.
     * 
     * @param status true to set under maintenance, false otherwise
     */
    public void setUnderMaintenance(boolean status) {
        maintenanceLock.writeLock().lock();
        try {
            this.underMaintenance = status;
        } finally {
            maintenanceLock.writeLock().unlock();
        }
    }

    public Map<SpotType, Set<ParkingSpot>> getSpotMap() {
        return spotMap;
    }

    public void showFloorDisplay() {
        maintenanceLock.readLock().lock();
        try {
            displayPanel.displayAvailableSpots(spotMap, underMaintenance);
        } finally {
            maintenanceLock.readLock().unlock();
        }
    }

    /**
     * Checks if the floor is full.
     * Thread-safe read operation.
     * 
     * @return true if all spots are occupied, false otherwise
     */
    public boolean isFull() {
        maintenanceLock.readLock().lock();
        try {
            for (Set<ParkingSpot> spots : spotMap.values()) {
                for (ParkingSpot spot : spots) {
                    if (!spot.isOccupied()) {
                        return false; // At least one spot is available
                    }
                }
            }
            return true; // All spots are occupied
        } finally {
            maintenanceLock.readLock().unlock();
        }
    }

    public ParkingSpot getSpotById(String spotId) {
        for (Set<ParkingSpot> spots : spotMap.values()) {
            for (ParkingSpot spot : spots) {
                if (spot.getId().equals(spotId)) {
                    return spot;
                }
            }
        }
        return null;
    }
}
