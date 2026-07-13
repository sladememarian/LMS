package ir.ac.kntu.finance;

import java.util.ArrayList;
import java.util.List;

import ir.ac.kntu.exception.NotFoundException;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.util.LoanRepository;
import ir.ac.kntu.util.SystemSettingsService;

public class LoanService {
    private static final List<Loan> LOANS = new ArrayList<>();

    static {
        LOANS.addAll(LoanRepository.getAllLoans());
    }

    private static void reload() {
        LOANS.clear();
        LOANS.addAll(LoanRepository.getAllLoans());
    }

    public static void recordLoan(String memberId, String itemId, int currentDay, int loanPeriodDays) {
        reload();
        int dueDay = currentDay + loanPeriodDays;
        LOANS.add(new Loan(memberId, itemId, currentDay, dueDay));
        LoanRepository.insertLoan(new Loan(memberId, itemId, currentDay, dueDay));
    }

    public static boolean clearLoan(String memberId, String itemId) {
        reload();
        boolean removed = LOANS.removeIf(loan -> loan.getMemberId().equals(memberId)
                && loan.getItemId().equals(itemId));
        if (removed) {
            LoanRepository.deleteLoan(memberId, itemId);
        }
        return removed;
    }

    public static boolean extendLoan(String memberId, String itemId, int extraDays) {
        reload();
        for (Loan loan : LOANS) {
            if (loan.getMemberId().equals(memberId) && loan.getItemId().equals(itemId)) {
                loan.setDueDay(loan.getDueDay() + extraDays);
                loan.setLastChargedDay(loan.getDueDay());
                LoanRepository.insertLoan(loan);
                return true;
            }
        }
        return false;
    }

    public static int getDueDay(String memberId, String itemId) {
        reload();
        for (Loan loan : LOANS) {
            if (loan.getMemberId().equals(memberId) && loan.getItemId().equals(itemId)) {
                return loan.getDueDay();
            }
        }
        throw new NotFoundException(
            "Loan not found for member " + memberId + " item " + itemId
        );
    }

    public static boolean isOverdue(String memberId, String itemId, int currentDay) {
        reload();
        for (Loan loan : LOANS) {
            if (loan.getMemberId().equals(memberId) && loan.getItemId().equals(itemId)) {
                return loan.isOverdue(currentDay);
            }
        }
        return false;
    }

    public static List<Loan> getLoans() {
        reload();
        return new ArrayList<>(LOANS);
    }

    public static List<Loan> getOverdueLoans(int currentDay) {
        reload();
        List<Loan> overdue = new ArrayList<>();
        for (Loan loan : LOANS) {
            if (loan.isOverdue(currentDay)) {
                overdue.add(loan);
            }
        }
        return overdue;
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
        int fineRate = SystemSettingsService.getFineRate();
        FinanceService.recordDebt(borrower, fineRate,
                "Overdue fine for " + loan.getItemId() + " (day " + currentDay + ")");
        return loan.getMemberId() + " +" + fineRate + " (" + loan.getItemId() + ")";
    }

    public static List<String> accrueOverdueDebts(int currentDay) {
        reload();
        List<String> charges = new ArrayList<>();
        for (Loan loan : LOANS) {
            String summary = chargeOverdue(loan, currentDay);
            if (summary != null) {
                charges.add(summary);
            }
        }
        for (Loan loan : LOANS) {
            LoanRepository.insertLoan(loan);
        }
        return charges;
    }
}
