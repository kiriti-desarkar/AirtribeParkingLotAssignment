package com.airtribe;

import com.airtribe.parkingfloor.ParkingFloor;
import com.airtribe.parkingstrategy.NearestAvailableSpotStrategy;
import com.airtribe.parkingstrategy.RandomSpotStrategy;
import com.airtribe.parkingticket.ParkingTicket;
import com.airtribe.coststrategy.StandardCostComputationStrategy;
import com.airtribe.payment.CardPaymentProcessor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Parking Lot Management System ===\n");
        
        // Basic sequential demo
        runSequentialDemo();
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("=== Concurrent Operations Demo ===");
        System.out.println("=".repeat(50) + "\n");
        
        // Concurrent demo
        runConcurrentDemo();
        
        System.out.println("\n=== Demo Complete ===");
        System.out.println("For comprehensive concurrency testing, run ConcurrencyTest.java");
    }
    
    private static void runSequentialDemo() throws InterruptedException {
        System.out.println("--- Sequential Demo ---");
        
        ParkingLot lot = new ParkingLot(new NearestAvailableSpotStrategy(),
                new CardPaymentProcessor(), new StandardCostComputationStrategy());

        ParkingFloor floor1 = new ParkingFloor("F1");
        floor1.addSpot(new ParkingSpot("F1-S1", SpotType.SMALL));
        floor1.addSpot(new ParkingSpot("F1-S2", SpotType.MEDIUM));
        floor1.addSpot(new ParkingSpot("F1-S3", SpotType.LARGE));
        lot.addFloor(floor1);

        ParkingFloor floor2 = new ParkingFloor("F2");
        floor2.addSpot(new ParkingSpot("F2-S1", SpotType.SMALL));
        floor2.addSpot(new ParkingSpot("F2-S2", SpotType.LARGE));
        lot.addFloor(floor2);

        ParkingFloor floor3 = new ParkingFloor("F3");
        floor3.setUnderMaintenance(true);
        floor3.addSpot(new ParkingSpot("F3-S1", SpotType.MEDIUM));
        lot.addFloor(floor3);

        for (ParkingFloor floor : lot.getFloors()) {
            floor.showFloorDisplay();
        }

        // Vehicles
        Vehicle car1 = new Vehicle("KA-01-1234", VehicleType.CAR);
        Vehicle truck1 = new Vehicle("KA-99-8888", VehicleType.TRUCK);
        Vehicle car2 = new Vehicle("KA-05-5678", VehicleType.CAR);
        Vehicle bus1 = new Vehicle("KA-09-0001", VehicleType.BUS);

        // Park vehicles
        ParkingTicket t1 = lot.getEntryPanel().parkVehicle(car1, lot);
        ParkingTicket t2 = lot.getEntryPanel().parkVehicle(truck1, lot);
        ParkingTicket t3 = lot.getEntryPanel().parkVehicle(car2, lot);
        ParkingTicket t4 = lot.getEntryPanel().parkVehicle(bus1, lot);

        // Simulate time passage
        Thread.sleep(2000);

        // Unpark vehicles
        lot.getExitPanel().unparkVehicle(t1, lot);
        lot.getExitPanel().unparkVehicle(t2, lot);
        lot.getExitPanel().unparkVehicle(t3, lot);

        // Change strategy and retry parking
        System.out.println("\nSwitching to Random Spot Strategy...");
        lot.changeStrategy(new RandomSpotStrategy());

        Vehicle truck2 = new Vehicle("KA-77-7777", VehicleType.TRUCK);
        ParkingTicket t6 = lot.getEntryPanel().parkVehicle(truck2, lot);

        Thread.sleep(1000);
        lot.getExitPanel().unparkVehicle(t4, lot);
        lot.getExitPanel().unparkVehicle(t6, lot);
    }
    
    private static void runConcurrentDemo() throws InterruptedException {
        // Setup parking lot
        ParkingLot lot = new ParkingLot(new NearestAvailableSpotStrategy(),
                new CardPaymentProcessor(), new StandardCostComputationStrategy());

        ParkingFloor floor1 = new ParkingFloor("F1");
        floor1.addSpot(new ParkingSpot("F1-S1", SpotType.SMALL));
        floor1.addSpot(new ParkingSpot("F1-S2", SpotType.SMALL));
        floor1.addSpot(new ParkingSpot("F1-S3", SpotType.MEDIUM));
        floor1.addSpot(new ParkingSpot("F1-S4", SpotType.LARGE));
        lot.addFloor(floor1);
        
        System.out.println("Parking lot setup: 1 floor, 4 spots");
        System.out.println("Simulating 6 vehicles arriving simultaneously...\n");
        
        // Create thread pool
        ExecutorService executor = Executors.newFixedThreadPool(6);
        
        // Create 6 vehicles trying to park simultaneously
        Vehicle[] vehicles = {
            new Vehicle("KA-10-1001", VehicleType.MOTORCYCLE),
            new Vehicle("KA-10-1002", VehicleType.CAR),
            new Vehicle("KA-10-1003", VehicleType.CAR),
            new Vehicle("KA-10-1004", VehicleType.TRUCK),
            new Vehicle("KA-10-1005", VehicleType.MOTORCYCLE),
            new Vehicle("KA-10-1006", VehicleType.CAR)
        };
        
        // Submit all parking tasks concurrently
        for (int i = 0; i < vehicles.length; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    // Small random delay to simulate real-world arrival
                    Thread.sleep((long)(Math.random() * 100));
                    
                    ParkingTicket ticket = lot.getEntryPanel().parkVehicle(vehicles[index], lot);
                    if (ticket != null) {
                        System.out.println("✓ Thread " + Thread.currentThread().getName() + 
                                         ": " + vehicles[index].getLicensePlate() + 
                                         " successfully parked at " + ticket.getSpotId());
                    } else {
                        System.out.println("✗ Thread " + Thread.currentThread().getName() + 
                                         ": " + vehicles[index].getLicensePlate() + 
                                         " could not find parking (lot may be full)");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        // Shutdown and wait
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        System.out.println("\nFinal state: " + lot.getActiveTicketCount() + " vehicles parked");
        System.out.println("\nNote: Some vehicles couldn't park because there were only 4 spots for 6 vehicles.");
        System.out.println("The concurrency mechanism prevented double-booking and race conditions!");
    }
}
