package com.blender.mainak.passwordmanager;

public class Record {

    public String username;
    public String password;

    Record() {
        username = null;
        password = null;
    }

    Record(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
