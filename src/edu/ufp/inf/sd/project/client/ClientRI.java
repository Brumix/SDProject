package edu.ufp.inf.sd.project.client;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientRI extends Remote {
    void printResult(String path, Integer result)throws RemoteException;
    void getCredits(int value) throws RemoteException;
    void sendCredits(int value) throws RemoteException;
}
