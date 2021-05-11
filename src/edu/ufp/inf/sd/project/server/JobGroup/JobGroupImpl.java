package edu.ufp.inf.sd.project.server.JobGroup;

import edu.ufp.inf.sd.project.client.WorkerRI;
import edu.ufp.inf.sd.project.server.SessionJobShop.JobShopSessionRI;

import java.io.File;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;

public class JobGroupImpl extends UnicastRemoteObject implements JobGroupRI {

    private final int id;
    private final JobShopSessionRI jobShopSession;
    private final File JSS;
    private int MaxWorkers;
    private ArrayList<WorkerRI> workers = new ArrayList<>();
    private HashMap<WorkerRI, Integer> resultsWokers = new HashMap<>();
    private WorkerRI bestWorker;

    public JobGroupImpl(int id, File JSS,JobShopSessionRI jobShopSession,int workers) throws RemoteException {
        super();
        this.id = id;
        this.JSS = JSS;
        this.jobShopSession= jobShopSession;
        this.MaxWorkers = workers;
    }

    @Override
    /**
     * simular a agregacao de um worker a um Jobgroup
     */

    public void addWorker(WorkerRI w) {
        try {
            //todo check limit of workers
            this.workers.add(w);
            if (this.workers.size() == this.MaxWorkers)
                this.execute();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void print() {
        System.out.println((long) workers.size());
    }

    @Override
    public String JobGroupStr() throws RemoteException {
        return "JobGroup :" + this.id + " with the work " + this.JSS + "\n";
    }

    @Override
    /**
     * Seguimento do DP obeserver (notify all)
     * Por os workers a trabalharem
     */

    public void execute() {
        try {
            for (WorkerRI w : this.workers) {
                w.giveTask(this.JSS);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Boolean hasTask() throws RemoteException {
        return this.JSS != null;
    }


    public void getResultFromWorker(WorkerRI w, int result)throws RemoteException {
        resultsWokers.put(w, result);
        if (resultsWokers.size() == workers.size()) {
            int major = resultsWokers.get(this.workers.get(0));
            for (WorkerRI worker : resultsWokers.keySet()) {
                int value=resultsWokers.get(worker);
                if (value < major) {
                    this.bestWorker = worker;
                    major =value;
                }
            }
            sendResult();
        }
    }

    private void sendResult()throws RemoteException{
        this.jobShopSession.sendResult(this.bestWorker,this.resultsWokers.get(this.bestWorker));
    }


}
