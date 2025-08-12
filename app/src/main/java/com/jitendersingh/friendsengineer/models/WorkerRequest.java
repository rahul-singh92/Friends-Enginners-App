package com.jitendersingh.friendsengineer.models;

public class WorkerRequest {
    private String amount;
    private String status;
    private String date;

    public WorkerRequest(String amount, String status, String date) {
        this.amount = amount;
        this.status = status;
        this.date = date;
    }

    public String getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getDate() { return date; }
}
