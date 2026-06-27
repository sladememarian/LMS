package ir.ac.kntu.finance;

import java.util.ArrayList;
import java.util.List;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.util.DatabaseAccess;

public class FinanceService {
    private static final List<Transaction> TX_LOGS = new ArrayList<>();
    private static final double TAX_RATE = 0.10;
    private static final String TYPE_DEBT = "DEBT";
    private static final String TYPE_DEBT_PAYMENT = "DEBT_PAYMENT";
    private static final String TYPE_TAX = "TAX";

    private static void ensureLoaded() {
        TX_LOGS.clear();
        TX_LOGS.addAll(DatabaseAccess.getAllTransactions());
    }

    public static boolean checkBorrowingPermission(String memberId) {
        return getOutstandingDebt(memberId) <= 0;
    }

    public static int getOutstandingDebt(String memberId) {
        ensureLoaded();
        int debt = 0;
        for (Transaction tx : TX_LOGS) {
            if (tx.getMemberId().equals(memberId)) {
                if (TYPE_DEBT.equals(tx.getType())) {
                    debt += tx.getAmount();
                } else if (TYPE_DEBT_PAYMENT.equals(tx.getType())) {
                    debt -= tx.getAmount();
                }
            }
        }
        return debt;
    }

    public static void proccessWalletCharge(Persona persona, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        PersonaService.updateWalletBalance(persona.getEmail(), amount);
        logTransaction(persona.getMemberId(), amount, "CHARGE", "Wallet charged");
    }

    private static void logTransaction(String memberId, int amount, String type, String description) {
        ensureLoaded();
        String txId = "TX-" + ((int) (Math.random() * 900_000) + 100_000);
        Transaction tx = new Transaction(txId, memberId, amount, type, description);
        TX_LOGS.add(tx);
        DatabaseAccess.insertTransaction(tx);
    }

    public static boolean proccessExtentionPayment(Persona persona, int amount) {
        int tax = (int) (amount * TAX_RATE);
        int totalAmount = amount + tax;

        if (amount <= 0) {
            return false;
        }
        if (persona.getWalletBalance() < totalAmount) {
            return false;
        }

        PersonaService.updateWalletBalance(persona.getEmail(), -totalAmount);
        PersonaService.transferToAdmin(tax);
        logTransaction(persona.getMemberId(), amount, TYPE_DEBT, "Extension payment amount");
        logTransaction(persona.getMemberId(), tax, TYPE_TAX, "Extension payment tax");
        return true;
    }

    public static List<Transaction> getTransactionsForMember(String memberId) {
        ensureLoaded();
        List<Transaction> result = new ArrayList<>();
        for (Transaction tx : TX_LOGS) {
            if (tx.getMemberId() != null && tx.getMemberId().equals(memberId)) {
                result.add(tx);
            }
        }
        result.sort((left, right) -> Long.compare(left.getTimestamp(), right.getTimestamp()));
        return result;
    }

    public static List<Transaction> getAllTransactions() {
        ensureLoaded();
        List<Transaction> all = new ArrayList<>(TX_LOGS);
        all.sort((left, right) -> Long.compare(left.getTimestamp(), right.getTimestamp()));
        return all;
    }

    public static int getTaxRevenueCollected() {
        ensureLoaded();
        int total = 0;
        for (Transaction tx : TX_LOGS) {
            if (TYPE_TAX.equals(tx.getType())) {
                total += tx.getAmount();
            }
        }
        return total;
    }

    public static void recordDebt(Persona persona, int amount, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Debt amount must be positive");
        }
        logTransaction(persona.getMemberId(), amount, TYPE_DEBT, description);
    }

    public static boolean payDebt(Persona persona) {
        int debt = getOutstandingDebt(persona.getMemberId());
        if (debt <= 0) {
            return false;
        }
        int tax = (int) (debt * TAX_RATE);
        int total = debt + tax;
        if (persona.getWalletBalance() < total) {
            return false;
        }
        PersonaService.updateWalletBalance(persona.getEmail(), -total);
        PersonaService.transferToAdmin(tax);
        logTransaction(persona.getMemberId(), debt, TYPE_DEBT_PAYMENT, "Debt cleared");
        logTransaction(persona.getMemberId(), tax, TYPE_TAX, "Debt payment tax");
        return true;
    }
}
