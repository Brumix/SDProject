package edu.ufp.inf.sd.project.server.JobGroup;

import edu.ufp.inf.sd.project.client.WorkerRI;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface JobGroupRI extends Remote {

     void addWorker(WorkerRI w)throws RemoteException;
     void print()throws RemoteException;
     String JobGroupStr() throws RemoteException;
     void execute()throws RemoteException;
     Boolean hasTask() throws RemoteException;
     void getResultFromWorker(WorkerRI w, int result)throws RemoteException;
}
