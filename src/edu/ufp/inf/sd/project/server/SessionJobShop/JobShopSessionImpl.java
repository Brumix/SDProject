package edu.ufp.inf.sd.project.server.Session;

import edu.ufp.inf.sd.project.server.Authentication.Factory.JobShopFactoryImpl;
import edu.ufp.inf.sd.project.server.Models.User;
import edu.ufp.inf.sd.project.util.tabusearch.TabuSearchJSSP;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JobShopSessionImpl extends UnicastRemoteObject implements JobShopSessionRI {

    private final JobShopFactoryImpl jobShopFactoryImpl;
    private final User user;

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



    public int runTS(String jsspInstance) throws RemoteException {

        TabuSearchJSSP ts = new TabuSearchJSSP(jsspInstance);
        int makespan = ts.run();
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "[TS] Makespan for {0} = {1}", new Object[]{jsspInstance, String.valueOf(makespan)});

        return makespan;
    }


}
