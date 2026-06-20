package ir.ac.kntu.finance;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.util.EnvConfig;

public class LoanService {
    // tracking who borrowed what since forever
    private static final List<Loan> LOANS = new ArrayList<>();
    private static final String FILE_PATH = "loans_secure.json";
    private static final int LOAN_PERIOD_DAYS = 3;
    private static final int OVERDUE_DAILY_FINE = 10_000;
    private static final String KEY_MEMBER = "mid";
    private static final String KEY_ITEM = "iid";
    private static final String KEY_BORROW_DAY = "bd";
    private static final String KEY_DUE_DAY = "due";
    private static final String KEY_LAST_CHARGED = "lc";

    private static byte[] getEncryptionKey() {
        return EnvConfig.get("MASTER_ADMIN_DATABASE_PASSWORD", "fallbackKey").getBytes();
    }

    public static void recordLoan(String memberId, String itemId, int currentDay) {
        load();
        int dueDay = currentDay + LOAN_PERIOD_DAYS;
        LOANS.add(new Loan(memberId, itemId, currentDay, dueDay));
        save();
    }

    public static boolean clearLoan(String memberId, String itemId) {
        load();
        boolean removed = LOANS.removeIf(loan -> loan.getMemberId().equals(memberId)
                && loan.getItemId().equals(itemId));
        if (removed) {
            save();
        }
        return removed;
    }

    public static List<Loan> getLoans() {
        load();
        return new ArrayList<>(LOANS);
    }

    private static String chargeOverdue(Loan loan, int currentDay) {
        boolean overdue = currentDay > loan.getDueDay() && loan.getLastChargedDay() < currentDay;
        if (!overdue) {
            return null;
        }
        Persona borrower = PersonaService.getProfileByMemberId(loan.getMemberId());
        loan.setLastChargedDay(currentDay);
        if (borrower == null) {
            return null;
        }
        FinanceService.recordDebt(borrower, OVERDUE_DAILY_FINE,
                "Overdue fine for " + loan.getItemId() + " (day " + currentDay + ")");
        return loan.getMemberId() + " +" + OVERDUE_DAILY_FINE + " (" + loan.getItemId() + ")";
    }

    public static List<String> accrueOverdueDebts(int currentDay) {
        load();
        List<String> charges = new ArrayList<>();
        for (Loan loan : LOANS) {
            String summary = chargeOverdue(loan, currentDay);
            if (summary != null) {
                charges.add(summary);
            }
        }
        save();
        return charges;
    }

    private static void appendLoan(StringBuilder builder, Loan loan) {
        String suffix = "\",\n";
        builder.append("  {\n")
                .append("    \"mid\": \"").append(loan.getMemberId()).append(suffix)
                .append("    \"iid\": \"").append(loan.getItemId()).append(suffix)
                .append("    \"bd\": \"").append(loan.getBorrowDay()).append(suffix)
                .append("    \"due\": \"").append(loan.getDueDay()).append(suffix)
                .append("    \"lc\": \"").append(loan.getLastChargedDay()).append("\"\n")
                .append("  }");
    }

    private static void save() {
        StringBuilder builder = new StringBuilder("[\n");
        for (int i = 0; i < LOANS.size(); i++) {
            appendLoan(builder, LOANS.get(i));
            if (i < LOANS.size() - 1) {
                builder.append(",");
            }
            builder.append("\n");
        }
        builder.append("]");
        byte[] keyBytes = getEncryptionKey();
        byte[] rawBytes = builder.toString().getBytes();
        byte[] enc = new byte[rawBytes.length];
        for (int i = 0; i < rawBytes.length; i++) {
            enc[i] = (byte) (rawBytes[i] ^ keyBytes[i % keyBytes.length]);
        }
        try (FileOutputStream fos = new FileOutputStream(FILE_PATH)) {
            fos.write(enc);
        } catch (IOException ex) {
            System.err.println("Error saving loans: " + ex.getMessage());
        }
    }

    private static void load() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] enc = fis.readAllBytes();
            byte[] keyBytes = getEncryptionKey();
            byte[] dec = new byte[enc.length];
            for (int i = 0; i < enc.length; i++) {
                dec[i] = (byte) (enc[i] ^ keyBytes[i % keyBytes.length]);
            }
            parseJson(new String(dec));
        } catch (IOException ex) {
            System.err.println("Error loading loans: " + ex.getMessage());
        }
    }

    private static void parseJson(String raw) {
        LOANS.clear();
        String clean = raw.replace("[", "").replace("]", "").trim();
        if (clean.isEmpty()) {
            return;
        }
        for (String block : clean.split("\\},")) {
            String obj = block.replace("{", "").replace("}", "").trim();
            String memberId = extract(obj, KEY_MEMBER);
            String itemId = extract(obj, KEY_ITEM);
            if (memberId != null && itemId != null) {
                Loan loan = new Loan(memberId, itemId,
                        parseInt(extract(obj, KEY_BORROW_DAY)), parseInt(extract(obj, KEY_DUE_DAY)));
                loan.setLastChargedDay(parseInt(extract(obj, KEY_LAST_CHARGED)));
                LOANS.add(loan);
            }
        }
    }

    private static int parseInt(String value) {
        return value == null || value.isEmpty() ? 0 : Integer.parseInt(value);
    }

    private static String extract(String src, String key) {
        String token = "\"" + key + "\": \"";
        int start = src.indexOf(token);
        if (start == -1) {
            return null;
        }
        start += token.length();
        return src.substring(start, src.indexOf("\"", start));
    }
}