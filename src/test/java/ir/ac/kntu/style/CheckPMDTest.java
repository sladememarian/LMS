/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.kntu.style;

import net.sourceforge.pmd.*;
import net.sourceforge.pmd.lang.LanguageRegistry;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author m.lotfi
 */
public class CheckPMDTest {

    @Test
    public void testPMD() {

        /*
         * Configuration
         */
        PMDConfiguration config = new PMDConfiguration();

        config.setDefaultLanguageVersion(LanguageRegistry.findLanguageByTerseName("java").getVersion("17"));
        config.addInputPath(Path.of("src/main/"));
        config.setMinimumPriority(RulePriority.LOW);
        config.addRuleSet("ir/ac/kntu/style/ruleset.xml");
        config.setReportFormat("text");
        config.setIgnoreIncrementalAnalysis(true);

        /*
         * Analysis
         */
        try (PmdAnalysis pmd = PmdAnalysis.create(config)) {
            Report report = pmd.performAnalysisAndCollectReport();

            List<RuleViolation> violations = report.getViolations();

            for (RuleViolation v : violations) {
                System.out.println("VIOLATION: " + v.getRule().getName() + " - " + v.getDescription());
            }

            System.out.println("Found " + violations.size() + " PMD rule violations.");
            assertEquals(0, violations.size(), violations.size() + " PMD rule violations found.");
        }
    }
}
