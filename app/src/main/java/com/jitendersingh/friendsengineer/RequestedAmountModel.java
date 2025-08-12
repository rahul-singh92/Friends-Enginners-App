package com.jitendersingh.friendsengineer;

public class RequestedAmountModel {
    String amount, reason, date, status;

    public RequestedAmountModel(String amount, String reason, String date, String status) {
        this.amount = amount;
        this.reason = reason;
        this.date = date;
        this.status = status;
    }
}
