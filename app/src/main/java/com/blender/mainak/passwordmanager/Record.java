package com.blender.mainak.passwordmanager;

public class Record {

    String domain, username, password;

    Record() {
        domain = null;
        username = null;
        password = null;
    }

    Record(String domain, String username, String password) {
        this.domain = domain;
        this.username = username;
        this.password = password;
    }
}
