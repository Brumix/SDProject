package edu.ufp.inf.sd.project.server.SessionJobShop;

import edu.ufp.inf.sd.project.client.ClientRI;
import edu.ufp.inf.sd.project.client.WorkerRMIRI;
import edu.ufp.inf.sd.project.server.JobGroup.JobGroupRI;

import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface JobShopSessionRI extends Remote {
    void print(String msg) throws RemoteException;

    void logout() throws RemoteException;

    void createJobGroup(File Jss, int workers, int credits, ClientRI clientRI) throws RemoteException;

    String listJobGroups() throws RemoteException;

    JobGroupRI getJobGroup(int id) throws RemoteException;

    void printALL() throws RemoteException;

    void sendResult(WorkerRMIRI bestWorker, int integer) throws RemoteException;

    ArrayList<String> getClientJobs(ClientRI client) throws RemoteException;

    void deleteJobGroup(int id) throws RemoteException;


}
