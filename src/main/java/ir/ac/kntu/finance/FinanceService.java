package ir.ac.kntu.finance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import ir.ac.kntu.exception.InsufficientFundsException;
import ir.ac.kntu.exception.ValidationException;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.util.TransactionRepository;

public class FinanceService {
    private static final List<Transaction> TX_LOGS = new ArrayList<>();
    private static final double TAX_RATE = 0.10;
    private static final String TYPE_DEBT = "DEBT";
    private static final String TYPE_DEBT_PAYMENT = "DEBT_PAYMENT";
    private static final String TYPE_TAX = "TAX";

    // The static TX_LOGS cache is rebuilt from the DB on every read via a
    // non-atomic clear()+addAll(). Two concurrent callers (the GUI runs finance
    // reads on a 4-thread pool) could interleave one thread's clear() with
    // another's addAll(), doubling the list and showing every transaction twice.
    // Every public entry point is synchronized on this monitor so load+read is
    // atomic and the cache can never be observed mid-rebuild.
    private static void ensureLoaded() {
        TX_LOGS.clear();
        TX_LOGS.addAll(TransactionRepository.getAllTransactions());
    }

    public static synchronized boolean checkBorrowingPermission(String memberId) {
        return getOutstandingDebt(memberId) <= 0;
    }

    public static synchronized int getOutstandingDebt(String memberId) {
        ensureLoaded();
        return TX_LOGS.stream()
                .filter(tx -> tx.getMemberId() != null && tx.getMemberId().equals(memberId))
                .mapToInt(FinanceService::signedDebt)
                .sum();
    }

    public static synchronized void proccessWalletCharge(Persona persona, int amount) {
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
        TransactionRepository.insertTransaction(tx);
    }

    public static synchronized void proccessExtentionPayment(Persona persona, int amount) {
        if (amount <= 0) {
            throw new ValidationException("Extension amount must be positive.");
        }
        int tax = (int) (amount * TAX_RATE);
        int totalAmount = amount + tax;
        if (persona.getWalletBalance() < totalAmount) {
            throw new InsufficientFundsException(
                "Insufficient funds. Required: " + totalAmount
                + ", available: " + persona.getWalletBalance()
            );
        }
        PersonaService.updateWalletBalance(persona.getEmail(), -totalAmount);
        PersonaService.transferToAdmin(tax);
        logTransaction(persona.getMemberId(), amount, TYPE_DEBT, "Extension payment amount");
        logTransaction(persona.getMemberId(), tax, TYPE_TAX, "Extension payment tax");
    }

    public static synchronized List<Transaction> getTransactionsForMember(String memberId) {
        ensureLoaded();
        return TX_LOGS.stream()
                .filter(tx -> tx.getMemberId() != null && tx.getMemberId().equals(memberId))
                .sorted(Comparator.comparingLong(Transaction::getTimestamp))
                .collect(Collectors.toList());
    }

    public static synchronized List<Transaction> getAllTransactions() {
        ensureLoaded();
        return TX_LOGS.stream()
                .sorted(Comparator.comparingLong(Transaction::getTimestamp))
                .collect(Collectors.toList());
    }

    public static synchronized int getTotalOutstandingDebt() {
        ensureLoaded();
        return TX_LOGS.stream()
                .mapToInt(FinanceService::signedDebt)
                .sum();
    }

    public static synchronized List<Transaction> getDebtAndPaymentTransactions() {
        ensureLoaded();
        return TX_LOGS.stream()
                .filter(tx -> TYPE_DEBT.equals(tx.getType()) || TYPE_DEBT_PAYMENT.equals(tx.getType()))
                .collect(Collectors.toList());
    }

    public static synchronized List<Transaction> getOpenDebtTransactions() {
        ensureLoaded();
        return TX_LOGS.stream()
                .filter(tx -> TYPE_DEBT.equals(tx.getType()))
                .collect(Collectors.toList());
    }

    public static synchronized int getTaxRevenueCollected() {
        ensureLoaded();
        return TX_LOGS.stream()
                .filter(tx -> TYPE_TAX.equals(tx.getType()))
                .mapToInt(Transaction::getAmount)
                .sum();
    }

    // Signed contribution of a transaction to outstanding debt: DEBT adds,
    // DEBT_PAYMENT subtracts, everything else is neutral.
    private static int signedDebt(Transaction tx) {
        if (TYPE_DEBT.equals(tx.getType())) {
            return tx.getAmount();
        }
        if (TYPE_DEBT_PAYMENT.equals(tx.getType())) {
            return -tx.getAmount();
        }
        return 0;
    }

    public static synchronized void recordDebt(Persona persona, int amount, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Debt amount must be positive");
        }
        logTransaction(persona.getMemberId(), amount, TYPE_DEBT, description);
    }

    public static synchronized void payDebt(Persona persona) {
        int debt = getOutstandingDebt(persona.getMemberId());
        if (debt <= 0) {
            throw new ValidationException("No outstanding debt to pay.");
        }
        int tax = (int) (debt * TAX_RATE);
        int total = debt + tax;
        if (persona.getWalletBalance() < total) {
            throw new InsufficientFundsException(
                "Insufficient funds to pay debt. Required: " + total
                + ", available: " + persona.getWalletBalance()
            );
        }
        PersonaService.updateWalletBalance(persona.getEmail(), -total);
        PersonaService.transferToAdmin(tax);
        logTransaction(persona.getMemberId(), debt, TYPE_DEBT_PAYMENT, "Debt cleared");
        logTransaction(persona.getMemberId(), tax, TYPE_TAX, "Debt payment tax");
    }
}
