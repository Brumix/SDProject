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
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientImpl extends UnicastRemoteObject implements ClientRI {

    private final String DATA_PATH = "/home/lenovo/IdeaProjects/SDProject/src/edu/ufp/inf/sd/project/data/";
    private SetupContextRMI contextRMI;
    private JobShopFactoryRI jobShopFactoryRI;
    private int numbWorkers;
    private int totalCredits = 0;
    private HashMap<String, Integer> credits = new HashMap<>();
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


    // todo? the best result is 10% better than the TS
    // todo consumer
    public void playService() {
        try {
            while (true) {
                //================== Remote Job Shop===============
                this.jobShopFactoryRI.print("Remote Job Shop");

                //================== Authentification ===============
                JobShopSessionRI jobShopSessionRI = login();
                jobShopSessionRI.print("Sou o client " + this.user.getName());


                //================== Workers ===============
                System.out.println("How many workers do you want to make available??");
                this.numbWorkers = new Scanner(System.in).nextInt();
                ThreadPool threadPool = new ThreadPool(this.numbWorkers);

                //================== TotalCredits ===============
                System.out.println("How many credits do you want to spend??");
                this.totalCredits = new Scanner(System.in).nextInt();

                //================== Create JobGroup ===============
                while (true) {
                    System.out.println("You now this many credits: " + this.totalCredits);
                    int flag = whatToDo(jobShopSessionRI);
                    if (flag == 1)
                    //==================Distribution of workers ===============
                    {
                        distributionOfWorkers(jobShopSessionRI, this.user.getName(), threadPool, this);
                    } else if (flag == -1)
                        break;

                }
            }

        } catch (RemoteException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    private boolean verifySession(JobShopSessionRI jobShopSessionRI) {
        return jobShopSessionRI != null;
    }

    private void distributionOfWorkers(JobShopSessionRI jobShopSessionRI, String nameU, ThreadPool threadPool, ClientRI clientRI) {
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
                        jobGroup.attach(new WorkerImpl(nameU + i, threadPool, jobGroup), clientRI);
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

    private int whatToDo(JobShopSessionRI jobShopSessionRI) throws RemoteException {
        while (true) {
            System.out.println("#########MENU#######");
            System.out.println("Choose a option");
            System.out.println("Logout ->1");
            System.out.println("Create JobGroups ->2");
            System.out.println("Just Work ->3");
            System.out.println("See my jobs ->4");
            System.out.println("######################");
            int choise = new Scanner(System.in).nextInt();
            switch (choise) {
                case 1:
                    jobShopSessionRI.logout();
                    return -1;
                case 2:
                    createJobGroup(jobShopSessionRI);
                    return 1;
                case 3:
                    return 1;
                case 4:
                    myJobsManager(jobShopSessionRI);
                    return 0;
                default:
                    System.out.println("Incorrect choice,try again");
            }
        }

    }

    private void createJobGroup(JobShopSessionRI jobShopSessionRI) throws RemoteException {

        System.out.println("How many jobGroups do you want to create?");
        int jobs = new Scanner(System.in).nextInt();
        HashMap<Integer, String> files = listJobsFromFolder();
        for (int i = 0; i < jobs; i++) {
            System.out.print("Job " + i + " get what job from the above list: ");
            int idFile = new Scanner(System.in).nextInt();
            if (idFile >= 0 && idFile < files.size()) {
                System.out.println("How many workers do you want to this job?");
                int workers = new Scanner(System.in).nextInt();
                String path = files.get(idFile);
                if (getCredits(path, jobs, workers)) {
                    jobShopSessionRI.createJobGroup(new File(path), workers, this.credits.get(path), this);
                }
            } else {
                System.out.println("ID INVALID");
                i--;
            }
        }
    }

    private HashMap<Integer, String> listJobsFromFolder() {
        //Creating a File object for directory
        File directoryPath = new File(DATA_PATH);
        HashMap<Integer, String> files = new HashMap<>();
        //List of all files and directories
        String[] contents = directoryPath.list();
        System.out.println("List of files and directories in the specified directory:");
        for (int i = 0; i < Objects.requireNonNull(contents).length; i++) {
            files.put(i, DATA_PATH + contents[i]);
            System.out.println("INDEX:" + i + " for the job:" + contents[i]);
        }
        return files;
    }

    @Override
    public void printResult(String path, Integer result) throws RemoteException {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "  The best result was " + result + " from the job " + path);
    }

    @Override
    public void getCredits(int value) throws RemoteException {
        this.totalCredits = this.totalCredits + value;
    }

    private boolean getCredits(String key, int Jobs, int workers) {
        int minCredits = (Jobs * 10) + workers;
        while (true) {
            System.out.println("For the amount of workers that you want for this job you need at minimum " + minCredits + " credits");
            System.out.println("Do you want to pay the exact amount press 1 other amount just say the number.");
            int credits = new Scanner(System.in).nextInt();
            if (credits == 1) {
                return analyzeCredits(key, minCredits);
            }
            if (credits < minCredits) {
                System.out.println("Insufficient credits");
            } else {
                return analyzeCredits(key, credits);

            }
        }
    }

    private boolean analyzeCredits(String key, int credits) {
        while (true) {
            int leftCredits = this.totalCredits - credits - totalCreditsOffAllJG();
            if (leftCredits < 0) {
                System.out.println("You don`t have enough credits,you just have " + this.totalCredits + " you need at least more " + (-leftCredits) + ":(");
                System.out.println("you want to add more credits (0 ->no  1->yes) ?");
                int choice = new Scanner(System.in).nextInt();
                switch (choice) {
                    case 0:
                        return false;
                    case 1:
                        System.out.println("How many credits do you want to add?");
                        int moreCredits = new Scanner(System.in).nextInt();
                        this.totalCredits += moreCredits;
                        break;
                    default:
                        System.out.println("Invalid Choice");
                }
            } else {
                this.credits.put(key, credits);
                return true;
            }
        }
    }

    private int totalCreditsOffAllJG() {
        int total = 0;
        for (int credit : this.credits.values()) {
            total += credit;
        }
        return total;
    }

    private void myJobsManager(JobShopSessionRI jobShopSessionRI) {
        if (listMyJobs(jobShopSessionRI))
            askDeleteWorkers(jobShopSessionRI);

    }

    private void askDeleteWorkers(JobShopSessionRI jobShopSessionRI) {
        System.out.println("Do you want to delete any job?");
        System.out.println("Yes ->1");
        System.out.println("No ->0");
        int choise = new Scanner(System.in).nextInt();
        switch (choise) {
            case 0:
                return;
            case 1:
                deleteWorkers(jobShopSessionRI);
                return;
            default:
                System.out.println("Incorrect choice,try again");
        }
    }

    private boolean listMyJobs(JobShopSessionRI jobShopSessionRI) {
        try {
            ArrayList<String> clientJobs = jobShopSessionRI.getClientJobs(this);
            if (clientJobs.size() < 1) {
                System.out.println(" You don`t have any work active in this moment :) \n");
                return false;
            }
            clientJobs.forEach(System.out::println);
            // for (String job : clientJobs) {
            //     System.out.println(job);
            // }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void deleteWorkers(JobShopSessionRI jobShopSessionRI) {
        try {
            System.out.println("\nWrite the index of the job that you want to delete?");
            int choice = new Scanner(System.in).nextInt();
            jobShopSessionRI.deleteJobGroup(choice);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void sendCredits(int value) throws RemoteException {
        System.out.println("##############" + this.totalCredits);
        this.totalCredits = this.totalCredits - value;
        System.out.println("##############" + this.totalCredits);
    }
}
