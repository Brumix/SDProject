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
    private ArrayList<WorkerRI> observer = new ArrayList<>();
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
   public void attach(WorkerRI w) {
        try {
            //todo check limit of workers
            this.observer.add(w);
            if (this.observer.size() == this.MaxWorkers)
                this.notifyall();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void print() {
        System.out.println((long) observer.size());
    }

    @Override
    public String JobGroupStr() throws RemoteException {
        return "JobGroup :" + this.id + " with the work " + this.JSS + "\n";
    }

    @Override
    public void notifyall() {
        try {
            for (WorkerRI w : this.observer) {
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

    public void update(WorkerRI w, int result)throws RemoteException {
        resultsWokers.put(w, result);
        if (resultsWokers.size() == observer.size()) {
            int smallers = resultsWokers.get(this.observer.get(0));
            for (WorkerRI worker : resultsWokers.keySet()) {
                int value=resultsWokers.get(worker);
                if (value < smallers) {
                    this.bestWorker = worker;
                    smallers =value;
                }
            }
            sendResult();
        }
    }

    private void sendResult()throws RemoteException{
        this.jobShopSession.sendResult(this.bestWorker,this.resultsWokers.get(this.bestWorker));
    }


}
