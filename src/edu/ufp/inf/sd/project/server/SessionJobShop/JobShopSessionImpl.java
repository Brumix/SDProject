package edu.ufp.inf.sd.project.server.SessionJobShop;

import edu.ufp.inf.sd.project.client.ClientRI;
import edu.ufp.inf.sd.project.client.WorkerRMIRI;
import edu.ufp.inf.sd.project.server.Authentication.Factory.JobShopFactoryImpl;
import edu.ufp.inf.sd.project.server.JobGroup.JobGroupImpl;
import edu.ufp.inf.sd.project.server.JobGroup.JobGroupRI;
import edu.ufp.inf.sd.project.server.Models.User;

import java.io.File;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
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

    /**
     *remove o login do username
     * @throws RemoteException
     */
    @Override
    public void logout() throws RemoteException {
        jobShopFactoryImpl.remove(this.user.getName());
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "[" + user.getName() + "]" + "Logged out Successfully!");
    }

    /**
     * cria um job group para um client
     * @param Jss ficheiro que quer que execute
     * @param workers nr de workers
     * @param credits
     * @param clientRI cliente que os criou
     */
    @Override
    public void createJobGroup(File Jss, int workers, int credits, ClientRI clientRI) {
        try {
            id++;
            JobGroupRI JG = new JobGroupImpl(id, Jss, this, workers, credits, clientRI);
            jobGroups.put(id, JG);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * lista os jobs groups de um  client
     * @return
     * @throws RemoteException
     */
    @Override
    public String listJobGroups() throws RemoteException {
        StringBuilder list = new StringBuilder();
        for (JobGroupRI jobGroup : this.jobGroups.values()) {
            list.append(jobGroup.JobGroupStr());
        }
        return list.toString();
    }

    @Override
    /**
     * obter jobgroup por id
     */
    public JobGroupRI getJobGroup(int id) throws RemoteException {
        return jobGroups.get(id);
    }

    @Override
    /**
     * imprimir todos os jobgroups
     */
    public void printALL() throws RemoteException {
        for (JobGroupRI j : this.jobGroups.values()) {
            j.print();
        }
    }

    @Override
    /**
     * enviar melhor resultado
     */
    public void sendResult(WorkerRMIRI bestWorker, int result) throws RemoteException {
        System.out.println("The best result was " + result);
    }

    @Override
    /**
     * listar jobgroups de um determinado cliente
     */
    public ArrayList<String> getClientJobs(ClientRI client) throws RemoteException {
        ArrayList<String> clientJobs = new ArrayList<>();

        for (JobGroupRI job : jobGroups.values()) {
            if (job.getClient().equals(client)) {
                clientJobs.add(job.whoIam());
            }
        }
        return clientJobs;
    }

    @Override
    /**
     * eliminar jobgroup
     */
    public void deleteJobGroup(int id) throws RemoteException {
        for (JobGroupRI job : jobGroups.values()) {
            int jobId = job.getId();
            if (jobId == id) {
                jobGroups.remove(jobId);
            }
        }
    }
}
