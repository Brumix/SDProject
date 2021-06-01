package edu.ufp.inf.sd.project.client;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import edu.ufp.inf.sd.project.producer.Producer;
import edu.ufp.inf.sd.project.server.Authentication.Factory.JobShopFactoryRI;
import edu.ufp.inf.sd.project.server.JobGroup.JobGroupRI;
import edu.ufp.inf.sd.project.server.Models.User;
import edu.ufp.inf.sd.project.server.SessionJobShop.JobShopSessionRI;
import edu.ufp.inf.sd.rmi.util.rmisetup.SetupContextRMI;
import edu.ufp.inf.sd.rmi.util.threading.ThreadPool;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Scanner;
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

    /**
     * Menu Inicial Em ciclo infinito
     *
     * @throws RemoteException
     */
    public void playService() throws RemoteException {
        boolean stateConection = false;

        while (true) {

            //================== Remote Job Shop===============
            this.jobShopFactoryRI.print("Remote Job Shop");

            //================== Authentification ===============
            JobShopSessionRI jobShopSessionRI = login();
            jobShopSessionRI.print("Sou o client " + this.user.getName());


            //================== Workers ===============
            System.out.println("How many workers do you want to make available??");
            this.numbWorkers = new Scanner(System.in).nextInt();


            //================== TotalCredits ===============
            System.out.println("How many credits do you want to spend??");
            this.totalCredits = new Scanner(System.in).nextInt();

            //================== Create JobGroup ===============
            while (true) {
                System.out.println("You now have this many credits: " + this.totalCredits);
                int flag = whatToDo(jobShopSessionRI);
                if (flag == 1)
                //==================Distribution of workers ===============
                {
                    if (!stateConection) {
                        InitConnection();
                        stateConection = true;
                    }

                    distributionOfWorkers(jobShopSessionRI);
                } else if (flag == -1)
                    break;

            }
        }

    }

    /**
     * cria coneccao com a queue
     */
    private void InitConnection() {
        {
            try {
                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost("localhost");
                //Use same username/passwd as the for accessing Management UI @ http://localhost:15672/
                //Default credentials are: guest/guest (change accordingly)
                factory.setUsername("guest");
                factory.setPassword("guest");
                //factory.setPassword("guest4rabbitmq");
                Connection connection = factory.newConnection();
                Channel channel = connection.createChannel();

                String resultsQueue = String.valueOf(this.user.getName());
                System.out.println(resultsQueue);
                channel.queueDeclare(resultsQueue, false, false, false, null);
                //channel.queueDeclare(Producer.QUEUE_NAME, true, false, false, null);
                System.out.println(" [*] Waiting for messages. To exit press CTRL+C");


                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    System.out.println(" [x] CLIENT '" + message + "'");
                    String[] info = message.split("@");

                    switch (info[0].trim()) {
                        case "winner":
                            this.printResult(info[1].trim(), Integer.parseInt(info[2].trim()));
                            break;
                        case "GetCredits":
                            this.getCredits(Integer.parseInt(info[1].trim()));
                            break;
                    }


                };
                channel.basicConsume(resultsQueue, true, deliverCallback, consumerTag -> {
                });

            } catch (Exception e) {
                //Logger.getLogger(Recv.class.getName()).log(Level.INFO, e.toString());
                e.printStackTrace();
            }
        }
    }

    /**
     * Verificar se a sessão é valida
     *
     * @param jobShopSessionRI
     * @return
     */
    private boolean verifySession(JobShopSessionRI jobShopSessionRI) {
        return jobShopSessionRI != null;
    }

    /**
     * Gere a distribuição de Workers pelos JobGroups
     *
     * @param jobShopSessionRI
     */
    private void distributionOfWorkers(JobShopSessionRI jobShopSessionRI) {
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
                    jobGroup.createWorkers(workers, this);
                    freeWorkers -= workers;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Menu de autenticação ou registo
     *
     * @return
     * @throws RemoteException
     */
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

    /**
     * Requisita a introdução das credenciais para login
     *
     * @return JobShopSessionRI caso o cliente se encontrar registado
     * @throws RemoteException
     */
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

    /**
     * Registo de clientes
     *
     * @throws RemoteException
     */
    private void register() throws RemoteException {
        System.out.println("Enter a username");
        String name = new Scanner(System.in).next();
        System.out.println("Enter a password");
        String pass = new Scanner(System.in).next();
        this.jobShopFactoryRI.register(new User(name, pass));
    }

    /**
     * Menu de distribuição de workers e gestão de jobgroups
     *
     * @param jobShopSessionRI
     * @return
     * @throws RemoteException
     */
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

    /**
     * Cria jobgroup para resolver um determinado ficheiro
     *
     * @param jobShopSessionRI
     * @throws RemoteException
     */
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

    /**
     * listar todos os ficheiros de uma pasta
     *
     * @return lista de ficheiros
     */
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
    /**
     * Imprime o melhor resultado de um determinado jobgroup
     */
    public void printResult(String path, Integer result) throws RemoteException {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "  The best result was " + result + " from the job " + path);
    }

    @Override
    /**
     * Adicionar creditos a um cliente
     */
    public void getCredits(int value) throws RemoteException {
        this.totalCredits = this.totalCredits + value;
    }

    /**
     * pede o valor minimo de creditos necessarios atraves do nr de workers que se encontram no jobgroup
     *
     * @param key     id do job group
     * @param Jobs    numero de jobs groups
     * @param workers numero de workers em todos os  jobgroup
     * @return
     */
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

    /**
     * Analise se o cliente possui creditos suficientes para um determinado jobgroup
     *
     * @param key     id do job group
     * @param credits creditos minimos para jobgroup
     * @return
     */
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

    /**
     * Calcula o numero de creditos de um jobgroup
     *
     * @return
     */
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

    /**
     * Lista todos os job groups de um client
     *
     * @param jobShopSessionRI
     * @return
     */
    private boolean listMyJobs(JobShopSessionRI jobShopSessionRI) {
        try {
            ArrayList<String> clientJobs = jobShopSessionRI.getClientJobs(this);
            if (clientJobs.size() < 1) {
                System.out.println(" You don`t have any work active in this moment :) \n");
                return false;
            }
            clientJobs.forEach(System.out::println);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Eliminar worker de um jobgroup
     *
     * @param jobShopSessionRI
     */
    private void deleteWorkers(JobShopSessionRI jobShopSessionRI) {
        try {
            System.out.println("\nWrite the index of the job that you want to delete?");
            int choice = new Scanner(System.in).nextInt();
            jobShopSessionRI.deleteJobGroup(choice);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    /**
     * retira  os creditos do client
     *
     * @param value nr de creditos a subtrair
     * @throws RemoteException
     */
    @Override
    public void sendCredits(int value) throws RemoteException {
        this.totalCredits = this.totalCredits - value;
    }

    public void createWorkers(String idQueue, int totalWorkers, JobGroupRI jobGroup) throws RemoteException {
        int RmiWorkers = (int) Math.floor((double) totalWorkers / 2);
        int RabbitWorkers = totalWorkers - RmiWorkers;
        ThreadPool threadPool = new ThreadPool(RmiWorkers);
        for (int i = 0; i < RabbitWorkers; i++) {
            WorkerRabbitRI rabbit = new WorkerRabbitImpl(jobGroup.getId() + jobGroup.getClient().getName());
            String rabbitId = rabbit.getPersonalId();
            new Thread(rabbit).start();
            new Producer(String.valueOf(jobGroup.getId()), "client @ " + this.user.getName() + " @  " + rabbitId);
        }
        for (int i = 0; i < RmiWorkers; i++) {
            jobGroup.attach(new WorkerRMIImpl(this.user.getName() + i, threadPool, jobGroup), this);
        }
    }


    @Override
    public String getName() throws RemoteException {
        return user.getName();
    }
}
