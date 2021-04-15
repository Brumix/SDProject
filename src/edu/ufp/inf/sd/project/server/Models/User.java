package edu.ufp.inf.sd.project.server.Models;

import java.io.Serializable;

public class User implements Serializable {

    private final String name;
    private final String pass;


    public User(String name, String pass) {
        this.name = name;
        this.pass = pass;
    }

    @Override
    public String toString() {
        return "User{" + "uname=" + this.name + ", pword=" + this.pass + '}';
    }

    public String getName() {
        return name;
    }

    public String getPass() {
        return pass;
    }
}
