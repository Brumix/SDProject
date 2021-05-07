package edu.ufp.inf.sd.project.server.Authentication.Factory;

import edu.ufp.inf.sd.project.server.Models.User;
import edu.ufp.inf.sd.project.server.SessionJobShop.JobShopSessionRI;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface JobShopFactoryRI extends Remote {
    void print(String msg) throws RemoteException;
    void register (User u) throws RemoteException;
    JobShopSessionRI login (User u) throws RemoteException;
}
