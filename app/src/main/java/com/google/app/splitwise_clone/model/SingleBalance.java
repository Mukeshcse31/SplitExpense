package com.google.app.splitwise_clone.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

public class SingleBalance implements Parcelable {

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

    protected SingleBalance(Parcel in){
        amount = in.readFloat();
        status = in.readString();
    }


    public static final Creator<SingleBalance> CREATOR = new Creator<SingleBalance>() {
        @Override
        public SingleBalance createFromParcel(Parcel in) {
            return new SingleBalance(in);
        }

        @Override
        public SingleBalance[] newArray(int size) {
            return new SingleBalance[size];
        }
    };


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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeFloat(amount);
        dest.writeString(status);
    }
}
