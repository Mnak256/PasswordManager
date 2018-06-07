package com.blender.mainak.passwordmanager;

public class Record {

    public String username;
    public String password;

    public Record() {
        username = null;
        password = null;
    }

    public Record(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
