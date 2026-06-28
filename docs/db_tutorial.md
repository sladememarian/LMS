# pgAdmin Query Reference

Useful SQL queries to run in pgAdmin's Query Tool (`Tools → Query Tool`) against the `lms` database.

---

## Quick overview

```sql
-- Count rows in every table at once
SELECT 'personas'       AS tbl, COUNT(*) FROM personas
UNION ALL SELECT 'library_items',   COUNT(*) FROM library_items
UNION ALL SELECT 'loans',           COUNT(*) FROM loans
UNION ALL SELECT 'transactions',    COUNT(*) FROM transactions
UNION ALL SELECT 'mail_messages',   COUNT(*) FROM mail_messages
UNION ALL SELECT 'support_tickets', COUNT(*) FROM support_tickets
UNION ALL SELECT 'role_requests',   COUNT(*) FROM role_requests
UNION ALL SELECT 'borrowed_items',  COUNT(*) FROM borrowed_items
ORDER BY tbl;
```

---

## Personas

```sql
-- All users with their role and wallet
SELECT email, role, wallet_balance, member_id, first_name, last_name
FROM personas
ORDER BY role, email;

-- Staff accounts only (admin + callcenter)
SELECT email, role, wallet_balance
FROM personas
WHERE role IN ('ADMIN', 'CALLCENTER');

-- Members by role with borrow count
SELECT p.email, p.role, COUNT(b.item_id) AS borrowed
FROM personas p
LEFT JOIN borrowed_items b ON b.email = p.email
WHERE p.role IN ('GUEST', 'STUDENT', 'TEACHER')
GROUP BY p.email, p.role
ORDER BY borrowed DESC;

-- Richest members (top 10 by wallet)
SELECT email, role, wallet_balance
FROM personas
ORDER BY wallet_balance DESC
LIMIT 10;
```

---

## Library

```sql
-- All items with availability
SELECT item_id, type, title, category, available_copies, total_copies, unit_price
FROM library_items
ORDER BY type, title;

-- Items currently fully borrowed out (nothing available)
SELECT item_id, type, title, total_copies
FROM library_items
WHERE available_copies = 0 AND total_copies > 0;

-- Item availability summary by type
SELECT type,
       COUNT(*)               AS total_items,
       SUM(total_copies)      AS total_copies,
       SUM(available_copies)  AS available_copies
FROM library_items
GROUP BY type;

-- Who has borrowed a specific item (replace the id)
SELECT p.email, p.role
FROM borrowed_items b
JOIN personas p ON p.email = b.email
WHERE b.item_id = 'ITEM-001';
```

---

## Loans & overdue fines

```sql
-- All active loans with days borrowed and due
SELECT l.member_id, l.item_id, li.title,
       l.borrow_day, l.due_day,
       (l.due_day - l.borrow_day) AS loan_length_days,
       l.last_charged_day
FROM loans l
LEFT JOIN library_items li ON li.item_id = l.item_id
ORDER BY l.due_day;

-- Overdue loans (current simulated day from clock_state)
SELECT l.member_id, l.item_id, li.title,
       l.due_day, c.current_day,
       (c.current_day - l.due_day) AS days_overdue
FROM loans l
JOIN clock_state c ON c.id = 1
LEFT JOIN library_items li ON li.item_id = l.item_id
WHERE c.current_day > l.due_day
ORDER BY days_overdue DESC;

-- Current simulated date
SELECT current_day, start_date FROM clock_state WHERE id = 1;
```

---

## Finance & transactions

```sql
-- All transactions newest first
SELECT tx_id, member_id, amount, type, description,
       to_timestamp(timestamp / 1000) AS tx_time
FROM transactions
ORDER BY timestamp DESC;

-- Total charged vs total topped-up per member
SELECT member_id,
       SUM(CASE WHEN amount > 0 THEN amount ELSE 0 END) AS total_credited,
       SUM(CASE WHEN amount < 0 THEN ABS(amount) ELSE 0 END) AS total_debited
FROM transactions
GROUP BY member_id
ORDER BY total_debited DESC;

-- Overdue fine transactions only
SELECT tx_id, member_id, ABS(amount) AS fine_amount, description,
       to_timestamp(timestamp / 1000) AS tx_time
FROM transactions
WHERE type = 'FINE'
ORDER BY timestamp DESC;
```

---

## Mail

```sql
-- Unread messages per user
SELECT recipient_email, COUNT(*) AS unread
FROM mail_messages
WHERE is_read = FALSE
GROUP BY recipient_email
ORDER BY unread DESC;

-- All messages for a specific user (replace email)
SELECT message_id, type, subject, sent_date, is_read
FROM mail_messages
WHERE recipient_email = 'user@example.com'
ORDER BY sent_date DESC;

-- 2FA codes still active (not yet consumed)
SELECT email, code,
       to_timestamp(issued_at / 1000) AS issued_at
FROM two_factor_codes
ORDER BY issued_at DESC;
```

---

## Support

```sql
-- All tickets by status
SELECT status, COUNT(*) FROM support_tickets GROUP BY status ORDER BY status;

-- Open tickets with priority
SELECT ticket_id, user_id, title, category, priority, status
FROM support_tickets
WHERE status = 'OPEN'
ORDER BY priority DESC, ticket_id;

-- Pending role-upgrade requests
SELECT request_id, requester_email, requested_role, message
FROM role_requests
WHERE status = 'PENDING'
ORDER BY request_id;

-- Full role request history
SELECT request_id, requester_email, requested_role, status
FROM role_requests
ORDER BY status, request_id;
```

---

## Housekeeping

```sql
-- Wipe everything and start fresh (WARNING: destroys all data)
DELETE FROM borrowed_items;
DELETE FROM loans;
DELETE FROM transactions;
DELETE FROM mail_messages;
DELETE FROM two_factor_codes;
DELETE FROM role_requests;
DELETE FROM support_tickets;
DELETE FROM library_items;
DELETE FROM suppliers;
DELETE FROM clock_state;
DELETE FROM personas;

-- Reset the simulated clock to day 1 today
UPDATE clock_state SET current_day = 1, start_date = CURRENT_DATE WHERE id = 1;
```
