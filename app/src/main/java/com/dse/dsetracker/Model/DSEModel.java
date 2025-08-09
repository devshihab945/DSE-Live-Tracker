package com.dse.dsetracker.Model;

public class DSEModel {
    private String companyName;
    private double sharePrice;
    private double change;
    private String changeRate;

    public DSEModel(String companyName, double sharePrice, double change, String changeRate) {
        this.companyName = companyName;
        this.sharePrice = sharePrice;
        this.change = change;
        this.changeRate = changeRate;
    }

    public String getCompanyName() {
        return companyName;
    }

    public double getSharePrice() {
        return sharePrice;
    }

    public double getChange() {
        return change;
    }

    public String getChangeRate() {
        return changeRate;
    }
}

