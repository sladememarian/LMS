package ir.ac.kntu.finance;

import java.util.ArrayList;
import java.util.List;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.util.DatabaseAccess;

public class LoanService {
    private static final List<Loan> LOANS = new ArrayList<>();
    private static final int LOAN_PERIOD_DAYS = 3;
    private static final int OVERDUE_DAILY_FINE = 10_000;

    static {
        LOANS.addAll(DatabaseAccess.getAllLoans());
    }

    public static void recordLoan(String memberId, String itemId, int currentDay) {
        LOANS.clear();
        LOANS.addAll(DatabaseAccess.getAllLoans());
        int dueDay = currentDay + LOAN_PERIOD_DAYS;
        LOANS.add(new Loan(memberId, itemId, currentDay, dueDay));
        DatabaseAccess.insertLoan(new Loan(memberId, itemId, currentDay, dueDay));
    }

    public static boolean clearLoan(String memberId, String itemId) {
        LOANS.clear();
        LOANS.addAll(DatabaseAccess.getAllLoans());
        boolean removed = LOANS.removeIf(loan -> loan.getMemberId().equals(memberId)
                && loan.getItemId().equals(itemId));
        if (removed) {
            DatabaseAccess.deleteLoan(memberId, itemId);
        }
        return removed;
    }

    public static boolean extendLoan(String memberId, String itemId, int extraDays) {
        LOANS.clear();
        LOANS.addAll(DatabaseAccess.getAllLoans());
        for (Loan loan : LOANS) {
            if (loan.getMemberId().equals(memberId) && loan.getItemId().equals(itemId)) {
                loan.setDueDay(loan.getDueDay() + extraDays);
                loan.setLastChargedDay(loan.getDueDay());
                DatabaseAccess.insertLoan(loan);
                return true;
            }
        }
        return false;
    }

    public static int getDueDay(String memberId, String itemId) {
        LOANS.clear();
        LOANS.addAll(DatabaseAccess.getAllLoans());
        for (Loan loan : LOANS) {
            if (loan.getMemberId().equals(memberId) && loan.getItemId().equals(itemId)) {
                return loan.getDueDay();
            }
        }
        return -1;
    }

    public static List<Loan> getLoans() {
        LOANS.clear();
        LOANS.addAll(DatabaseAccess.getAllLoans());
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
        LOANS.clear();
        LOANS.addAll(DatabaseAccess.getAllLoans());
        List<String> charges = new ArrayList<>();
        for (Loan loan : LOANS) {
            String summary = chargeOverdue(loan, currentDay);
            if (summary != null) {
                charges.add(summary);
            }
        }
        for (Loan loan : LOANS) {
            DatabaseAccess.insertLoan(loan);
        }
        return charges;
    }
}
