package com.google.app.splitwise_clone.model;

//Getter and Setter are very important for fire base writing

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

public class User implements Parcelable {

//    private int id;
    private String name;
    private String email;
    private Float amount;
    private String imageUrl;
    private Map<String, Boolean> friends = new HashMap<>();
private Balance balances;

    public User() {
    }

    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }

    protected User(Parcel in) {
        name = in.readString();
        email = in.readString();
        if (in.readByte() == 0) {
            amount = null;
        } else {
            amount = in.readFloat();
        }
        imageUrl = in.readString();
        friends = new HashMap<String, Boolean>();
        in.readMap(friends, HashMap.class.getClassLoader());
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Float getAmount() {
        return amount;
    }

    public void setAmount(Float amount) {
        this.amount = amount;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Map<String, Boolean> getFriends() {
        return friends;
    }

    public void setFriends(Map<String, Boolean> friends) {
        this.friends = friends;
    }

    public void addAsFriend(String friendName){
            this.friends.put(friendName, true);
    }

    public void removeAsFriend(String friendName){
        friends.remove(friendName);
    }

    public Balance getBalances() {
        return balances;
    }

    public void setBalances(Balance balances) {
        this.balances = balances;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(email);
        if (amount == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeFloat(amount);
        }
        dest.writeString(imageUrl);
    }
}
