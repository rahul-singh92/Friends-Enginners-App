package com.jitendersingh.friendsengineer.models;

public class WageSlipItem {

    private final String branch;
    private final String month;
    private final String workerId;

    public WageSlipItem(String branch, String month, String workerId) {
        this.branch = branch;
        this.month = month;
        this.workerId = workerId;
    }

    public String getBranch() {
        return branch;
    }

    public String getMonth() {
        return month;
    }

    public String getWorkerId() {
        return workerId;
    }
}