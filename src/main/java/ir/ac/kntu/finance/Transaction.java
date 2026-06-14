package ir.ac.kntu.finance;

public class Transaction {
    private final String transactionId;
    private final String memberId;
    private final int amount;
    private final String type;
    private final String description;

    public Transaction(String txId, String memberId, int amount, String type, String desc) {
        this.transactionId = txId;
        this.memberId = memberId;
        this.amount = amount;
        this.type = type;
        this.description = desc;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getMemberId() {
        return memberId;
    }

    public int getAmount() {
        return amount;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }
}