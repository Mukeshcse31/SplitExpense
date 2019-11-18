package com.android.app.splitwise_clone.model;

import java.util.HashMap;
import java.util.Map;

public class Balance {

    private Float amount;
    private Map<String, Map<String, Float>> groups = new HashMap<>();

    public Balance() {
    }

    public Balance(Float amount, Map<String, Map<String, Float>> groups) {
        this.amount = amount;
        this.groups = groups;
    }

    public Float getAmount() {
        return amount;
    }

    public void setAmount(Float amount) {
        this.amount = amount;
    }

    public Map<String, Map<String, Float>> getGroups() {
        return groups;
    }

    public void setGroups(Map<String, Map<String, Float>> groups) {
        this.groups = groups;
    }
}
