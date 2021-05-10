package edu.ufp.inf.sd.project.server.SessionJobShop;

import edu.ufp.inf.sd.project.client.WorkerRI;
import edu.ufp.inf.sd.project.server.JobGroup.JobGroupRI;

import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface JobShopSessionRI extends Remote {
    void print(String msg) throws RemoteException;
    void logout() throws RemoteException;
    int createJobGroup(File Jss)throws RemoteException;
    String listJobGroups()throws RemoteException;
    void executeJobGroup(int id) throws RemoteException;
    JobGroupRI getJobGroup(int id) throws  RemoteException;
    void printALL()throws RemoteException;
    void sendResult(WorkerRI bestWorker, Integer integer)throws RemoteException;
}
