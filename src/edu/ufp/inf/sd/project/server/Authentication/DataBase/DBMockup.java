package edu.ufp.inf.sd.project.server.Authentication.DataBase;

import edu.ufp.inf.sd.project.server.Models.User;

import java.util.ArrayList;

public class  DBMockup {
    private final ArrayList<User> users = new ArrayList<>();

    private DBMockup instance;

    public DBMockup() {
        this.users.add(new User("guest", "guest"));
        this.users.add(new User("test", "test"));
    }

    public void register(User u) {
        if (!exists(u)) {
            users.add(u);
        }
    }

    public boolean exists(User u) {
        for (User usr : this.users) {
            if (usr.getName().compareTo(u.getName()) == 0 && usr.getPass().compareTo(u.getPass()) == 0) {
                return true;
            }
        }
        return false;
    }

    public DBMockup getInstance() {
        if (this.instance == null)
            this.instance = new DBMockup();
        return this.instance;
    }

}
