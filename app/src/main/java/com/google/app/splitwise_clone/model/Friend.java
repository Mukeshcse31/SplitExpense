package com.google.app.splitwise_clone.model;

//Getter and Setter are very important for firebase writing

import java.util.HashMap;
import java.util.Map;

public class Friend {

    private int id;
    private String name;
    private String email;
    private Map<String, Boolean> groups = new HashMap<>();
    private Map<String, SingleBalance> balances = new HashMap<>();

    public Friend() {
    }

    public Friend(int id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Map<String, Boolean> getGroups() {
        return groups;
    }

    public void setGroups(Map<String, Boolean> groups) {
        this.groups = groups;
    }

    public void addToGroup(String groupName){
            this.groups.put(groupName, true);
    }


    public void removeFromGroup(String groupName){
            if(groups.containsKey(groupName))
                groups.remove(groupName);
    }

    public void addToBalance(String friendName, SingleBalance sb){
        this.balances.put("qq", new SingleBalance("44","gg"));
    }

    public void removeBalance(String friendName){
        if(balances.containsKey(friendName)){
            balances.remove(friendName);
        }
    }

    public Map<String, SingleBalance> getBalances() {
        return balances;
    }

    public void setBalances(Map<String, SingleBalance> balances) {
        this.balances = balances;
    }
}
