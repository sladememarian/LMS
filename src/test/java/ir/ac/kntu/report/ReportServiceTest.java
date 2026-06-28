package ir.ac.kntu.report;

import ir.ac.kntu.library.LibraryService;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportServiceTest {
    // Reports: because guesswork is not a strategy

    @AfterEach
    void clearUser() {
        Persona.setCurrentUser(null);
    }

    @Test
    void authorizationByRole() {
        // Not authorized? No report. Authorized? Here's a PDF.
        Persona.setCurrentUser(null);
        assertFalse(ReportService.isAuthorized());
        PersonaService.validateCredentials("admin@system.local", "adminpass");
        Persona.setCurrentUser(PersonaService.getProfile("admin@system.local"));
        assertTrue(ReportService.isAuthorized());
        PersonaService.validateCredentials("callcenter@system.local", "ccpass");
        Persona.setCurrentUser(PersonaService.getProfile("callcenter@system.local"));
        assertTrue(ReportService.isAuthorized());
        String email = "rep_" + System.nanoTime() + "@test.com";
        PersonaService.registerPersona(email, "Passw0rd!");
        Persona.setCurrentUser(PersonaService.getProfile(email));
        assertFalse(ReportService.isAuthorized());
    }

    @Test
    void financialsCoverAllSuppliers() {
        // Every supplier gets a row. Even Acme.
        List<SupplierFinancials> data = ReportService.computeSupplierFinancials();
        assertEquals(LibraryService.getAllSuppliers().size(), data.size());
        long totalInventory = 0;
        int items = 0;
        for (SupplierFinancials row : data) {
            totalInventory += row.getInventoryValue();
            items += row.getItemCount();
        }
        assertTrue(totalInventory > 0);
        assertTrue(items > 0);
    }

    @Test
    void htmlContainsHeadingAndCompany() {
        // HTML: the crayon drawing of the programming world
        String html = ReportService.buildHtml(ReportService.computeSupplierFinancials());
        assertTrue(html.contains("Supplier Financial Status Report"));
        assertTrue(html.contains("Global Books Inc."));
        assertTrue(html.contains("<svg"));
    }

    @Test
    void exportRequiresAuthorization() {
        // Export without auth? IllegalStateException says no.
        Persona.setCurrentUser(null);
        assertThrows(IllegalStateException.class, () -> ReportService.exportReport("build/should_not_exist.html"));
    }

    @Test
    void exportWritesFileWhenAuthorized() {
        // With auth comes great file-writing power
        PersonaService.validateCredentials("admin@system.local", "adminpass");
        Persona.setCurrentUser(PersonaService.getProfile("admin@system.local"));
        String path = ReportService.exportReport("build/test_financial_report.html");
        assertTrue(new File(path).exists());
    }
}