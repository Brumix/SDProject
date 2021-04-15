package edu.ufp.inf.sd.project.server.Session;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface JobShopSessionRI extends Remote {

    void logout() throws RemoteException;
    int runTS(String jsspInstance) throws RemoteException;
}
