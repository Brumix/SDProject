package edu.ufp.inf.sd.project.server.JobGroup;

import edu.ufp.inf.sd.project.client.WorkerRI;

import java.io.File;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class JobGroupImpl extends UnicastRemoteObject implements JobGroupRI {

    private final int id;
    private final File JSS;
    private ArrayList<WorkerRI> workers = new ArrayList<>();

    public JobGroupImpl(int id, File JSS) throws RemoteException {
        super();
        this.id = id;
        this.JSS = JSS;
    }

    @Override
    /**
     * simular a agregacao de um worker a um Jobgroup
     */
    // TODO addWorker
    public void addWorker(WorkerRI w) {
        try {
            this.workers.add(w);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override // "JobGroup :" + this.id + " with the work " + this.JSS
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
    //TODO execute
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

}
