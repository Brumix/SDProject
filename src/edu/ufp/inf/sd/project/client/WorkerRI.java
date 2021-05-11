package edu.ufp.inf.sd.project.client;

import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface WorkerRI extends Remote {
    void runAlgorthim() throws RemoteException;
    void print(String msg) throws  RemoteException;
    void giveTask(File file)throws  RemoteException;
    public String whoIAm() throws RemoteException;
}