package edu.ufp.inf.sd.project.server.Session;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface JobShopSessionRI extends Remote {

    void print(String msg) throws RemoteException;
    void logout() throws RemoteException;
    int runTS(String jsspInstance) throws RemoteException;
}
