package edu.ufp.inf.sd.project.server.JobGroup;

import com.rabbitmq.client.Channel;
import edu.ufp.inf.sd.project.client.ClientRI;
import edu.ufp.inf.sd.project.client.WorkerRI;
import edu.ufp.inf.sd.project.consumer.Consumer;
import edu.ufp.inf.sd.project.producer.Producer;
import edu.ufp.inf.sd.project.server.SessionJobShop.JobShopSessionRI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JobGroupImpl extends UnicastRemoteObject implements JobGroupRI {

    private final String FILE_PATH = "/home/lenovo/IdeaProjects/SDProject/src/edu/ufp/inf/sd/project/server/data/";
    private final int id;
    private final JobShopSessionRI jobShopSession;
    private final File JSS;
    private final int credits;
    private int MaxWorkers;
    private ArrayList<WorkerRI> observer = new ArrayList<>();
    private HashMap<WorkerRI, Integer> resultsWokers = new HashMap<>();
    private WorkerRI bestWorker;
    private final ClientRI cliente;
    private HashMap<WorkerRI, ClientRI> OnwersOfTheWorkes = new HashMap<>();


    public JobGroupImpl(int id, File JSS, JobShopSessionRI jobShopSession, int workers, int credits, ClientRI cliente) throws RemoteException {
        super();
        this.id = id;
        this.JSS = storeFile(JSS);
        this.jobShopSession = jobShopSession;
        this.MaxWorkers = workers;
        this.credits = credits;
        this.cliente = cliente;
    }

    @Override
    public void attach(WorkerRI w, ClientRI c) {
        try {
            this.observer.add(w);
            this.OnwersOfTheWorkes.put(w, c);
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

    public void update(WorkerRI w, int result) throws RemoteException {
        this.resultsWokers.put(w, result);
        if (resultsWokers.size() == observer.size()) {
            int smallers = resultsWokers.get(this.observer.get(0));
            this.bestWorker = this.observer.get(0);
            for (WorkerRI worker : resultsWokers.keySet()) {
                int value = resultsWokers.get(worker);
                if (value < smallers) {
                    this.bestWorker = worker;
                    smallers = value;
                }
            }
            sendResult();
            cleanUp();
        }
    }

    @Override
    public ClientRI getClient() throws RemoteException {
        return this.cliente;
    }

    @Override
    public int getId() throws RemoteException {
        return this.id;
    }

    @Override
    public String whoIam() throws RemoteException {
        StringBuilder builder = new StringBuilder();
        builder.append(this.id);
        builder.append(" com o job ");
        builder.append(this.JSS.getPath());
        return builder.toString();
    }

    @Override
    public void sendCredits(int value, WorkerRI w) throws RemoteException {
        this.OnwersOfTheWorkes.get(w).getCredits(value);
    }

    private void sendResult() throws RemoteException {
        this.cliente.sendCredits(this.credits);
        distributeCredits();
        this.cliente.printResult(this.JSS.getPath(), this.resultsWokers.get(this.bestWorker));
    }


    private File storeFile(File jss) {
        try {
            Scanner myReader = new Scanner(jss);
            StringBuilder data = new StringBuilder();
            while (myReader.hasNextLine()) {
                data.append(myReader.nextLine() + "\n");
            }
            File file = new File(FILE_PATH + jss.getName());
            FileWriter fr = new FileWriter(file);
            fr.write(data.toString());
            fr.close();

            return file;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void cleanUp() throws RemoteException {
        if (this.JSS.delete()) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "[Deleted the file:" + this.JSS.getName() + "]");
            jobShopSession.deleteJobGroup(this.id);
        } else {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "[Failed to delete the file]");
        }

    }

    private void distributeCredits() throws RemoteException {
        int winner = 11;
        int loser = 1;
        for (WorkerRI workerRI : this.observer) {
            if (workerRI.equals(this.bestWorker))
                workerRI.getCredits(winner);
            else
                workerRI.getCredits(loser);
        }
    }


}


