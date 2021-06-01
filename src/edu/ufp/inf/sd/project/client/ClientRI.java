package edu.ufp.inf.sd.project.client;

import edu.ufp.inf.sd.project.server.JobGroup.JobGroupRI;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientRI extends Remote {
    void printResult(String path, Integer result)throws RemoteException;
    void getCredits(int value) throws RemoteException;
    void sendCredits(int value) throws RemoteException;
    void createWorkers(String idQueue, int totalWorkers, JobGroupRI jobGroup)throws RemoteException;
    String getName()throws RemoteException;
}
