# Step 1 — Existing OOP Class Hierarchy (Library Items)

## What This Step Covers

This document describes the **existing** inheritance hierarchy for library items — the **before** state that Step 4 refactors into full polymorphism.

---

## Class Hierarchy

```
LibraryItem  (abstract)
├── PhysicalItem  (abstract, extends LibraryItem)
│   ├── Book          getItemType() → "BOOK"
│   └── Magazine      getItemType() → "MAGAZINE"
└── DigitalItem   (abstract, extends LibraryItem)
    ├── EBook         getItemType() → "EBOOK"
    └── AudioBook     getItemType() → "AUDIOBOOK"
```

### `LibraryItem` (abstract base)

`src/main/java/ir/ac/kntu/library/LibraryItem.java`

Stores properties common to **every** catalog entry:

| Field | Type | Purpose |
|-------|------|---------|
| `itemId` | String | Unique identifier (e.g. `ITEM-001`) |
| `title` | String | Display title |
| `category` | String | Genre / subject area |
| `publishYear` | int | Year of publication |
| `supplierId` | String | Which supplier owns this item |
| `totalCopies` | int | Total copies in the library |
| `availableCopies` | int | How many can be borrowed right now |
| `unitPrice` | int | Price per copy (used in financial reports) |

**Abstract method**: `getItemType()` — every concrete class must override this to return its type label string.

**Computed getter**: `getBorrowedCopies()` = `totalCopies - availableCopies`

The constructor is:
```java
public LibraryItem(String id, String title, String cat, int year)
```
The four-arg version. Supplier, copies, and price are set by setters after construction.

---

### `PhysicalItem` (abstract, extends `LibraryItem`)

`src/main/java/ir/ac/kntu/library/PhysicalItem.java`

Adds two physical-world properties:

| Field | Type | Purpose |
|-------|------|---------|
| `shelfLocation` | String | Where to find it in the building (e.g. `Shelf A-1`) |
| `physicalCondition` | String | Condition label (`GOOD`, `NEW`, etc.) |

Overrides `getItemType()` → `"PHYSICAL"` (but this is overridden again in each leaf).

---

### `DigitalItem` (abstract, extends `LibraryItem`)

`src/main/java/ir/ac/kntu/library/DigitalItem.java`

Adds two digital-world properties:

| Field | Type | Purpose |
|-------|------|---------|
| `downloadUrl` | String | Must start with `https://` (validated by `Validator`) |
| `fileSize` | long | File size in bytes |

Overrides `getItemType()` → `"DIGITAL"` (overridden again in each leaf).

---

### `Book` (extends `PhysicalItem`)

`src/main/java/ir/ac/kntu/library/Book.java`

| Extra field | Type | Purpose |
|-------------|------|---------|
| `author` | String | Author name |
| `isbn` | String | ISBN-13 (e.g. `978-0132350884`) |

`getItemType()` → `"BOOK"`

---

### `Magazine` (extends `PhysicalItem`)

`src/main/java/ir/ac/kntu/library/Magazine.java`

| Extra field | Type | Purpose |
|-------------|------|---------|
| `issueNumber` | int | Which issue (e.g. `42`) |

`getItemType()` → `"MAGAZINE"`

---

### `EBook` (extends `DigitalItem`)

`src/main/java/ir/ac/kntu/library/EBook.java`

| Extra field | Type | Purpose |
|-------------|------|---------|
| `pageCount` | int | Number of pages |

`getItemType()` → `"EBOOK"`

---

### `AudioBook` (extends `DigitalItem`)

`src/main/java/ir/ac/kntu/library/AudioBook.java`

| Extra field | Type | Purpose |
|-------------|------|---------|
| `narrator` | String | Who reads the book |
| `durationMinutes` | int | Total listening time |

`getItemType()` → `"AUDIOBOOK"`

---

## What OOP Concepts Are Already Present

| Concept | Where |
|---------|-------|
| **Abstraction** | `LibraryItem` and `PhysicalItem`/`DigitalItem` are abstract — you cannot instantiate them directly |
| **Inheritance** | Book and Magazine inherit all fields from LibraryItem via PhysicalItem |
| **Method overriding** | Every leaf class overrides `getItemType()` |
| **Constructor chaining** | `Book(id, title, cat, year)` calls `super(id, title, cat, year)` which calls `LibraryItem(...)` |

---

## What Is Still Missing (Fixed in Step 4)

The hierarchy only answers **"what type is this?"** via `getItemType()`. It does NOT answer behavioural questions polymorphically:

| Question | Current approach | Problem |
|----------|-----------------|---------|
| Who wrote it? | `instanceof Book` check in `LibraryPrinter` | Violates Open/Closed Principle |
| Can it be reserved? | No method — every caller guesses | Inconsistent |
| How long is the borrow period? | Hardcoded constant everywhere | Not item-specific |
| What actions does the user have? | No method — callers check type string | Fragile |

Step 4 adds these as overridable methods directly on `LibraryItem`.

---

## Constructor Chain Example

```
new Book("ITEM-001", "Clean Code", "Programming", 2008)
    → Book(id, title, cat, yr)
        → PhysicalItem(id, title, cat, yr)   [super()]
            → LibraryItem(id, title, cat, yr) [super()]
                sets itemId, title, category, publishYear
```

After the constructor, setters are called:
```java
cleanCode.setSupplierId("SUP-101");
cleanCode.setTotalCopies(5);
cleanCode.setAvailableCopies(5);
cleanCode.setUnitPrice(250);
cleanCode.setShelfLocation("Shelf A-1");
cleanCode.setAuthor("Robert Martin");
```

---

## Seeded Data (11 Items across 4 Suppliers)

| ID | Title | Type | Supplier |
|----|-------|------|---------|
| ITEM-001 | Clean Code | BOOK | Global Books Inc. |
| ITEM-002 | Effective Java | EBOOK | Digital Reads Ltd. |
| ITEM-003 | Introduction to Algorithms | BOOK | KNTU Academic Press |
| ITEM-004 | Sapiens (Audio) | AUDIOBOOK | Digital Reads Ltd. |
| ITEM-005 | Nature Weekly | MAGAZINE | Magazine World |
| ITEM-006 | The Great Gatsby | BOOK | Global Books Inc. |
| ITEM-007 | Dune | EBOOK | Digital Reads Ltd. |
| ITEM-008 | The Hobbit (Audio) | AUDIOBOOK | Digital Reads Ltd. |
| ITEM-009 | Structure and Interpretation | BOOK | KNTU Academic Press |
| ITEM-010 | IEEE Spectrum | MAGAZINE | Magazine World |
| ITEM-011 | The Pragmatic Programmer | EBOOK | Digital Reads Ltd. |

See `docs/mock_data.md` for full details.
