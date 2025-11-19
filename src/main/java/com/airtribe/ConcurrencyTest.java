package com.airtribe;

import com.airtribe.coststrategy.StandardCostComputationStrategy;
import com.airtribe.parkingfloor.ParkingFloor;
import com.airtribe.parkingstrategy.NearestAvailableSpotStrategy;
import com.airtribe.parkingticket.ParkingTicket;
import com.airtribe.payment.CardPaymentProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Demonstrates concurrent handling of multiple vehicles entering and exiting simultaneously.
 * This test simulates real-world scenarios with multiple threads accessing the parking lot.
 */
public class ConcurrencyTest {
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Parking Lot Concurrency Test ===\n");
        
        // Setup parking lot
        ParkingLot parkingLot = setupParkingLot();
        
        // Test 1: Concurrent Entry
        System.out.println("\n--- Test 1: Multiple vehicles entering simultaneously ---");
        testConcurrentEntry(parkingLot);
        
        Thread.sleep(2000);
        
        // Test 2: Concurrent Exit
        System.out.println("\n--- Test 2: Multiple vehicles exiting simultaneously ---");
        testConcurrentExit(parkingLot);
        
        Thread.sleep(2000);
        
        // Test 3: Mixed Entry and Exit
        System.out.println("\n--- Test 3: Mixed entry and exit operations ---");
        testMixedOperations(parkingLot);
        
        System.out.println("\n=== All concurrency tests completed ===");
    }
    
    private static ParkingLot setupParkingLot() {
        ParkingLot lot = new ParkingLot(
            new NearestAvailableSpotStrategy(),
            new CardPaymentProcessor(),
            new StandardCostComputationStrategy()
        );
        
        // Create multiple floors with various spots
        ParkingFloor floor1 = new ParkingFloor("F1");
        floor1.addSpot(new ParkingSpot("F1-S1", SpotType.SMALL));
        floor1.addSpot(new ParkingSpot("F1-S2", SpotType.SMALL));
        floor1.addSpot(new ParkingSpot("F1-S3", SpotType.MEDIUM));
        floor1.addSpot(new ParkingSpot("F1-S4", SpotType.MEDIUM));
        floor1.addSpot(new ParkingSpot("F1-S5", SpotType.LARGE));
        lot.addFloor(floor1);
        
        ParkingFloor floor2 = new ParkingFloor("F2");
        floor2.addSpot(new ParkingSpot("F2-S1", SpotType.SMALL));
        floor2.addSpot(new ParkingSpot("F2-S2", SpotType.MEDIUM));
        floor2.addSpot(new ParkingSpot("F2-S3", SpotType.LARGE));
        lot.addFloor(floor2);
        
        System.out.println("Parking lot setup complete with 2 floors and 8 spots total");
        return lot;
    }
    
    /**
     * Test concurrent entry: Multiple vehicles trying to enter at the same time
     */
    private static void testConcurrentEntry(ParkingLot parkingLot) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(10);
        
        // Create 10 vehicles trying to enter simultaneously
        List<Vehicle> vehicles = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            VehicleType type = i % 3 == 0 ? VehicleType.TRUCK : 
                              i % 2 == 0 ? VehicleType.CAR : VehicleType.MOTORCYCLE;
            vehicles.add(new Vehicle("KA-" + String.format("%02d", i) + "-" + (1000 + i), type));
        }
        
        // Submit parking tasks
        for (Vehicle vehicle : vehicles) {
            executor.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();
                    
                    long startTime = System.currentTimeMillis();
                    ParkingTicket ticket = parkingLot.getEntryPanel().parkVehicle(vehicle, parkingLot);
                    long endTime = System.currentTimeMillis();
                    
                    if (ticket != null) {
                        System.out.println("✓ " + vehicle.getLicenseNumber() + " parked at " +
                                         ticket.getSpotId() + " (took " + (endTime - startTime) + "ms)");
                    } else {
                        System.out.println("✗ " + vehicle.getLicenseNumber() + " could not find parking");
                    }
                } catch (Exception e) {
                    System.err.println("Error parking " + vehicle.getLicenseNumber() + ": " + e.getMessage());
                } finally {
                    completionLatch.countDown();
                }
            });
        }
        
        // Start all threads simultaneously
        System.out.println("Starting concurrent entry for " + vehicles.size() + " vehicles...");
        startLatch.countDown();
        
        // Wait for all to complete
        completionLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        
        System.out.println("Active tickets in system: " + parkingLot.getActiveTicketCount());
    }
    
    /**
     * Test concurrent exit: Multiple vehicles trying to exit at the same time
     */
    private static void testConcurrentExit(ParkingLot parkingLot) throws InterruptedException {
        // First, park some vehicles
        List<ParkingTicket> tickets = new ArrayList<>();
        System.out.println("Parking vehicles first...");
        
        for (int i = 1; i <= 5; i++) {
            Vehicle vehicle = new Vehicle("KA-50-" + (5000 + i), VehicleType.CAR);
            ParkingTicket ticket = parkingLot.getEntryPanel().parkVehicle(vehicle, parkingLot);
            if (ticket != null) {
                tickets.add(ticket);
                System.out.println("Parked: " + vehicle.getLicenseNumber() + " at " + ticket.getSpotId());
            }
        }
        
        Thread.sleep(1000); // Simulate parking time
        
        // Now try concurrent exit
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(tickets.size());
        
        System.out.println("\nStarting concurrent exit for " + tickets.size() + " vehicles...");
        
        for (ParkingTicket ticket : tickets) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    long startTime = System.currentTimeMillis();
                    parkingLot.getExitPanel().unparkVehicle(ticket, parkingLot);
                    long endTime = System.currentTimeMillis();
                    
                    System.out.println("✓ " + ticket.getVehicle().getLicenseNumber() +
                                     " exited (took " + (endTime - startTime) + "ms)");
                } catch (Exception e) {
                    System.err.println("Error exiting " + ticket.getVehicle().getLicenseNumber() +
                                     ": " + e.getMessage());
                } finally {
                    completionLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        completionLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        
        System.out.println("Active tickets in system: " + parkingLot.getActiveTicketCount());
    }
    
    /**
     * Test mixed operations: Vehicles entering and exiting simultaneously
     */
    private static void testMixedOperations(ParkingLot parkingLot) throws InterruptedException {
        // First park some vehicles
        List<ParkingTicket> ticketsToExit = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Vehicle vehicle = new Vehicle("KA-70-" + (7000 + i), VehicleType.CAR);
            ParkingTicket ticket = parkingLot.getEntryPanel().parkVehicle(vehicle, parkingLot);
            if (ticket != null) {
                ticketsToExit.add(ticket);
            }
        }
        
        Thread.sleep(500);
        
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(6);
        
        System.out.println("Starting mixed entry/exit operations...");
        
        // Submit entry tasks
        for (int i = 1; i <= 3; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Vehicle vehicle = new Vehicle("KA-80-" + (8000 + index), VehicleType.MOTORCYCLE);
                    ParkingTicket ticket = parkingLot.getEntryPanel().parkVehicle(vehicle, parkingLot);
                    if (ticket != null) {
                        System.out.println("✓ [ENTRY] " + vehicle.getLicenseNumber() + " parked");
                    }
                } catch (Exception e) {
                    System.err.println("Entry error: " + e.getMessage());
                } finally {
                    completionLatch.countDown();
                }
            });
        }
        
        // Submit exit tasks
        for (ParkingTicket ticket : ticketsToExit) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    parkingLot.getExitPanel().unparkVehicle(ticket, parkingLot);
                    System.out.println("✓ [EXIT] " + ticket.getVehicle().getLicenseNumber() + " exited");
                } catch (Exception e) {
                    System.err.println("Exit error: " + e.getMessage());
                } finally {
                    completionLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        completionLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        
        System.out.println("Active tickets in system: " + parkingLot.getActiveTicketCount());
    }
}
