package com.airtribe.panels;

import com.airtribe.ParkingLot;
import com.airtribe.ParkingSpot;
import com.airtribe.coststrategy.CostComputationStrategy;
import com.airtribe.displaypanel.ExitDisplayPanel;
import com.airtribe.parkingticket.ParkingTicket;
import com.airtribe.payment.PaymentProcessor;

public class ExitPanel {
    private final ExitDisplayPanel displayPanel;
    private final PaymentProcessor paymentProcessor;
    private final CostComputationStrategy costStrategy;

    public ExitPanel(PaymentProcessor paymentProcessor, CostComputationStrategy costStrategy) {
        this.displayPanel = new ExitDisplayPanel();
        this.paymentProcessor = paymentProcessor;
        this.costStrategy = costStrategy;
    }

    /**
     * Unparks a vehicle in a thread-safe manner.
     * Ensures that only one thread can process a ticket at a time.
     * 
     * @param ticket The parking ticket
     * @param parkingLot The parking lot
     */
    public synchronized void unparkVehicle(ParkingTicket ticket, ParkingLot parkingLot) {
        if (ticket == null || ticket.getSpotId() == null) {
            displayPanel.displayError("Invalid parking ticket");
            return;
        }

        // Verify ticket still exists (not already processed)
        ParkingTicket activeTicket = parkingLot.getTicket(ticket.getTicketId());
        if (activeTicket == null) {
            displayPanel.displayError("Ticket already processed or invalid: " + ticket.getTicketId());
            return;
        }

        // Calculate cost using the strategy pattern
        double cost = costStrategy.computeCost(ticket);

        // Retrieve the spot from ParkingLot by spotId
        ParkingSpot spot = parkingLot.getSpotById(ticket.getSpotId());
        if (spot == null) {
            displayPanel.displayError("Parking spot not found: " + ticket.getSpotId());
            return;
        }

        // Verify the spot is occupied
        if (!spot.isOccupied()) {
            displayPanel.displayError("Parking spot is already empty: " + ticket.getSpotId());
            return;
        }

        // Process payment first (before releasing the spot)
        boolean paymentSuccess = paymentProcessor.processPayment(cost);

        if (paymentSuccess) {
            // Remove vehicle from spot (thread-safe operation)
            spot.removeVehicle();
            
            // Remove ticket from active tickets
            parkingLot.removeTicket(ticket.getTicketId());
            
            // Display success messages
            displayPanel.displayCost(ticket.getVehicle(), cost);
            displayPanel.displayExitSuccess(ticket.getVehicle());
        } else {
            displayPanel.displayError("Payment failed. Please try again.");
        }
    }
}
