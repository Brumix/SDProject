package edu.ufp.inf.sd.project.server.SessionJobShop;

import edu.ufp.inf.sd.project.client.ClientRI;
import edu.ufp.inf.sd.project.client.WorkerRI;
import edu.ufp.inf.sd.project.server.Authentication.Factory.JobShopFactoryImpl;
import edu.ufp.inf.sd.project.server.JobGroup.JobGroupImpl;
import edu.ufp.inf.sd.project.server.JobGroup.JobGroupRI;
import edu.ufp.inf.sd.project.server.Models.User;

import java.io.File;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JobShopSessionImpl extends UnicastRemoteObject implements JobShopSessionRI {

    private final JobShopFactoryImpl jobShopFactoryImpl;
    private final User user;
    private static final HashMap<Integer, JobGroupRI> jobGroups = new HashMap<>();
    private static Integer id = 0;


    public JobShopSessionImpl(JobShopFactoryImpl jobShopFactory, User user) throws RemoteException {
        this.jobShopFactoryImpl = jobShopFactory;
        this.user = user;
    }

    @Override
    public void print(String msg) throws RemoteException {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "[Session] = {0}", new Object[]{msg});
    }

    @Override
    public void logout() throws RemoteException {
        jobShopFactoryImpl.remove(this.user.getName());
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "[" + user.getName() + "]" + "Logged out Successfully!");
    }

    @Override
    public void createJobGroup(File Jss, int workers, int credits, ClientRI clientRI) {
        try {
            id++;
            JobGroupRI JG = new JobGroupImpl(id, Jss, this, workers, credits,clientRI);
            jobGroups.put(id, JG);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String listJobGroups() throws RemoteException {
        StringBuilder list = new StringBuilder();
        for (JobGroupRI jobGroup : this.jobGroups.values()) {
            list.append(jobGroup.JobGroupStr());
        }
        return list.toString();
    }

    @Override
    public JobGroupRI getJobGroup(int id) throws RemoteException {
        return jobGroups.get(id);
    }

    @Override
    public void printALL() throws RemoteException {
        for (JobGroupRI j : this.jobGroups.values()) {
            j.print();
        }
    }

    @Override
    public void sendResult(WorkerRI bestWorker, int result) throws RemoteException {
        System.out.println("The best result was " + result);
    }
}
