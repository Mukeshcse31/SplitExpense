package com.google.app.splitwise_clone.model;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.HashMap;
import java.util.Map;

public class Expense implements Parcelable {

    private String dateSpent;
    private String payer;
    private String description;
    private float total = 0.2f;
    private Map<String, SingleBalance> splitExpense = new HashMap<>();


    public Expense() {
    }

    public Expense(String dateSpent, String memberSpent, String description, float total) {
        this.dateSpent = dateSpent;
        this.payer = memberSpent;
        this.description = description;
        this.total = total;
    }

    protected Expense(Parcel in){

        dateSpent = in.readString();
        payer = in.readString();
        description = in.readString();
        total = in.readFloat();
        splitExpense = new HashMap<String, SingleBalance>();
        in.readMap(splitExpense, SingleBalance.class.getClassLoader());
    }

    public static final Creator<Expense> CREATOR = new Creator<Expense>() {
        @Override
        public Expense createFromParcel(Parcel in) {
            return new Expense(in);
        }

        @Override
        public Expense[] newArray(int size) {
            return new Expense[size];
        }
    };


    public String getDateSpent() {
        return dateSpent;
    }

    public void setDateSpent(String dateSpent) {
        this.dateSpent = dateSpent;
    }

    public String getPayer() {
        return payer;
    }

    public void setPayer(String payer) {
        this.payer = payer;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public float getTotal() {
        return total;
    }

    public void setTotal(float total) {
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
        splitExpense.remove(memberName);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeString(dateSpent);
        dest.writeString(payer);
        dest.writeString(description);
        dest.writeFloat(total);
        dest.writeMap(splitExpense);
    }
}
