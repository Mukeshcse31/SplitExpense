package com.google.app.splitwise_clone.model;

import java.util.HashMap;
import java.util.Map;

public class Group {

    private String name;
private Map<String, SingleBalance> members = new HashMap<>();

    public Group() {
    }

    public Group(String name) {
        this.name = name;
    }

    public Group(String name, Map<String, SingleBalance> members) {
        this.name = name;
        this.members = members;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, SingleBalance> getMembers() {
        return members;
    }

    public void setMembers(Map<String, SingleBalance> members) {
        this.members = members;
    }

    public void addMember(String memberName, SingleBalance sb){
        this.members.put(memberName, sb);
    }


    public void removeMember(String memberName){
        if(members.containsKey(memberName))
            members.remove(memberName);
    }

}
