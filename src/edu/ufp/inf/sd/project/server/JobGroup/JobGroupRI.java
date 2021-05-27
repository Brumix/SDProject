package edu.ufp.inf.sd.project.server.JobGroup;

import edu.ufp.inf.sd.project.client.ClientRI;
import edu.ufp.inf.sd.project.client.WorkerRI;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface JobGroupRI extends Remote {

     void attach(WorkerRI w)throws RemoteException;
     void print()throws RemoteException;
     String JobGroupStr() throws RemoteException;
     void notifyall()throws RemoteException;
     Boolean hasTask() throws RemoteException;
     void update(WorkerRI w, int result)throws RemoteException;
     ClientRI getClient() throws  RemoteException;
     int getId() throws  RemoteException;
     String whoIam() throws  RemoteException;
     void freeWorkers()throws RemoteException;
}
