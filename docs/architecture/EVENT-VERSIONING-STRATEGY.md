# Event Versioning Strategy - Agrifood Platform

## 🎯 Nega Event Versioning Kerak?

### Real-World Scenario
```
2025: ProcurementCreatedEvent v1 - faqat basic fields
2026: Yangi qonun - "organic certification" field majburiy
2027: Yangi feature - "multi-currency support"
```

**Muammo**: Eski event consumer'lar yangi field'larni tushunmaydi!

---

## 📋 Versioning Strategy

### 1️⃣ Schema Evolution Types

#### A. Backward Compatible (Safe)
✅ Yangi optional field qo'shish
✅ Yangi event type qo'shish
✅ Field description o'zgartirish

#### B. Breaking Changes (Dangerous)
❌ Mavjud field o'chirish
❌ Field type o'zgartirish (String → Integer)
❌ Required field qo'shish

---

## 🔄 Implementation Patterns

### Pattern 1: Version Field (Current)
```java
public class ProcurementCreatedEvent {
    private final String version = "v1";  // ✅ Already implemented
    
    // v1 fields
    private Long procurementId;
    private String procurementNumber;
    private BigDecimal budget;
    // ...
}
```

### Pattern 2: Event Upcasting (Recommended)
```java
/**
 * Event Upcaster - converts old events to new format
 * Ensures backward compatibility
 */
public class ProcurementEventUpcaster {
    
    public ProcurementCreatedEvent upcast(String eventJson) {
        JsonNode node = objectMapper.readTree(eventJson);
        String version = node.get("version").asText("v1");
        
        switch (version) {
            case "v1":
                return upcastV1ToV2(node);
            case "v2":
                return parseV2(node);
            default:
                throw new UnsupportedEventVersionException(version);
        }
    }
    
    private ProcurementCreatedEvent upcastV1ToV2(JsonNode v1Event) {
        // v1 → v2: add default values for new fields
        ProcurementCreatedEvent v2 = parseV1(v1Event);
        
        // New fields in v2 (with safe defaults)
        v2.setOrganicCertified(false);  // default: not certified
        v2.setCurrency("UZS");          // default: UZS
        
        return v2;
    }
}
```

### Pattern 3: Multiple Event Classes
```java
// v1 - original
public class ProcurementCreatedEventV1 {
    private final String version = "v1";
    private Long procurementId;
    private BigDecimal budget;
}

// v2 - extended
public class ProcurementCreatedEventV2 extends ProcurementCreatedEventV1 {
    private final String version = "v2";
    private Boolean organicCertified;
    private String currency;
}

// Consumer handles both
@EventListener
public void handle(ProcurementCreatedEventV1 event) {
    // Works for both v1 and v2 (polymorphism)
}
```

---

## 🛠️ Practical Implementation

### Step 1: Event Base Class
```java
/**
 * Base class for all domain events
 * Provides versioning and metadata
 */
public abstract class DomainEvent {
    private String eventId = UUID.randomUUID().toString();
    private String eventType;
    private String version;
    private LocalDateTime timestamp = LocalDateTime.now();
    
    // Getters
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getVersion() { return version; }
    public LocalDateTime getTimestamp() { return timestamp; }
    
    protected DomainEvent(String eventType, String version) {
        this.eventType = eventType;
        this.version = version;
    }
}
```

### Step 2: Versioned Event
```java
/**
 * ProcurementCreatedEvent v2
 * 
 * CHANGELOG:
 * v1 (2025-01): Initial version
 * v2 (2026-03): Added organicCertified, currency fields
 * 
 * BACKWARD COMPATIBILITY:
 * - v1 consumers: ignore new fields (forward compatible)
 * - v2 consumers: handle v1 events via upcasting
 */
public class ProcurementCreatedEvent extends DomainEvent {
    
    // v1 fields (always present)
    private Long procurementId;
    private String procurementNumber;
    private Long buyerId;
    private String productCategory;
    private BigDecimal quantity;
    private BigDecimal budget;
    private LocalDateTime deadline;
    
    // v2 fields (optional for backward compatibility)
    private Boolean organicCertified;  // null = unknown (v1 events)
    private String currency;           // null = default UZS (v1 events)
    
    public ProcurementCreatedEvent() {
        super("ProcurementCreated", "v2");
    }
    
    // Getters/Setters...
}
```

### Step 3: Event Consumer with Version Handling
```java
/**
 * Event consumer that handles multiple versions
 */
@Component
public class ProcurementEventConsumer {
    
    @EventListener
    public void handleProcurementCreated(ProcurementCreatedEvent event) {
        String version = event.getVersion();
        
        log.info("Processing event version: {}", version);
        
        // Handle v1 events (backward compatibility)
        if ("v1".equals(version)) {
            handleV1Event(event);
        } 
        // Handle v2 events (current)
        else if ("v2".equals(version)) {
            handleV2Event(event);
        }
        else {
            log.warn("Unknown event version: {}", version);
            // Fallback to v1 handling
            handleV1Event(event);
        }
    }
    
    private void handleV1Event(ProcurementCreatedEvent event) {
        // Process only v1 fields
        Long procurementId = event.getProcurementId();
        BigDecimal budget = event.getBudget();
        
        // Use defaults for missing v2 fields
        boolean organic = false;  // default
        String currency = "UZS";  // default
        
        processEvent(procurementId, budget, organic, currency);
    }
    
    private void handleV2Event(ProcurementCreatedEvent event) {
        // Process all fields including v2
        Long procurementId = event.getProcurementId();
        BigDecimal budget = event.getBudget();
        Boolean organic = event.getOrganicCertified() != null 
            ? event.getOrganicCertified() 
            : false;
        String currency = event.getCurrency() != null 
            ? event.getCurrency() 
            : "UZS";
        
        processEvent(procurementId, budget, organic, currency);
    }
    
    private void processEvent(Long id, BigDecimal budget, 
                             boolean organic, String currency) {
        // Actual business logic
    }
}
```

---

## 📊 Version Migration Flow

```
┌─────────────────────────────────────────────────────┐
│  Event Store (Kafka/RabbitMQ)                       │
├─────────────────────────────────────────────────────┤
│  v1 events (old) ──┐                                │
│  v2 events (new) ──┼──▶ Consumer                    │
└────────────────────┴─────────────────────────────────┘
                     │
                     ▼
            ┌────────────────┐
            │  Upcaster      │
            │  v1 → v2       │
            └────────────────┘
                     │
                     ▼
            ┌────────────────┐
            │  Business      │
            │  Logic         │
            └────────────────┘
```

---

## 🎯 Best Practices

### 1. Always Add Version Field
```java
private final String version = "v1";  // ✅ Good
```

### 2. Document Changes
```java
/**
 * CHANGELOG:
 * v1 (2025-01): Initial version
 * v2 (2026-03): Added organicCertified field
 * v3 (2027-06): Added multi-currency support
 */
```

### 3. Use Optional Fields for New Features
```java
// ✅ Good - nullable, backward compatible
private Boolean organicCertified;

// ❌ Bad - breaks old consumers
private boolean organicCertified;  // primitive, can't be null
```

### 4. Provide Default Values
```java
public Boolean getOrganicCertified() {
    return organicCertified != null ? organicCertified : false;
}
```

### 5. Test Backward Compatibility
```java
@Test
public void testV1EventProcessing() {
    // Create v1 event (without new fields)
    ProcurementCreatedEvent v1Event = new ProcurementCreatedEvent();
    v1Event.setVersion("v1");
    v1Event.setProcurementId(1L);
    // organicCertified = null (not set)
    
    // Should process without errors
    consumer.handleProcurementCreated(v1Event);
}
```

---

## 🚨 Migration Strategy

### Phase 1: Dual Write (Transition Period)
```java
// Publish both v1 and v2 events
eventPublisher.publish(createV1Event(procurement));
eventPublisher.publish(createV2Event(procurement));
```

### Phase 2: Monitor Consumers
```
- Track which consumers still use v1
- Notify teams to upgrade
- Set deprecation timeline
```

### Phase 3: Deprecate Old Version
```java
@Deprecated(since = "2026-06", forRemoval = true)
public class ProcurementCreatedEventV1 {
    // Will be removed in 2027-01
}
```

---

## 📈 Monitoring

### Metrics to Track
```java
// Event version distribution
eventVersionCounter.increment("v1");  // decreasing
eventVersionCounter.increment("v2");  // increasing

// Consumer compatibility
consumerVersionGauge.set("supplier-service", "v2");
consumerVersionGauge.set("contract-service", "v1");  // needs upgrade!
```

---

## 🎓 Summary

| Strategy | Pros | Cons | Use When |
|----------|------|------|----------|
| Version Field | Simple, lightweight | Manual handling | Small changes |
| Upcasting | Automatic conversion | Complex logic | Major changes |
| Multiple Classes | Type safety | Code duplication | Breaking changes |

**Recommendation**: Start with Version Field + Upcasting (hybrid approach)
