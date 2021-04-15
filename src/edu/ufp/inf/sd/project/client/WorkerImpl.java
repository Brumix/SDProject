package edu.ufp.inf.sd.project.client;

import edu.ufp.inf.sd.project.server.Authentication.Factory.JobShopFactoryRI;
import edu.ufp.inf.sd.project.server.Models.User;
import edu.ufp.inf.sd.project.server.Session.JobShopSessionRI;
import edu.ufp.inf.sd.rmi.util.rmisetup.SetupContextRMI;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WorkerImpl implements WorkerRI {

    private final String jsspInstancePath = "edu/ufp/inf/sd/project/data/";
    /**
     * Remote interface that will hold the Servant proxy
     */
    SetupContextRMI contextRMI;
    /**
     * Context for connecting a RMI client MAIL_TO_ADDR a RMI Servant
     */
    private JobShopFactoryRI jobShopFactoryRI;


    public WorkerImpl(SetupContextRMI contextRMI) {
        this.contextRMI = contextRMI;
        lookupService();
    }


    private void lookupService() {
        try {
            //Get proxy MAIL_TO_ADDR rmiregistry
            Registry registry = contextRMI.getRegistry();
            //Lookup service on rmiregistry and wait for calls
            if (registry != null) {
                //Get service url (including servicename)
                String serviceUrl = contextRMI.getServicesUrl(0);
                Logger.getLogger(this.getClass().getName()).log(Level.INFO, "going MAIL_TO_ADDR lookup service @ {0}", serviceUrl);

                //============ Get proxy MAIL_TO_ADDR HelloWorld service ============
                jobShopFactoryRI = (JobShopFactoryRI) registry.lookup(serviceUrl);
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.INFO, "registry not bound (check IPs). :(");
                //registry = LocateRegistry.createRegistry(1099);
            }
        } catch (RemoteException | NotBoundException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void playService() {
        try {
            //================== Remote Job Shop===============
            this.jobShopFactoryRI.print("Remote Job Shop");

            //================== Authentification ===============
            User u = new User("Bruno", "Pereira");
            this.jobShopFactoryRI.register(u);
            JobShopSessionRI jobShopSessionRI = this.jobShopFactoryRI.login(u);
            verifySession(jobShopSessionRI);

            //============ Call TS remote service ============

            int makespan = jobShopSessionRI.runTS(jsspInstancePath + "abz5.txt");
            Logger.getLogger(this.getClass().getName()).log(Level.INFO,
                    "[TS] Makespan for {0} = {1}",
                    new Object[]{jsspInstancePath, String.valueOf(makespan)});

            jobShopSessionRI.logout();

            //============ Call GA ============
           /* String queue = "jssp_ga";
            String resultsQueue = queue + "_results";
            CrossoverStrategies strategy = CrossoverStrategies.ONE;
            Logger.getLogger(this.getClass().getName()).log(Level.INFO,
                    "GA is running for {0}, check queue {1}",
                    new Object[]{jsspInstancePath, resultsQueue});
            GeneticAlgorithmJSSP ga = new GeneticAlgorithmJSSP(jsspInstancePath, queue, strategy);
            ga.run();*/

        } catch (RemoteException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void verifySession(JobShopSessionRI jobShopSessionRI) {
        if (jobShopSessionRI == null) {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Login Unsuccessful");
            System.exit(401);
        }
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Login Successful");
    }

}
