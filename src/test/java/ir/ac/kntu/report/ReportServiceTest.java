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

    @AfterEach
    void clearUser() {
        Persona.setCurrentUser(null);
    }

    @Test
    void authorizationByRole() {
        Persona.setCurrentUser(null);
        assertFalse(ReportService.isAuthorized());
        Persona.setCurrentUser(PersonaService.getProfileByUsername("admin"));
        assertTrue(ReportService.isAuthorized());
        Persona.setCurrentUser(PersonaService.getProfileByUsername("callcenter"));
        assertTrue(ReportService.isAuthorized());
        String email = "rep_" + System.nanoTime() + "@test.com";
        PersonaService.registerPersona(email, "Passw0rd!");
        Persona.setCurrentUser(PersonaService.getProfile(email));
        assertFalse(ReportService.isAuthorized());
    }

    @Test
    void financialsCoverAllSuppliers() {
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
        String html = ReportService.buildHtml(ReportService.computeSupplierFinancials());
        assertTrue(html.contains("Supplier Financial Status Report"));
        assertTrue(html.contains("Global Books Inc."));
        assertTrue(html.contains("<svg"));
    }

    @Test
    void exportRequiresAuthorization() {
        Persona.setCurrentUser(null);
        assertThrows(IllegalStateException.class, () -> ReportService.exportReport("build/should_not_exist.html"));
    }

    @Test
    void exportWritesFileWhenAuthorized() {
        Persona.setCurrentUser(PersonaService.getProfileByUsername("admin"));
        String path = ReportService.exportReport("build/test_financial_report.html");
        assertTrue(new File(path).exists());
    }
}
