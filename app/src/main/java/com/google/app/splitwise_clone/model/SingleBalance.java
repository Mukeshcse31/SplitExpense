package com.google.app.splitwise_clone.model;

import java.util.HashMap;
import java.util.Map;

public class SingleBalance {

private float amount;
private String status;

    public SingleBalance() {
        this.amount = 0.0f;
        this.status = "no expenses";
    }

    public SingleBalance(float amount, String status) {
        this.amount = amount;
        this.status = status;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
