package edu.ufp.inf.sd.project.client;


import edu.ufp.inf.sd.project.consumer.Consumer;
import edu.ufp.inf.sd.project.server.JobGroup.JobGroupRI;
import edu.ufp.inf.sd.project.util.geneticalgorithm.CrossoverStrategies;
import edu.ufp.inf.sd.project.util.geneticalgorithm.GeneticAlgorithmJSSP;
import edu.ufp.inf.sd.project.util.tabusearch.TabuSearchJSSP;
import edu.ufp.inf.sd.rmi.util.threading.ThreadPool;

import java.io.File;
import java.io.FileWriter;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WorkerImpl extends UnicastRemoteObject implements WorkerRI, Runnable {

    private File file;
    private final String id;
    private String path;
    private final ThreadPool thread;
    private final JobGroupRI jobGroup;

    public WorkerImpl(String id, ThreadPool thread, JobGroupRI jobGroupRI) throws RemoteException {
        this.id = id;
        this.thread = thread;
        this.jobGroup = jobGroupRI;
    }

    public void runAlgorthim() throws RemoteException {
        this.thread.execute(this::runGN);

    }

    public void print(String msg) throws RemoteException {
        System.out.println(msg);
    }

    public String whoIAm() throws RemoteException {
        return this.id + "com o trabalho" + this.path;
    }

    @Override
    public void giveTask(File file) throws RemoteException {
        this.file = file;
        readFile();
        runAlgorthim();
    }

    private void readFile() throws RemoteException {
        try {
            Scanner myReader = new Scanner(this.file);
            String namefile = new File(" ").getAbsolutePath().trim() + this.id + ".txt";
            this.path = namefile;
            FileWriter myfile = new FileWriter(namefile);
            StringBuilder data = new StringBuilder();
            while (myReader.hasNextLine()) {
                data.append(myReader.nextLine() + "\n");
            }
            myfile.write(data.toString());
            myfile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void notify(Integer resultTS) throws RemoteException {
        this.jobGroup.update(this, resultTS);
    }

    public void runTS() {
        TabuSearchJSSP ts = new TabuSearchJSSP(this.path);
        ReentrantLock lock = new ReentrantLock();
        int makespan = ts.run();
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "[TS] Makespan for {0} = {1}", new Object[]{this.path, String.valueOf(makespan)});
        try {
            lock.lock();
            notify(makespan);
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
            cleanUp();
        }
    }

    private void cleanUp() {
        new File(this.path).delete();
    }

    private void runGN() {
        try {
            String resultsQueue = this.whoIAm() + "_results";
            CrossoverStrategies strategy = CrossoverStrategies.ONE;
            Logger.getLogger(this.getClass().getName()).log(Level.INFO,
                    "GA is running for {0}, check queue {1}",
                    new Object[]{this.path, resultsQueue});

            Consumer consumer = new Consumer(this.whoIAm());
            consumer.consume();


            GeneticAlgorithmJSSP ga = new GeneticAlgorithmJSSP(this.path, this.whoIAm(), strategy);
            ga.run();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {

    }

}
