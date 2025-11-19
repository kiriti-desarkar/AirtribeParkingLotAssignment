package com.airtribe;

import java.util.concurrent.locks.ReentrantLock;

public class ParkingSpot {

    private final String id;
    private final SpotType spotType;
    private volatile boolean isOccupied;
    private Vehicle parkedVehicle;
    private final ReentrantLock lock = new ReentrantLock();

    public ParkingSpot(String id, SpotType spotType) {
        this.id = id;
        this.spotType = spotType;
        this.isOccupied = false;
    }

    public boolean canFitVehicle(Vehicle vehicle) {
        if (isOccupied) {
            return false;
        }

        return switch (vehicle.getType()) {
            case MOTORCYCLE -> spotType == SpotType.SMALL;
            case CAR -> spotType == SpotType.SMALL || spotType == SpotType.MEDIUM;
            case BUS, TRUCK -> spotType == SpotType.LARGE;
        };
    }

    /**
     * Attempts to park a vehicle in this spot.
     * Thread-safe operation using lock.
     * @return true if parking was successful, false if spot was taken by another thread
     */
    public boolean parkVehicle(Vehicle vehicle) {
        lock.lock();
        try {
            if (isOccupied) {
                return false; // Spot was already taken
            }
            this.parkedVehicle = vehicle;
            this.isOccupied = true;
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes vehicle from the spot.
     * Thread-safe operation using lock.
     */
    public void removeVehicle() {
        lock.lock();
        try {
            this.parkedVehicle = null;
            this.isOccupied = false;
        } finally {
            lock.unlock();
        }
    }

    public boolean isOccupied() {
        return isOccupied;
    }

    public String getId() {
        return id;
    }

    public SpotType getSpotType() {
        return spotType;
    }

    public Vehicle getParkedVehicle() {
        lock.lock();
        try {
            return parkedVehicle;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Tries to acquire the lock for this spot.
     * Used for atomic check-and-park operations.
     */
    public boolean tryLock() {
        return lock.tryLock();
    }

    /**
     * Releases the lock for this spot.
     */
    public void unlock() {
        lock.unlock();
    }
}
