package edu.ufp.inf.sd.project.client;


import java.rmi.Remote;
import java.rmi.RemoteException;


public interface WorkerRabbitRI extends Remote,Runnable {
    String getPersonalId() throws RemoteException;
    String getQueuId()throws  RemoteException;
}
