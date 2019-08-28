package com.google.app.splitwise_clone.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

//getters and setters are required for all the variables for DB read/write
public class SingleBalance implements Parcelable {

private float amount;
private String status;
private String name;
private Map<String, Float> splitDues = new HashMap<>();

public SingleBalance(){

}
    public SingleBalance(String key) {
        this.amount = 0.0f;
        this.status = "no expenses";
        this.name = key;

    }

    public SingleBalance( float amount, String status, String key) {
        this.amount = amount;
        this.status = status;
        this.name = key;
    }

    protected SingleBalance(Parcel in){
        amount = in.readFloat();
        status = in.readString();
        name = in.readString();
        splitDues = new HashMap<String, Float>();
        in.readMap(splitDues, HashMap.class.getClassLoader());

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Map<String, Float> getSplitDues() {
        return splitDues;
    }

    public void setSplitDues(Map<String, Float> splitDues) {
        this.splitDues = splitDues;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeFloat(amount);
        dest.writeString(status);
        dest.writeString(name);
        dest.writeMap(splitDues);
    }
}