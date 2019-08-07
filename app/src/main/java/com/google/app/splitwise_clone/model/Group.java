package com.google.app.splitwise_clone.model;


import java.util.List;

class Group {

    private String id;
    private String name;
private List<String> friends;

    public Group() {
    }

    public Group(String id, String name, List<String> friends) {
        this.id = id;
        this.name = name;
        this.friends = friends;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getFriends() {
        return friends;
    }

    public void setFriends(List<String> friends) {
        this.friends = friends;
    }
}
