package edu.ufp.inf.sd.project.server.JobGroup;

import edu.ufp.inf.sd.project.client.ClientRI;
import edu.ufp.inf.sd.project.client.WorkerRMIRI;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

public interface JobGroupRI extends Remote {

     void attach(WorkerRMIRI w, ClientRI c)throws RemoteException;
     void print()throws RemoteException;
     String JobGroupStr() throws RemoteException;
     void notifyall()throws RemoteException;
     Boolean hasTask() throws RemoteException;
     void update(WorkerRMIRI w, int result)throws RemoteException;
     ClientRI getClient() throws  RemoteException;
     int getId() throws  RemoteException;
     String whoIam() throws  RemoteException;
     void sendCredits(int value, WorkerRMIRI w) throws  RemoteException;
     void createWorkers(int workers,ClientRI clientRI) throws RemoteException;
}
