package com.airtribe.panels;

import com.airtribe.ParkingLot;
import com.airtribe.ParkingSpot;
import com.airtribe.Vehicle;
import com.airtribe.displaypanel.EntryDisplayPanel;
import com.airtribe.parkingstrategy.ParkingStrategy;
import com.airtribe.parkingticket.ParkingTicket;
import com.airtribe.parkingticket.ParkingTicketGenerator;

public class EntryPanel {
    private volatile ParkingStrategy strategy;
    private final EntryDisplayPanel displayPanel;
    private final ParkingTicketGenerator ticketGenerator;

    public EntryPanel(ParkingStrategy strategy) {
        this(strategy, new ParkingTicketGenerator());
    }

    public EntryPanel(ParkingStrategy strategy, ParkingTicketGenerator ticketGenerator) {
        this.strategy = strategy;
        this.displayPanel = new EntryDisplayPanel();
        this.ticketGenerator = ticketGenerator;
    }

    public synchronized void changeStrategy(ParkingStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Parks a vehicle in a thread-safe manner.
     * Handles race conditions where multiple threads might try to park in the same spot.
     * 
     * @param vehicle The vehicle to park
     * @param parkingLot The parking lot
     * @return ParkingTicket if successful, null otherwise
     */
    public ParkingTicket parkVehicle(Vehicle vehicle, ParkingLot parkingLot) {
        // Maximum retry attempts in case of race conditions
        int maxRetries = 3;
        int attempt = 0;
        
        while (attempt < maxRetries) {
            // Find an available spot using the current strategy
            ParkingSpot spot = strategy.findSpot(vehicle, parkingLot);
            
            if (spot == null) {
                // No spot available
                displayPanel.displayTicketIssued(null);
                return null;
            }
            
            // Try to park the vehicle (atomic operation)
            boolean parked = spot.parkVehicle(vehicle);
            
            if (parked) {
                // Successfully parked, generate ticket
                ParkingTicket ticket = ticketGenerator.generateTicket(vehicle, spot);
                parkingLot.issueTicket(ticket);
                displayPanel.displayTicketIssued(ticket);
                return ticket;
            }
            
            // Spot was taken by another thread, retry
            attempt++;
            System.out.println("Race condition detected for vehicle " + vehicle.getLicensePlate() + 
                             ". Retrying... (Attempt " + attempt + "/" + maxRetries + ")");
            
            // Small delay before retry to reduce contention
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // Failed to park after retries
        System.out.println("Failed to park vehicle " + vehicle.getLicensePlate() + 
                         " after " + maxRetries + " attempts");
        displayPanel.displayTicketIssued(null);
        return null;
    }
}
