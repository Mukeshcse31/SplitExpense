package com.google.app.splitwise_clone.model;

import java.util.HashMap;
import java.util.Map;

public class Expense {

    private String dateSpent;
    private String memberSpent;
    private String description;
    private String total;
    private Map<String, SingleBalance> splitExpense = new HashMap<>();


    public Expense() {
    }

    public Expense(String dateSpent, String memberSpent, String description, String total) {
        this.dateSpent = dateSpent;
        this.memberSpent = memberSpent;
        this.description = description;
        this.total = total;
    }

    public String getDateSpent() {
        return dateSpent;
    }

    public void setDateSpent(String dateSpent) {
        this.dateSpent = dateSpent;
    }

    public String getMemberSpent() {
        return memberSpent;
    }

    public void setMemberSpent(String memberSpent) {
        this.memberSpent = memberSpent;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public Map<String, SingleBalance> getSplitExpense() {
        return splitExpense;
    }

    public void setSplitExpense(Map<String, SingleBalance> splitExpense) {
        this.splitExpense = splitExpense;
    }

    public void addMember(String memberName, SingleBalance sb){
        this.splitExpense.put(memberName, sb);
    }


    public void removeMember(String memberName){
        if(splitExpense.containsKey(memberName))
            splitExpense.remove(memberName);
    }

}
