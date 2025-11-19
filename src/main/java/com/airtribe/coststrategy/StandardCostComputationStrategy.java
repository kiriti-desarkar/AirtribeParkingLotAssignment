package com.airtribe.coststrategy;

import com.airtribe.SpotType;
import com.airtribe.VehicleType;
import com.airtribe.parkingticket.ParkingTicket;

/**
 * Standard implementation of CostComputationStrategy that calculates
 * parking costs based on duration, vehicle type, and spot type.
 */
public class StandardCostComputationStrategy implements CostComputationStrategy {

    // Base rates per hour for different vehicle types
    private static final double MOTORCYCLE_BASE_RATE = 5.0;
    private static final double CAR_BASE_RATE = 10.0;
    private static final double BUS_BASE_RATE = 25.0;
    private static final double TRUCK_BASE_RATE = 20.0;

    // Multipliers for different spot types
    private static final double SMALL_SPOT_MULTIPLIER = 1.0;
    private static final double MEDIUM_SPOT_MULTIPLIER = 1.3;
    private static final double LARGE_SPOT_MULTIPLIER = 1.5;

    // Minimum charge (e.g., 30 minutes minimum)
    private static final double MINIMUM_HOURS = 0.5;

    @Override
    public double computeCost(ParkingTicket ticket) {
        if (ticket == null) {
            throw new IllegalArgumentException("Parking ticket cannot be null");
        }

        // Calculate parking duration in hours
        long durationMillis = System.currentTimeMillis() - ticket.getEntryTime();
        double durationHours = Math.max(durationMillis / (1000.0 * 60 * 60), MINIMUM_HOURS);

        // Get base rate based on vehicle type
        double baseRate = getBaseRateForVehicle(ticket.getVehicle().getType());

        // Get multiplier based on spot type
        double spotMultiplier = getSpotMultiplier(ticket.getSpotType());

        // Calculate total cost
        double totalCost = durationHours * baseRate * spotMultiplier;

        // Round to 2 decimal places
        return Math.round(totalCost * 100.0) / 100.0;
    }

    private double getBaseRateForVehicle(VehicleType vehicleType) {
        return switch (vehicleType) {
            case MOTORCYCLE -> MOTORCYCLE_BASE_RATE;
            case CAR -> CAR_BASE_RATE;
            case BUS -> BUS_BASE_RATE;
            case TRUCK -> TRUCK_BASE_RATE;
            default -> CAR_BASE_RATE; // Default to car rate
        };
    }

    private double getSpotMultiplier(String spotType) {
        try {
            SpotType type = SpotType.valueOf(spotType.toUpperCase());
            return switch (type) {
                case SMALL -> SMALL_SPOT_MULTIPLIER;
                case MEDIUM -> MEDIUM_SPOT_MULTIPLIER;
                case LARGE -> LARGE_SPOT_MULTIPLIER;
                default -> SMALL_SPOT_MULTIPLIER;
            };
        } catch (IllegalArgumentException e) {
            return SMALL_SPOT_MULTIPLIER; // Default to small rate
        }
    }
}
