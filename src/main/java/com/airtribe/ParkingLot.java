package com.airtribe;

import com.airtribe.coststrategy.CostComputationStrategy;
import com.airtribe.panels.EntryPanel;
import com.airtribe.panels.ExitPanel;
import com.airtribe.parkingfloor.ParkingFloor;
import com.airtribe.parkingstrategy.ParkingStrategy;
import com.airtribe.parkingticket.ParkingTicket;
import com.airtribe.payment.PaymentProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ParkingLot {
    private final List<ParkingFloor> floors;
    private final EntryPanel entryPanel;
    private ExitPanel exitPanel;
    // Using ConcurrentHashMap for thread-safe ticket management
    private final ConcurrentHashMap<String, ParkingTicket> activeTickets = new ConcurrentHashMap<>();

    public ParkingLot(ParkingStrategy strategy, PaymentProcessor paymentProcessor,
                      CostComputationStrategy costStrategy) {
        this.floors = new ArrayList<>();
        this.entryPanel = new EntryPanel(strategy);
        this.exitPanel = new ExitPanel(paymentProcessor, costStrategy);
    }

    public void addFloor(ParkingFloor floor) {
        floors.add(floor);
    }

    public List<ParkingFloor> getFloors() {
        return floors;
    }

    public EntryPanel getEntryPanel() {
        return entryPanel;
    }

    public ExitPanel getExitPanel() {
        return exitPanel;
    }

    public void changeStrategy(ParkingStrategy strategy) {
        entryPanel.changeStrategy(strategy);
    }

    /**
     * Issues a parking ticket. Thread-safe operation.
     */
    public void issueTicket(ParkingTicket ticket) {
        activeTickets.put(ticket.getTicketId(), ticket);
    }

    /**
     * Retrieves a parking ticket. Thread-safe operation.
     */
    public ParkingTicket getTicket(String ticketId) {
        return activeTickets.get(ticketId);
    }

    /**
     * Removes a ticket from active tickets. Thread-safe operation.
     * Should be called after successful exit.
     */
    public ParkingTicket removeTicket(String ticketId) {
        return activeTickets.remove(ticketId);
    }

    public void setExitPanel(ExitPanel exitPanel) {
        this.exitPanel = exitPanel;
    }

    public ParkingSpot getSpotById(String spotId) {
        for (ParkingFloor floor : floors) {
            ParkingSpot spot = floor.getSpotById(spotId);
            if (spot != null) {
                return spot;
            }
        }
        return null;
    }

    public void isParkingLotFull() {
        for (ParkingFloor floor : floors) {
            if (!floor.isFull()) {
                System.out.println("Parking lot is not full.");
                return;
            }
        }
        System.out.println("Parking lot is full.");
    }

    /**
     * Gets the count of active tickets. Thread-safe operation.
     */
    public int getActiveTicketCount() {
        return activeTickets.size();
    }
}
