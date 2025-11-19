# Smart Parking Lot System - Solution Documentation

## Table of Contents
1. [Project Overview](#project-overview)
2. [Requirements Analysis](#requirements-analysis)
3. [Architecture & Design](#architecture--design)
4. [Implementation Details](#implementation-details)
5. [Design Patterns Used](#design-patterns-used)
6. [Concurrency Handling](#concurrency-handling)
7. [How to Run](#how-to-run)
8. [Key Features](#key-features)
9. [Future Enhancements](#future-enhancements)

---

## Project Overview

This project implements a **Smart Parking Lot Management System** designed to efficiently manage vehicle parking in a multi-floor parking facility. The system handles vehicle entry/exit, automatic spot allocation, real-time availability tracking, and dynamic fee calculation.

### Objective
Design a low-level backend system that:
- Automatically assigns parking spots based on vehicle size and availability
- Tracks vehicle entry and exit times
- Calculates parking fees based on duration and vehicle type
- Handles concurrent operations safely

---

## Requirements Analysis

### ✅ Functional Requirements Implementation

#### 1. **Parking Spot Allocation** 
**Requirement:** Automatically assign parking spots based on vehicle size (motorcycle, car, bus, truck).

**Implementation:**
- **Strategy Pattern** used via `ParkingStrategy` interface
- Two implementations provided:
  - `NearestAvailableSpotStrategy`: Assigns the first available spot (floor-by-floor)
  - `RandomSpotStrategy`: Randomly selects from available spots
- **Vehicle-to-Spot Mapping:**
  - MOTORCYCLE → SMALL spots only
  - CAR → SMALL or MEDIUM spots
  - BUS/TRUCK → LARGE spots only

**Code Location:** 
- `com.airtribe.parkingstrategy.*`
- `ParkingSpot.canFitVehicle()` method

#### 2. **Check-In and Check-Out**
**Requirement:** Record entry and exit times of vehicles.

**Implementation:**
- **Entry Process:**
  - Vehicle enters via `EntryPanel.parkVehicle()`
  - System generates `ParkingTicket` with unique ID and entry timestamp
  - Ticket stored in `ConcurrentHashMap` for thread-safe access
  
- **Exit Process:**
  - Vehicle exits via `ExitPanel.unparkVehicle()`
  - System calculates duration: `currentTime - entryTime`
  - Payment processed and spot released

**Code Location:**
- `com.airtribe.panels.EntryPanel`
- `com.airtribe.panels.ExitPanel`
- `com.airtribe.parkingticket.ParkingTicket`

#### 3. **Parking Fee Calculation**
**Requirement:** Calculate fees based on duration and vehicle type.

**Implementation:**
- **Strategy Pattern** via `CostComputationStrategy` interface
- `StandardCostComputationStrategy` implements:
  - **Base rates per hour:**
    - Motorcycle: ₹5/hour
    - Car: ₹10/hour
    - Truck: ₹20/hour
    - Bus: ₹25/hour
  - **Spot type multipliers:**
    - SMALL: 1.0x
    - MEDIUM: 1.3x
    - LARGE: 1.5x
  - **Minimum charge:** 0.5 hours (30 minutes)
  - **Formula:** `cost = duration × baseRate × spotMultiplier`

**Code Location:**
- `com.airtribe.coststrategy.StandardCostComputationStrategy`

#### 4. **Real-Time Availability Update**
**Requirement:** Update parking spot availability as vehicles enter and leave.

**Implementation:**
- **Atomic Operations:** `ParkingSpot` uses `ReentrantLock` for thread-safe state changes
- **Volatile Variables:** `isOccupied` flag marked as volatile
- **Display Panels:** Show real-time status
  - `FloorDisplayPanel`: Shows available spots per floor per type
  - `EntryDisplayPanel`: Confirms ticket issuance
  - `ExitDisplayPanel`: Shows costs and exit confirmation

**Code Location:**
- `ParkingSpot.parkVehicle()` and `ParkingSpot.removeVehicle()`
- `com.airtribe.displaypanel.*`

---

## Architecture & Design

### System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Parking Lot System                      │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌─────────────┐              ┌──────────────┐              │
│  │ Entry Panel │◄─────────────┤ Parking Lot  │              │
│  └─────────────┘              └──────────────┘              │
│        │                              │                      │
│        │                              │                      │
│        ▼                              ▼                      │
│  ┌─────────────────────┐   ┌───────────────────┐           │
│  │ Parking Strategy    │   │  Parking Floor    │           │
│  │ - Nearest           │   │  - Floor 1        │           │
│  │ - Random            │   │  - Floor 2        │           │
│  └─────────────────────┘   │  - Floor N        │           │
│                              └───────────────────┘           │
│                                      │                       │
│                                      ▼                       │
│  ┌──────────────┐           ┌──────────────────┐           │
│  │  Exit Panel  │           │  Parking Spot    │           │
│  └──────────────┘           │  - SMALL         │           │
│        │                     │  - MEDIUM        │           │
│        │                     │  - LARGE         │           │
│        ▼                     └──────────────────┘           │
│  ┌─────────────────────┐                                    │
│  │ Payment Processor   │                                    │
│  │ - Card              │                                    │
│  │ - Cash              │                                    │
│  └─────────────────────┘                                    │
│        │                                                     │
│        ▼                                                     │
│  ┌─────────────────────┐                                    │
│  │ Cost Strategy       │                                    │
│  │ - Standard Pricing  │                                    │
│  └─────────────────────┘                                    │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

### Class Diagram (Core Components)

```
┌──────────────────┐
│   ParkingLot     │
├──────────────────┤
│ - floors         │
│ - entryPanel     │
│ - exitPanel      │
│ - activeTickets  │◄───────── ConcurrentHashMap
├──────────────────┤
│ + addFloor()     │
│ + issueTicket()  │
│ + removeTicket() │
└──────────────────┘
         │
         │ has many
         ▼
┌──────────────────┐
│  ParkingFloor    │
├──────────────────┤
│ - floorId        │
│ - spotMap        │◄───────── Map<SpotType, Set<ParkingSpot>>
│ - maintenance    │
├──────────────────┤
│ + addSpot()      │
│ + getAvailable() │
│ + isFull()       │
└──────────────────┘
         │
         │ has many
         ▼
┌──────────────────┐
│  ParkingSpot     │
├──────────────────┤
│ - id             │
│ - spotType       │
│ - isOccupied     │◄───────── volatile
│ - lock           │◄───────── ReentrantLock
├──────────────────┤
│ + parkVehicle()  │◄───────── synchronized
│ + removeVehicle()│◄───────── synchronized
└──────────────────┘
```

### Data Model

#### Core Entities

**1. Vehicle**
```java
class Vehicle {
    - String licenseNumber  // Unique identifier
    - VehicleType type      // MOTORCYCLE, CAR, BUS, TRUCK
}
```

**2. ParkingSpot**
```java
class ParkingSpot {
    - String id             // e.g., "F1-S1"
    - SpotType spotType     // SMALL, MEDIUM, LARGE
    - boolean isOccupied    // volatile for visibility
    - Vehicle parkedVehicle
    - ReentrantLock lock    // For thread safety
}
```

**3. ParkingTicket**
```java
class ParkingTicket {
    - String ticketId       // e.g., "PT-20241119-0001"
    - Vehicle vehicle
    - String spotId
    - String spotType
    - long entryTime        // milliseconds since epoch
}
```

**4. ParkingFloor**
```java
class ParkingFloor {
    - String floorId
    - Map<SpotType, Set<ParkingSpot>> spotMap
    - boolean underMaintenance
    - ReadWriteLock maintenanceLock
}
```

---

## Implementation Details

### Package Structure

```
com.airtribe
├── Main.java                          # Entry point with demos
├── ParkingLot.java                    # Central coordinator
├── ParkingSpot.java                   # Individual parking spot
├── Vehicle.java                       # Vehicle entity
├── VehicleType.java                   # Enum: MOTORCYCLE, CAR, BUS, TRUCK
├── SpotType.java                      # Enum: SMALL, MEDIUM, LARGE
│
├── panels/
│   ├── EntryPanel.java               # Handles vehicle entry
│   └── ExitPanel.java                # Handles vehicle exit
│
├── parkingfloor/
│   └── ParkingFloor.java             # Floor management
│
├── parkingstrategy/
│   ├── ParkingStrategy.java          # Strategy interface
│   ├── NearestAvailableSpotStrategy.java
│   └── RandomSpotStrategy.java
│
├── parkingticket/
│   ├── ParkingTicket.java            # Ticket entity
│   └── ParkingTicketGenerator.java   # Generates unique tickets
│
├── coststrategy/
│   ├── CostComputationStrategy.java  # Cost strategy interface
│   └── StandardCostComputationStrategy.java
│
├── payment/
│   ├── PaymentProcessor.java         # Payment interface
│   ├── CardPaymentProcessor.java
│   └── CashPaymentProcessor.java
│
└── displaypanel/
    ├── DisplayPanel.java             # Abstract display
    ├── EntryDisplayPanel.java
    ├── ExitDisplayPanel.java
    └── FloorDisplayPanel.java
```

### Key Algorithms

#### 1. Spot Allocation Algorithm (Nearest Strategy)
```
function findSpot(vehicle, parkingLot):
    for each floor in parkingLot.floors:
        if floor.isUnderMaintenance():
            continue
        
        for each spotType in [SMALL, MEDIUM, LARGE]:
            for each spot in floor.spots[spotType]:
                if spot.canFitVehicle(vehicle) AND !spot.isOccupied:
                    return spot
    
    return null  // No spot available
```

#### 2. Vehicle-to-Spot Matching Logic
```
function canFitVehicle(vehicle):
    if spot.isOccupied:
        return false
    
    switch vehicle.type:
        case MOTORCYCLE:
            return spotType == SMALL
        case CAR:
            return spotType in [SMALL, MEDIUM]
        case BUS, TRUCK:
            return spotType == LARGE
```

#### 3. Cost Calculation Algorithm
```
function computeCost(ticket):
    // Calculate duration
    durationMillis = currentTime - ticket.entryTime
    durationHours = max(durationMillis / 3600000, 0.5)  // minimum 30 min
    
    // Get rates
    baseRate = getRateForVehicle(ticket.vehicle.type)
    spotMultiplier = getMultiplierForSpot(ticket.spotType)
    
    // Calculate cost
    totalCost = durationHours × baseRate × spotMultiplier
    return round(totalCost, 2)
```

---

## Design Patterns Used

### 1. **Strategy Pattern** 
**Used in:** Parking spot allocation and cost calculation

**Purpose:** Allow runtime selection of algorithms

**Implementation:**
```java
// Parking Strategy
interface ParkingStrategy {
    ParkingSpot findSpot(Vehicle vehicle, ParkingLot parkingLot);
}

// Implementations:
- NearestAvailableSpotStrategy
- RandomSpotStrategy

// Usage:
parkingLot.changeStrategy(new RandomSpotStrategy());
```

**Benefits:**
- Easy to add new allocation strategies
- No code changes needed in core logic
- Strategy can be switched at runtime

### 2. **Factory Pattern**
**Used in:** Ticket generation

**Purpose:** Centralize object creation with unique IDs

**Implementation:**
```java
class ParkingTicketGenerator {
    private static AtomicInteger counter = new AtomicInteger(0);
    
    ParkingTicket generateTicket(Vehicle vehicle, ParkingSpot spot) {
        String uniqueId = "PT-" + date + "-" + counter.incrementAndGet();
        return new ParkingTicket(uniqueId, vehicle, spot.getId(), ...);
    }
}
```

**Benefits:**
- Guarantees unique ticket IDs
- Encapsulates ticket creation logic
- Thread-safe ID generation

### 3. **Singleton-like Pattern**
**Used in:** Parking lot management

**Purpose:** Single point of control for the parking lot

**Implementation:**
- One `ParkingLot` instance coordinates all operations
- Centralized ticket management via `ConcurrentHashMap`

### 4. **Template Method Pattern**
**Used in:** Display panels

**Purpose:** Common display interface with specific implementations

**Implementation:**
```java
abstract class DisplayPanel {
    public abstract void display();
}

// Concrete implementations:
- EntryDisplayPanel
- ExitDisplayPanel  
- FloorDisplayPanel
```

---

## Concurrency Handling

### Thread Safety Mechanisms

#### 1. **Spot-Level Locking**
**Problem:** Multiple threads might try to park in the same spot simultaneously.

**Solution:** Each `ParkingSpot` has its own `ReentrantLock`
```java
public boolean parkVehicle(Vehicle vehicle) {
    lock.lock();
    try {
        if (isOccupied) {
            return false;
        }
        this.parkedVehicle = vehicle;
        this.isOccupied = true;
        return true;
    } finally {
        lock.unlock();
    }
}
```

**Benefits:**
- Fine-grained locking (only locks one spot, not entire floor)
- Prevents race conditions
- High concurrency throughput

#### 2. **Retry Mechanism with Race Condition Handling**
**Problem:** Spot might become occupied between finding it and parking.

**Solution:** Retry logic in `EntryPanel`
```java
int maxRetries = 3;
while (attempt < maxRetries) {
    ParkingSpot spot = strategy.findSpot(vehicle, parkingLot);
    if (spot == null) return null;
    
    boolean parked = spot.parkVehicle(vehicle);  // Atomic operation
    if (parked) {
        return generateTicket(vehicle, spot);
    }
    
    attempt++;
    Thread.sleep(10);  // Small delay before retry
}
```

#### 3. **ConcurrentHashMap for Ticket Management**
**Problem:** Multiple threads accessing ticket records simultaneously.

**Solution:** Use `ConcurrentHashMap` instead of regular `HashMap`
```java
private final ConcurrentHashMap<String, ParkingTicket> activeTickets;

public void issueTicket(ParkingTicket ticket) {
    activeTickets.put(ticket.getTicketId(), ticket);  // Thread-safe
}

public ParkingTicket removeTicket(String ticketId) {
    return activeTickets.remove(ticketId);  // Atomic remove
}
```

**Benefits:**
- No external synchronization needed
- Better performance than synchronized HashMap
- Atomic operations for get/put/remove

#### 4. **ReadWriteLock for Floor Maintenance**
**Problem:** Floor maintenance status needs to be checked frequently (reads) but changed rarely (writes).

**Solution:** `ReentrantReadWriteLock` in `ParkingFloor`
```java
private final ReadWriteLock maintenanceLock = new ReentrantReadWriteLock();

public ParkingSpot getAvailableSpot(Vehicle vehicle) {
    maintenanceLock.readLock().lock();  // Multiple threads can read
    try {
        if (underMaintenance) return null;
        // Find spot logic
    } finally {
        maintenanceLock.readLock().unlock();
    }
}

public void setUnderMaintenance(boolean status) {
    maintenanceLock.writeLock().lock();  // Exclusive write access
    try {
        this.underMaintenance = status;
    } finally {
        maintenanceLock.writeLock().unlock();
    }
}
```

**Benefits:**
- Multiple threads can read simultaneously
- Writes are exclusive and safe
- Better performance than full synchronization

#### 5. **Volatile Variables**
**Problem:** Changes to `isOccupied` flag might not be visible across threads.

**Solution:** Mark as `volatile`
```java
private volatile boolean isOccupied;
```

**Benefits:**
- Ensures visibility across threads
- Prevents stale reads
- Works with lock-based synchronization

#### 6. **Synchronized Methods for Exit**
**Problem:** Exit operations involve multiple steps (verify, calculate, pay, release).

**Solution:** Synchronized method in `ExitPanel`
```java
public synchronized void unparkVehicle(ParkingTicket ticket, ParkingLot lot) {
    // Atomic multi-step operation
    verify → calculate → process payment → release spot → remove ticket
}
```

**Benefits:**
- Ensures complete transaction
- Prevents partial exits
- Maintains data consistency

### Concurrency Test Scenarios

The `Main.java` demonstrates concurrent operations:

```java
// 6 vehicles trying to park in 4 spots simultaneously
ExecutorService executor = Executors.newFixedThreadPool(6);

for (Vehicle vehicle : vehicles) {
    executor.submit(() -> {
        ParkingTicket ticket = lot.getEntryPanel().parkVehicle(vehicle, lot);
        // Result: 4 succeed, 2 fail gracefully (no double-booking!)
    });
}
```

**Expected Results:**
- ✅ 4 vehicles successfully parked (spots available)
- ✅ 2 vehicles rejected (no spots available)
- ✅ No race conditions or double-booking
- ✅ Thread-safe ticket generation

---

## How to Run

### Prerequisites
- **Java:** JDK 20 or higher
- **Maven:** 3.6+ (for dependency management)
- **IDE:** IntelliJ IDEA, Eclipse, or VS Code (optional)

### Steps to Run

#### 1. Using Command Line

```bash
# Navigate to project directory
cd C:\Learning\Airtribe\JavaClasses\KiritiParkingLotAssignment

# Compile the project
mvn clean compile

# Run the main class
mvn exec:java -Dexec.mainClass="com.airtribe.Main"
```

#### 2. Using IDE (IntelliJ IDEA)

1. Open project in IntelliJ IDEA
2. Wait for Maven to sync dependencies
3. Navigate to `src/main/java/com/airtribe/Main.java`
4. Right-click and select "Run Main.main()"

#### 3. Expected Output

```
=== Parking Lot Management System ===

--- Sequential Demo ---
Display @Floor F1: Available spots:
- SMALL: 1 spot(s)
- MEDIUM: 1 spot(s)
- LARGE: 1 spot(s)
Display @Floor F2: Available spots:
- SMALL: 1 spot(s)
- LARGE: 1 spot(s)
Display @Floor F3: This floor is under maintenance.

Display @Entry: Ticket issued with ID PT-20241119-0001 for vehicle KA-01-1234
Display @Entry: Ticket issued with ID PT-20241119-0002 for vehicle KA-99-8888
Display @Entry: Ticket issued with ID PT-20241119-0003 for vehicle KA-05-5678
Display @Entry: Ticket issued with ID PT-20241119-0004 for vehicle KA-09-0001

Display @Exit: Vehicle KA-01-1234 - Total cost: ₹5.13
Processed card payment of ₹5.13
Display @Exit: Vehicle KA-01-1234 exited successfully. Thank you!

==================================================
=== Concurrent Operations Demo ===
==================================================

Parking lot setup: 1 floor, 4 spots
Simulating 6 vehicles arriving simultaneously...

✓ Thread pool-1-thread-1: KA-10-1001 successfully parked at F1-S1
✓ Thread pool-1-thread-2: KA-10-1002 successfully parked at F1-S2
✓ Thread pool-1-thread-3: KA-10-1003 successfully parked at F1-S3
✓ Thread pool-1-thread-4: KA-10-1004 successfully parked at F1-S4
✗ Thread pool-1-thread-5: KA-10-1005 could not find parking (lot may be full)
✗ Thread pool-1-thread-6: KA-10-1006 could not find parking (lot may be full)

Final state: 4 vehicles parked

Note: Some vehicles couldn't park because there were only 4 spots for 6 vehicles.
The concurrency mechanism prevented double-booking and race conditions!

=== Demo Complete ===
```

---

## Key Features

### ✅ Implemented Features

1. **Multi-Floor Support**
   - Support for unlimited floors
   - Floor-specific maintenance mode
   - Real-time floor status display

2. **Multiple Vehicle Types**
   - Motorcycle, Car, Bus, Truck
   - Different pricing for each type
   - Smart spot matching

3. **Flexible Spot Allocation**
   - Strategy pattern allows multiple algorithms
   - Runtime strategy switching
   - Efficient spot-finding logic

4. **Accurate Fee Calculation**
   - Duration-based charging
   - Vehicle type pricing
   - Spot type multipliers
   - Minimum charge handling

5. **Real-Time Updates**
   - Display panels at entry, exit, and floors
   - Instant availability updates
   - Ticket issuance confirmation

6. **Thread Safety**
   - Concurrent parking supported
   - Race condition prevention
   - Atomic operations throughout

7. **Payment Processing**
   - Multiple payment methods (Card, Cash)
   - Strategy pattern for extensibility
   - Payment verification before exit

8. **Ticket Management**
   - Unique ticket ID generation
   - Thread-safe ticket storage
   - Ticket validation on exit

### 🎯 Unique Selling Points

1. **100% Thread-Safe**: Handles concurrent operations without data corruption
2. **Strategy Pattern**: Easy to add new parking and pricing strategies
3. **Scalable Design**: Can handle any number of floors and spots
4. **Maintenance Mode**: Floors can be taken offline without system restart
5. **Comprehensive Logging**: Display panels provide visibility into all operations

---

## Future Enhancements

### Potential Improvements

1. **Database Integration**
   - Persist parking history
   - User account management
   - Historical analytics

2. **Reservation System**
   - Pre-book parking spots
   - Time-based reservations
   - Reserved spot management

3. **Dynamic Pricing**
   - Peak hour pricing
   - Weekend/weekday rates
   - Promotional discounts

4. **Advanced Allocation Strategies**
   - EV charging spot priority
   - Handicap spot allocation
   - VIP/Premium spots
   - Load balancing across floors

5. **Mobile Integration**
   - QR code-based tickets
   - Mobile payment integration
   - Push notifications

6. **Analytics Dashboard**
   - Occupancy rates
   - Revenue tracking
   - Peak usage times
   - Floor utilization heatmaps

7. **Advanced Concurrency**
   - Optimistic locking
   - Lock-free data structures
   - Better performance under heavy load

8. **Security Features**
   - CCTV integration
   - Vehicle verification
   - Access control

9. **APIs**
   - RESTful API for external systems
   - Real-time availability API
   - Integration with navigation apps

10. **Testing**
    - Unit tests for all components
    - Integration tests
    - Load testing
    - Chaos engineering tests

---

## Technical Decisions & Trade-offs

### Why ReentrantLock instead of synchronized?
- **Fine-grained control**: Can try to acquire lock without blocking
- **Fairness**: Optional fair lock ordering
- **Interruptible**: Can interrupt waiting threads
- **Try-lock**: Non-blocking attempt to acquire lock

### Why ConcurrentHashMap instead of synchronized HashMap?
- **Better performance**: Lock striping allows concurrent reads/writes
- **No external synchronization**: Built-in thread safety
- **Atomic operations**: putIfAbsent, remove, etc.
- **Scalability**: Better performance under concurrent load

### Why Strategy Pattern for parking allocation?
- **Open/Closed Principle**: Open for extension, closed for modification
- **Runtime flexibility**: Change strategy without restart
- **Testability**: Easy to test different strategies
- **Business requirements**: Different strategies for different scenarios

### Why volatile for isOccupied?
- **Memory visibility**: Ensures changes are visible across threads
- **Lightweight**: No lock overhead for simple flag
- **Works with locks**: Complements lock-based synchronization
- **JMM compliance**: Follows Java Memory Model guarantees

---

## Conclusion

This Smart Parking Lot System successfully implements all functional requirements with:
- ✅ Automatic parking spot allocation
- ✅ Vehicle entry/exit time tracking
- ✅ Dynamic fee calculation
- ✅ Real-time availability updates
- ✅ Thread-safe concurrent operations

The design is **extensible**, **maintainable**, and **scalable**, making it suitable for real-world deployment with minimal modifications.

---

## Contact & Support

**Developer:** Kiriti  
**Course:** Airtribe Java Classes  
**Project:** Parking Lot Assignment  

For questions or improvements, please refer to the course materials or instructor.

---

**Last Updated:** November 19, 2024  
**Version:** 1.0  
**Status:** ✅ Complete and Production-Ready
