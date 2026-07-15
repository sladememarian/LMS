# CallCenter Ticket Assignment (Support Sections)

## In plain terms
A support ticket has a **section** — the topic it's about (technical, book
request, finance, or reservation). CallCenter agents don't see every ticket;
each agent is **assigned certain sections** by an admin, and only sees tickets in
those sections. This doc explains how a ticket gets its section, how an admin
assigns sections to an agent, and how the inbox filters what each agent sees.

For the broader Support microservice overview see `docs/support.md`; this doc
zooms in on the section/assignment mechanism.

---

## The four sections
`SupportSection` is an enum with exactly four values:

```
BOOK_REQUEST   TECHNICAL   FINANCE   RESERVATION
```

Every ticket belongs to exactly one of these.

---

## Step 1 — a ticket gets its section when the member creates it
In `SupportMemberConsole`, the menu option the member picks decides the section.
`createTicket` then converts that choice into the enum and builds the ticket:

```java
SupportService.createTicket(user.getMemberId(), section, title, description);
```

Two things worth noting:
- The ticket's `userId` is the creator's **member id** (e.g. `STU-123456`), not
  their email. This matters later for notifying them back.
- The **priority** is auto-assigned by `resolvePriority(section, title)`:
  - `TECHNICAL` section → `HIGH`
  - otherwise, if the title contains `URGENT`, `CRASH`, or `BLOCK` → `CRITICAL`
  - otherwise → `LOW`

  Tickets sort by priority (critical > high > medium > low) via
  `SupportTicket.compareTo`, so the most urgent surface at the top.

---

## Step 2 — an admin assigns sections to a CallCenter agent
Where the assignment is **stored**: on the agent's `Persona`, not on the
transient profile object.

```java
private Set<SupportSection> assignedSupportSections = EnumSet.noneOf(SupportSection.class);
```

It starts empty — a freshly created agent (including the seeded
`callcenter@system.local`) sees nothing until an admin assigns sections.

The service that assigns them:

```java
public static void assignSupportSections(Persona actor, String agentEmail, Set<SupportSection> sections) {
    if (actor.getRole() != UserRole.ADMIN) {
        throw new AuthorizationException("Only Admins can assign support sections.");
    }
    Persona agent = PersonaService.getProfile(agentEmail);
    if (agent == null || agent.getRole() != UserRole.CALLCENTER) {
        throw new UserNotFoundException("CallCenter agent not found: " + agentEmail);
    }
    agent.setAssignedSupportSections(sections);   // overwrites the whole set
    PersonaRepository.insertPersona(agent);        // persists
}
```

Rules in plain terms:
- Only an **admin** may assign sections.
- The target must be a **CallCenter** persona (else "agent not found").
- The call **replaces** the agent's whole section set (it's not additive) and
  saves it to the database immediately.

**In the UI:** `AdminInbox` option **12 "Assign CallCenter Support Sections"**
prompts for the agent's email, prints the four section names, and reads a
comma-separated list (e.g. `TECHNICAL, FINANCE`). Each token is parsed with
`SupportSection.valueOf(token.toUpperCase())`; an unknown token aborts the whole
assignment with an error.

> Why store on the Persona and not the profile? `UserProfile.forRole()` builds a
> brand-new `CallCenterProfile` on every call, so anything stored on the profile
> would vanish instantly. The `Persona` is the durable holder;
> `CallCenterProfile.addSection/removeSection` just read/write through to it.

---

## Step 3 — the inbox shows each agent only their sections
`SupportService.getTicketsForAgent(agent)` is the filter:

```java
public static List<SupportTicket> getTicketsForAgent(Persona agent) {
    if (agent.getRole() != UserRole.CALLCENTER) {
        return getAllTickets();                     // admins see everything
    }
    Set<SupportSection> allowed = agent.getAssignedSupportSections();
    return TICKETS.stream()
            .filter(t -> allowed.contains(t.getSection()))
            .collect(Collectors.toList());
}
```

- A **non-CallCenter** caller (e.g. an admin) gets **all** tickets.
- A **CallCenter** agent gets only tickets whose section is in their assigned
  set. If their set is empty, they see nothing.

**In the UI:** `CallCenterInbox` builds its menu **dynamically** — one entry per
assigned section (alphabetical), labeled "`<Section>` Tickets", followed by fixed
actions:

| Menu entry | Action |
|------------|--------|
| `<Section>` Tickets (one per assigned section) | List that section's tickets (`getTicketsForAgent` then narrow to the chosen section) |
| Respond To Ticket | `respondToTicket` → sets status `IN_PROGRESS`, notifies the creator |
| Close Ticket | `updateTicketStatus(id, "CLOSED")` |
| Add Library Item | `addLibraryItemViaSupport` (allowed for CallCenter or Admin) |
| View Notifications | show the agent's system notifications |

An agent with no assigned sections gets a menu with **no** section entries — just
the fixed actions.

---

## Ticket lifecycle (statuses)
| Status | Set when |
|--------|----------|
| `OPEN` | created (`createTicket`) |
| `IN_PROGRESS` | an agent responds (`respondToTicket`) |
| `CLOSED` | an agent closes it (Close Ticket) |
| `RESOLVED` | defined but not set by any current code path |

CallCenter agents can only **respond** (→ IN_PROGRESS) and **close** (→ CLOSED)
through their inbox — they cannot delete tickets or set arbitrary statuses.

---

## Notifying the member back
When an agent responds, the creator is notified. Because the ticket stored the
creator's **member id**, `notifyCreator` looks them up by that id:

```java
Persona creator = PersonaService.getProfileByMemberId(ticket.getUserId());
// subject: "Reply to ticket " + ticketId
// if found -> NotificationService.notify(creator, subject, message)
// else     -> NotificationService.notifyAddress(ticket.getUserId(), ...)
```

The notification lands in the member's mailbox (a `SYSTEM_NOTIFICATION` mail),
which they read via "View Notifications." Note: only **responding** notifies the
creator — closing a ticket does not.
