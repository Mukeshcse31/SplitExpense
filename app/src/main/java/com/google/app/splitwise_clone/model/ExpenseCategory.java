package com.google.app.splitwise_clone.model;

public enum ExpenseCategory {
    FOOD("food"),
    BULB("bulb"),
    GAS("gas"),
    CART("cart"),
    HOUSE("house"),
    OTHER("other");

private String value;

ExpenseCategory(String val){
value = val;
    }

    public String getValue(){
    return value;
    }
}
