package edu.ufp.inf.sd.project.client;

import edu.ufp.inf.sd.project.server.JobGroup.JobGroupRI;
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

    public void runTS() throws RemoteException {
        this.thread.execute(this);
    }

    public void print(String msg) throws RemoteException {
        System.out.println(msg);
    }

    @Override
    public void giveTask(File file) throws RemoteException {
        this.file = file;
        readFile();
        runTS();
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

    private void sendResult(Integer resultTS) throws RemoteException{
        this.jobGroup.getResultFromWorker(this, resultTS);
    }

    @Override
    public void run() {
        TabuSearchJSSP ts = new TabuSearchJSSP(this.path);
        ReentrantLock lock = new ReentrantLock();
        int makespan = ts.run();
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "[TS] Makespan for {0} = {1}", new Object[]{this.path, String.valueOf(makespan)});
        try {
            lock.lock();
            sendResult(makespan);
            //lock.unlock();
        }catch (RemoteException e){
            e.printStackTrace();
        }finally {
            lock.unlock();
        }

    }
}
