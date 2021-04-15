package edu.ufp.inf.sd.project.server.Authentication.Factory;

import edu.ufp.inf.sd.project.server.Authentication.DataBase.DBMockup;
import edu.ufp.inf.sd.project.server.Models.User;
import edu.ufp.inf.sd.project.server.Session.JobShopSessionImpl;
import edu.ufp.inf.sd.project.server.Session.JobShopSessionRI;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JobShopFactoryImpl extends UnicastRemoteObject implements JobShopFactoryRI{
    private HashMap<String,JobShopSessionRI> sessions = new HashMap<>();
    private DBMockup db;

    public JobShopFactoryImpl() throws RemoteException {
        super();
        this.db = new DBMockup().getInstance();
    }

    @Override
    public void print(String msg) throws RemoteException {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "someone called me with msg = {0}", new Object[]{msg});

    }

    @Override
    public void register(User u) throws RemoteException {
        db.register(u);
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "["+u.getName()+"]"+ "Registered Successfully!");

    }

    @Override
    public JobShopSessionRI login(User u) throws RemoteException {

        if (db.exists(u)) {
           JobShopSessionRI jobShopSession = new JobShopSessionImpl(this, u);
            sessions.put(u.getName(), jobShopSession);
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "["+u.getName()+"]"+ "Session Created Successfully!");
            return jobShopSession;

        }
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "["+u.getName()+"]"+ "Session Failed!");
        return null;
    }

    public void remove(String name){
        sessions.remove(name);
    }
}
