package edu.ufp.inf.sd.project.client;

import edu.ufp.inf.sd.project.server.Authentication.Factory.JobShopFactoryRI;
import edu.ufp.inf.sd.project.server.JobGroup.JobGroupRI;
import edu.ufp.inf.sd.project.server.Models.User;
import edu.ufp.inf.sd.project.server.SessionJobShop.JobShopSessionRI;
import edu.ufp.inf.sd.rmi.util.rmisetup.SetupContextRMI;
import edu.ufp.inf.sd.rmi.util.threading.ThreadPool;

import java.io.File;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Objects;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientImpl extends UnicastRemoteObject implements ClientRI {

    private SetupContextRMI contextRMI;
    private JobShopFactoryRI jobShopFactoryRI;
    private int numbWorkers;
    private int totalCredits = 0;
    private User user;


    public ClientImpl(SetupContextRMI contextRMI) throws RemoteException {
        super();
        this.contextRMI = contextRMI;
        this.lookupService();
        this.playService();
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
                this.jobShopFactoryRI = (JobShopFactoryRI) registry.lookup(serviceUrl);
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
            // todo delete
            // todo preserve workers when i give to most workers for a job
            // todo see function above
            //================== Remote Job Shop===============
            this.jobShopFactoryRI.print("Remote Job Shop");
            //================== Authentification ===============
            JobShopSessionRI jobShopSessionRI = login();
            jobShopSessionRI.print("Sou o client " + this.user.getName());


            //================== Workers ===============
            System.out.println("How many workers do you want to make available??");
            this.numbWorkers = new Scanner(System.in).nextInt();
            ThreadPool threadPool = new ThreadPool(this.numbWorkers);

            //================== Create JobGroup ===============
            whatToDo(jobShopSessionRI);

            //==================Distribution of workers ===============
            distributionOfWorkers(jobShopSessionRI, this.user.getName(), threadPool);



            //  jobShopSessionRI.logout();
            //============ Call TS remote service ============
            /*
            int makespan = jobShopSessionRI.runTS(jsspInstancePath);
            Logger.getLogger(this.getClass().getName()).log(Level.INFO,
                    "[TS] Makespan for {0} = {1}",
                    new Object[]{jsspInstancePath, String.valueOf(makespan)});
            //============ Call GA ============
            String queue = "jssp_ga";
            String resultsQueue = queue + "_results";
            CrossoverStrategies strategy = CrossoverStrategies.ONE;
            Logger.getLogger(this.getClass().getName()).log(Level.INFO,
                    "GA is running for {0}, check queue {1}",
                    new Object[]{jsspInstancePath, resultsQueue});
            GeneticAlgorithmJSSP ga = new GeneticAlgorithmJSSP(jsspInstancePath, queue, strategy);
            ga.run();
            */
        } catch (RemoteException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    private boolean verifySession(JobShopSessionRI jobShopSessionRI) {
        return jobShopSessionRI != null;
    }

    private void distributionOfWorkers(JobShopSessionRI jobShopSessionRI, String nameU, ThreadPool threadPool) {
        int freeWorkers = this.numbWorkers;
        try {
            while (freeWorkers > 0) {
                System.out.println("You have " + freeWorkers + " workers free, give them work");
                System.out.println(jobShopSessionRI.listJobGroups());
                System.out.println("how many workers do you want to select if you want let the rest free press 0: ");
                int workers = new Scanner(System.in).nextInt();
                if (workers == 0)
                    return;
                if (workers > freeWorkers) {
                    System.out.println("You have selected to many workers :(");
                    continue;
                }
                System.out.println("For what JobGroup");
                int jgId = new Scanner(System.in).nextInt();
                JobGroupRI jobGroup = jobShopSessionRI.getJobGroup(jgId);
                if (jobGroup != null) {
                    for (int i = 0; i < workers; i++) {
                        jobGroup.addWorker(new WorkerImpl(nameU + i, threadPool, jobGroup));
                    }
                    freeWorkers -= workers;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JobShopSessionRI login() throws RemoteException {
        while (true) {
            System.out.println("#########MENU#######");
            System.out.println("Choose one option:");
            System.out.println("Login-> 1");
            System.out.println("Register-> 2");
            System.out.println("Exit -> 0");
            System.out.println("######################");
            int choise = new Scanner(System.in).nextInt();
            switch (choise) {
                case 1:
                    return autentication();
                case 2:
                    register();
                    break;
                case 0:
                    System.exit(200);
                default:
                    System.out.println("Incorrect choice,try again");
            }
        }
    }

    private JobShopSessionRI autentication() throws RemoteException {
        System.out.println("Enter your username");
        String name = new Scanner(System.in).next();
        System.out.println("Enter your password");
        String pass = new Scanner(System.in).next();
        User u = new User(name, pass);
        JobShopSessionRI jobShopSessionRI = this.jobShopFactoryRI.login(u);
        if (verifySession(jobShopSessionRI)) {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "LOGIN SUCCESSFUL :)");
            this.user = u;
            return jobShopSessionRI;
        }
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "LOGIN UNSUCCESSFUL :(");
        System.exit(401);
        return null;
    }

    private void register() throws RemoteException {
        System.out.println("Enter a username");
        String name = new Scanner(System.in).next();
        System.out.println("Enter a password");
        String pass = new Scanner(System.in).next();
        this.jobShopFactoryRI.register(new User(name, pass));
    }

    private void whatToDo(JobShopSessionRI jobShopSessionRI) throws RemoteException {
        System.out.println("#########MENU#######");
        System.out.println("Choose a option");
        System.out.println("Logout ->1");
        System.out.println("Create JobGroups ->2");
        System.out.println("Just Work ->3");
        System.out.println("######################");
        int choise = new Scanner(System.in).nextInt();
        switch (choise) {
            case 1:
                jobShopSessionRI.logout();
                System.exit(200);
            case 2:
                createJobGroup(jobShopSessionRI);
                return;
            case 3:
                return;
            default:
                System.out.println("Incorrect choice,try again");
        }
    }

    private void createJobGroup(JobShopSessionRI jobShopSessionRI) throws RemoteException {
        int workersJob = 0;
        System.out.println("How many jobGroups do you want to create?");
        int jobs = new Scanner(System.in).nextInt();
        HashMap<Integer, String> files = listJobsFromFolder();
        for (int i = 0; i < jobs; i++) {
            System.out.print("Job " + i + " get what job from the above list: ");
            int idFile = new Scanner(System.in).nextInt();
            if (idFile >= 0 && idFile < files.size()) {
                System.out.println("How many workers do you want to this job?");
                int workers = new Scanner(System.in).nextInt();
                workersJob += workers;
                jobShopSessionRI.createJobGroup(new File(files.get(idFile)), workers);
            } else {
                System.out.println("ID INVALID");
                i--;
            }
        }
        getCredits(jobs, workersJob);
    }

    private HashMap<Integer, String> listJobsFromFolder() {
        //todo how to fix absolute path
        //Creating a File object for directory
        File directoryPath = new File("/home/hp/IdeaProjects/SDProject/src/edu/ufp/inf/sd/project/data");
        HashMap<Integer, String> files = new HashMap<>();
        //List of all files and directories
        String[] contents = directoryPath.list();
        System.out.println("List of files and directories in the specified directory:");
        for (int i = 0; i < Objects.requireNonNull(contents).length; i++) {
            files.put(i, "/home/hp/IdeaProjects/SDProject/src/edu/ufp/inf/sd/project/data/" + contents[i]);
            System.out.println("INDEX:" + i + " for the job:" + contents[i]);
        }
        return files;
    }

    @Override
    //todo isn`t working, who to made the server send to the client the result
    public void printResult(WorkerRI w, Integer result) throws RemoteException {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, w.whoIAm() + " com o resultado de " + result);
    }

    private void getCredits(int Jobs, int workers) {
        int minCredits = (Jobs * 10) + workers;
        while (true) {
            System.out.println("For the amount of Jobs and workers that you want you need at minimum " + minCredits + " credits");
            System.out.println("Do you want to pay the exact amount press 1 other amount just say the number.");
            int credits = new Scanner(System.in).nextInt();
            if (credits == 1) {
                this.totalCredits = minCredits;
                break;
            }
            if (credits < minCredits) {
                System.out.println("Creditos Insuficientes");
            } else {
                this.totalCredits = credits;
                break;
            }
        }
    }


}
