package ir.ac.kntu.support;

import ir.ac.kntu.persona.AdminManagementService;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.persona.UserRole;
import ir.ac.kntu.util.DatabaseAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupportPermissionsTest {

    private Persona owner;

    @BeforeEach
    void setUp() {
        DatabaseAccess.clearPersonas();
        PersonaService.reset();
        DatabaseAccess.clearSupportTickets();
        owner = PersonaService.getProfile("admin@system.local");
    }

    private Persona registerCallCenterAgent(String email) {
        Persona agent = PersonaService.registerPersona(email, "pass");
        agent.updateRole(UserRole.CALLCENTER);
        DatabaseAccess.insertPersona(agent);
        return agent;
    }

    @Test
    void agentOnlySeesTicketsForItsAssignedSection() {
        Persona agent = registerCallCenterAgent("cc1@test.com");
        AdminManagementService.assignSupportSections(owner, "cc1@test.com",
                EnumSet.of(SupportSection.TECHNICAL));

        SupportService.createTicket("U1", SupportSection.TECHNICAL, "Tech issue", "desc");
        SupportService.createTicket("U2", SupportSection.FINANCE, "Money issue", "desc");

        List<SupportTicket> tickets = SupportService.getTicketsForAgent(agent);

        assertEquals(1, tickets.size());
        assertEquals(SupportSection.TECHNICAL, tickets.get(0).getSection());
    }

    @Test
    void agentWithMultipleSectionsSeesAllOfThem() {
        Persona agent = registerCallCenterAgent("cc2@test.com");
        AdminManagementService.assignSupportSections(owner, "cc2@test.com",
                EnumSet.of(SupportSection.TECHNICAL, SupportSection.FINANCE));

        SupportService.createTicket("U1", SupportSection.TECHNICAL, "Tech issue", "desc");
        SupportService.createTicket("U2", SupportSection.FINANCE, "Money issue", "desc");
        SupportService.createTicket("U3", SupportSection.RESERVATION, "Reservation issue", "desc");
        SupportService.createTicket("U4", SupportSection.BOOK_REQUEST, "Book request", "desc");

        List<SupportTicket> tickets = SupportService.getTicketsForAgent(agent);

        assertEquals(2, tickets.size());
        assertTrue(tickets.stream().allMatch(t ->
                t.getSection() == SupportSection.TECHNICAL || t.getSection() == SupportSection.FINANCE));
    }

    @Test
    void agentWithNoAssignedSectionsSeesNothing() {
        Persona agent = registerCallCenterAgent("cc3@test.com");

        SupportService.createTicket("U1", SupportSection.TECHNICAL, "Tech issue", "desc");
        SupportService.createTicket("U2", SupportSection.FINANCE, "Money issue", "desc");

        List<SupportTicket> tickets = SupportService.getTicketsForAgent(agent);

        assertEquals(0, tickets.size());
    }

    @Test
    void differentAgentsSeeOnlyTheirOwnSections() {
        Persona techAgent = registerCallCenterAgent("cc-tech@test.com");
        Persona financeAgent = registerCallCenterAgent("cc-finance@test.com");
        AdminManagementService.assignSupportSections(owner, "cc-tech@test.com",
                EnumSet.of(SupportSection.TECHNICAL));
        AdminManagementService.assignSupportSections(owner, "cc-finance@test.com",
                EnumSet.of(SupportSection.FINANCE));

        SupportService.createTicket("U1", SupportSection.TECHNICAL, "Tech issue", "desc");
        SupportService.createTicket("U2", SupportSection.FINANCE, "Money issue", "desc");

        List<SupportTicket> techTickets = SupportService.getTicketsForAgent(techAgent);
        List<SupportTicket> financeTickets = SupportService.getTicketsForAgent(financeAgent);

        assertEquals(1, techTickets.size());
        assertEquals(SupportSection.TECHNICAL, techTickets.get(0).getSection());
        assertEquals(1, financeTickets.size());
        assertEquals(SupportSection.FINANCE, financeTickets.get(0).getSection());
    }

    @Test
    void reassigningSectionsReplacesThePreviousAssignment() {
        Persona agent = registerCallCenterAgent("cc4@test.com");
        AdminManagementService.assignSupportSections(owner, "cc4@test.com",
                EnumSet.of(SupportSection.TECHNICAL));
        AdminManagementService.assignSupportSections(owner, "cc4@test.com",
                EnumSet.of(SupportSection.RESERVATION));

        SupportService.createTicket("U1", SupportSection.TECHNICAL, "Tech issue", "desc");
        SupportService.createTicket("U2", SupportSection.RESERVATION, "Reservation issue", "desc");

        List<SupportTicket> tickets = SupportService.getTicketsForAgent(agent);

        assertEquals(1, tickets.size());
        assertEquals(SupportSection.RESERVATION, tickets.get(0).getSection());
    }
}
