package com.google.app.splitwise_clone.model;

import java.util.HashMap;
import java.util.Map;

public class SingleBalance {

private String amount;
private String status;

    public SingleBalance() {
    }

    public SingleBalance(String amount, String status) {
        this.amount = amount;
        this.status = status;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
