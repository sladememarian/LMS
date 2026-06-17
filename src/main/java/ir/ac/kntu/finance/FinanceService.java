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

public class FinanceService {

    private static final List<Transaction> TX_LOGS = new ArrayList<>();
    private static final String FILE_PATH = "finance_secure.json";
    private static final double TAX_RATE = 0.10;

    private static byte[] getEncryptionKey() {
        return EnvConfig.get("MASTER_ADMIN_DATABASE_PASSWORD", "fallbackKey").getBytes();
    }

    public static boolean checkBorrowingPermission(String memberId) {
        if (TX_LOGS.isEmpty()) {
            loadTransactions();
        }

        for (Transaction tx : TX_LOGS) {
            if (tx.getMemberId().equals(memberId) && "DEBT".equals(tx.getType())) {
                return false;
            }
        }

        return true;
    }

    public static void proccessWalletCharge(Persona persona, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        PersonaService.updateWalletBalance(persona.getEmail(), amount);
        logTransaction(persona.getMemberId(), amount, "CHARGE", "Wallet charged");
    }

    private static void logTransaction(String memberId, int amount, String type, String description) {
        if(TX_LOGS.isEmpty()) {
            loadTransactions();
        }
        // String txId = "TX-" + System.currentTimeMillis();
        String txId = "TX-" + ((int) (Math.random() * 900_000) + 100_000);
        Transaction tx = new Transaction(txId, memberId, amount, type, description);
        TX_LOGS.add(tx);
        saveTransactions();
    }

    private static void saveTransactions() {
        StringBuilder jsonBuilder = new StringBuilder("[\n");
        String fieldSuffix = "\",\n";
        for (int i = 0; i < TX_LOGS.size(); i++) {
            Transaction tx = TX_LOGS.get(i);
            jsonBuilder.append("  {\n")
                    .append("    \"txId\": \"").append(tx.getTransactionId()).append(fieldSuffix)
                    .append("    \"mid\": \"").append(tx.getMemberId()).append(fieldSuffix)
                    .append("    \"amt\": \"").append(tx.getAmount()).append(fieldSuffix)
                    .append("    \"type\": \"").append(tx.getType()).append(fieldSuffix)
                    .append("    \"desc\": \"").append(tx.getDescription()).append("\"\n")
                    .append("  }");
            if (i < TX_LOGS.size() - 1) {
                jsonBuilder.append(",");
            }
            jsonBuilder.append("\n");
        }
        jsonBuilder.append("]");

        try (FileOutputStream fos = new FileOutputStream(FILE_PATH)) {
            byte[] keyBytes = getEncryptionKey();
            byte[] rawBytes = jsonBuilder.toString().getBytes();
            byte[] encryptedData = new byte[rawBytes.length];
            for (int i = 0; i < rawBytes.length; i++) {
                encryptedData[i] = (byte) (rawBytes[i] ^ keyBytes[i % keyBytes.length]);
            }
            fos.write(encryptedData);
        } catch (IOException e) {
            System.err.println("Error saving transactions: " + e.getMessage());
        }
    }

    public static boolean proccessExtentionPayment(Persona persona, int amount) {
        int tax = (int) (amount * TAX_RATE);
        int totalAmount = amount + tax;

        if (amount <= 0) {
            return false;
        }
        if(persona.getWalletBalance() < totalAmount) {
            return false;
        }
        
        PersonaService.updateWalletBalance(persona.getEmail(), -totalAmount);
        PersonaService.transferToAdmin(tax);
        logTransaction(persona.getMemberId(), amount, "DEBT", "Extension payment amount");
        logTransaction(persona.getMemberId(), tax, "TAX", "Extension payment tax");
        return true;
    }

    public static void loadTransactions() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] encryptedData = fis.readAllBytes();
            byte[] keyBytes = getEncryptionKey();
            byte[] decryptedData = new byte[encryptedData.length];
            for (int i = 0; i < encryptedData.length; i++) {
                decryptedData[i] = (byte) (encryptedData[i] ^ keyBytes[i % keyBytes.length]);
            }
            parseTxFromJson(new String(decryptedData));
        } catch (IOException e) {
            System.err.println("Error loading transactions: " + e.getMessage());
        }
    }

    private static void parseTxFromJson(String raw) {
        TX_LOGS.clear();
        String clean = raw.replace("[", "").replace("]", "").trim();
        if (clean.isEmpty()) {
            return;
        }
        String[] chunks = clean.split("\\},");
        for (String chunk : chunks) {
            String obj = chunk.replace("{", "").replace("}", "").trim();
            String txId = fetchToken(obj, "txId");
            String mid = fetchToken(obj, "mid");
            String amtStr = fetchToken(obj, "amt");
            String type = fetchToken(obj, "type");
            String desc = fetchToken(obj, "desc");

            if (txId != null && amtStr != null) {
                int amt = Integer.parseInt(amtStr);
                TX_LOGS.add(new Transaction(txId, mid, amt, type, desc));
            }
        }
    }

    private static String fetchToken(String source, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = source.indexOf(pattern);
        if (start == -1) {
            return null;
        }
        start += pattern.length();
        return source.substring(start, source.indexOf("\"", start));
    }

}
