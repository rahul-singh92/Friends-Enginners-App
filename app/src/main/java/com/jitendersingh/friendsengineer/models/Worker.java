package com.jitendersingh.friendsengineer.models;

public class Worker {
    private String docId;
    private String name;
    private String fatherName;
    private String amount;
    private String reason;
    private String requestTime;

    public Worker(String docId, String name, String fatherName, String amount, String reason, String requestTime) {
        this.docId = docId;
        this.name = name;
        this.fatherName = fatherName;
        this.amount = amount;
        this.reason = reason;
        this.requestTime = requestTime;
    }

    public String getDocId() { return docId; }
    public void setDocId(String docId) { this.docId = docId; }

    public String getName() { return name; }
    public String getFatherName() { return fatherName; }
    public String getAmount() { return amount; }
    public String getReason() { return reason; }
    public String getRequestTime() { return requestTime; }
    public void setRequestTime(String requestTime) { this.requestTime = requestTime; }
}
