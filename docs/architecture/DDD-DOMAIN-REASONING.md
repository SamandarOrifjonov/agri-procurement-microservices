# DDD Domain Reasoning - Agrifood Platform

## Nega Alohida Bounded Context'lar?

### 🎯 Strategic Design Principles

Har bir service alohida bounded context, chunki:

---

## 1️⃣ PROCUREMENT SERVICE (Xarid Talabi)

### Domain Responsibility
**Xaridor tomonidan mahsulot talabini boshqarish**

### Nega Alohida?
- **Ubiquitous Language**: "Procurement", "Tender", "Bid", "Deadline"
- **Business Logic**: Xarid jarayoni qoidalari (budget validation, deadline management)
- **Lifecycle**: DRAFT → PUBLISHED → BIDDING → AWARDED → CLOSED
- **Autonomy**: Xarid talabini yaratish va boshqarish mustaqil jarayon

### Core Invariants
```java
// Procurement domain qoidalari:
// 1. Budget > 0 bo'lishi kerak
// 2. Deadline kelajakda bo'lishi kerak  
// 3. PUBLISHED holatda o'zgartirish cheklangan
// 4. Bir procurement faqat bitta contract'ga olib kelishi mumkin
```

### Why NOT merge with Contract?
- Procurement mavjud, lekin contract yo'q bo'lishi mumkin (bidding stage)
- Procurement bir nechta bid olishi mumkin, lekin faqat bitta contract
- Har birining alohida lifecycle va business rules bor

---

## 2️⃣ SUPPLIER SERVICE (Ta'minotchi)

### Domain Responsibility
**Qishloq xo'jaligi mahsulot yetkazib beruvchilarni boshqarish**

### Nega Alohida?
- **Ubiquitous Language**: "Supplier", "Rating", "Capacity", "Certification"
- **Business Logic**: Supplier verification, rating calculation, capacity management
- **Lifecycle**: PENDING → ACTIVE → SUSPENDED → BLACKLISTED
- **Autonomy**: Supplier ma'lumotlari va reytingi mustaqil boshqariladi

### Core Invariants
```java
// Supplier domain qoidalari:
// 1. Email unique bo'lishi kerak
// 2. Rating 0-5 oralig'ida
// 3. SUSPENDED supplier yangi contract ololmaydi
// 4. Rating faqat completed contracts orqali o'zgaradi
```

### Why NOT merge with Contract?
- Supplier contract'siz mavjud (registration stage)
- Bir supplier ko'plab contract'larda ishtirok etadi
- Supplier rating va capacity - alohida business concern
- Supplier verification jarayoni contract'dan mustaqil

---

## 3️⃣ CONTRACT SERVICE (Shartnoma)

### Domain Responsibility
**Xaridor va ta'minotchi o'rtasidagi shartnomani boshqarish**

### Nega Alohida?
- **Ubiquitous Language**: "Contract", "Agreement", "Delivery", "Payment"
- **Business Logic**: Contract terms, delivery tracking, payment milestones
- **Lifecycle**: DRAFT → SIGNED → IN_PROGRESS → COMPLETED → CANCELLED
- **Autonomy**: Shartnoma shartlari va bajarilishi mustaqil boshqariladi

### Core Invariants
```java
// Contract domain qoidalari:
// 1. Bir procurement faqat bitta active contract
// 2. Amount va quantity procurement bilan mos kelishi kerak
// 3. SIGNED contract'ni o'chirish mumkin emas
// 4. Delivery tracking faqat IN_PROGRESS holatda
```

### Why NOT merge with Procurement or Supplier?
- Contract ikki domain o'rtasidagi relationship (Procurement + Supplier)
- Contract o'z lifecycle va business rules'ga ega
- Contract delivery va payment tracking - alohida concern
- Saga pattern uchun alohida orchestration kerak

---

## 🔗 Domain Relationships

```
┌─────────────────┐         ┌──────────────────┐
│  PROCUREMENT    │────────▶│    CONTRACT      │
│  (Xarid Talabi) │  creates│   (Shartnoma)    │
└─────────────────┘         └──────────────────┘
                                      │
                                      │ involves
                                      ▼
                            ┌──────────────────┐
                            │    SUPPLIER      │
                            │  (Ta'minotchi)   │
                            └──────────────────┘
```

### Eventual Consistency
- **Procurement** → event → **Contract** (ProcurementCreatedEvent)
- **Contract** → event → **Supplier** (update rating, completed contracts)
- **Supplier** → event → **Procurement** (update bid count)

---

## 📊 DDD Patterns Applied

### 1. Bounded Context
Har bir service o'z domain model va ubiquitous language'ga ega

### 2. Aggregate Root
- Procurement aggregate: Procurement entity
- Supplier aggregate: Supplier entity  
- Contract aggregate: Contract entity + Saga state

### 3. Domain Events
- ProcurementCreatedEvent
- ContractSignedEvent (future)
- SupplierRatingUpdatedEvent (future)

### 4. Anti-Corruption Layer
Har bir service o'z API orqali boshqa service'lar bilan gaplashadi, to'g'ridan-to'g'ri database access yo'q

---

## 🎓 Business Justification

### Scalability
Har bir domain mustaqil scale qilinadi:
- Procurement service - ko'p read operations
- Contract service - ko'p write operations (saga)
- Supplier service - kam o'zgaradi, cache-friendly

### Team Autonomy
Har bir team o'z domain'ida ishlaydi:
- Procurement team - tender jarayonini yaxshilaydi
- Supplier team - verification va rating'ni yaxshilaydi
- Contract team - delivery tracking'ni yaxshilaydi

### Regulatory Compliance
Har bir domain o'z compliance requirements'ga ega:
- Procurement - tender qonunlari
- Contract - shartnoma qonunlari
- Supplier - sertifikatsiya talablari

---

## 🚀 Future Evolution

Har bir bounded context mustaqil evolve qiladi:
- Procurement: auction mechanism qo'shish
- Supplier: certification management
- Contract: payment milestone tracking
- Delivery: yangi bounded context (future)
