package com.android.app.splitwise_clone.model;

public enum ExpenseListViewType {
    DATE("date"),
    CATEGORY("category"),
    ARCHIVE("archive");

    private String value;

    ExpenseListViewType(String val) {
        value = val;
    }

    public String getValue() {
        return value;
    }
}
