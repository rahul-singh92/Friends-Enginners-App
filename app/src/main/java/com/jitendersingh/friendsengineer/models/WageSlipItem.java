package com.jitendersingh.friendsengineer.models;

public class WageSlipItem {

    private String branch;
    private String month;

    public WageSlipItem(String branch, String month) {
        this.branch = branch;
        this.month = month;
    }

    public String getBranch() {
        return branch;
    }

    public String getMonth() {
        return month;
    }
}