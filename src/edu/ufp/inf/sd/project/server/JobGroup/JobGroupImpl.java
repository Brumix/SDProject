package edu.ufp.inf.sd.project.server.JobGroup;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import edu.ufp.inf.sd.project.client.ClientRI;
import edu.ufp.inf.sd.project.client.WorkerRMIRI;
import edu.ufp.inf.sd.project.client.WorkerRabbitRI;
import edu.ufp.inf.sd.project.producer.Producer;
import edu.ufp.inf.sd.project.server.Models.ResultGenetic;
import edu.ufp.inf.sd.project.server.SessionJobShop.JobShopSessionRI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JobGroupImpl extends UnicastRemoteObject implements JobGroupRI {

    private final int REWARDWINNER = 11;
    private final int REWARDLOSER = 1;
    private final String FILE_PATH = "/home/lenovo/IdeaProjects/SDProject/src/edu/ufp/inf/sd/project/server/data/";
    private final int id;
    private final JobShopSessionRI jobShopSession;
    private final File JSS;
    private final int credits;
    private int MaxWorkers;
    private ArrayList<WorkerRMIRI> observer = new ArrayList<>();
    private Map<String, ClientRI> RabbitObservers = new HashMap<>();
    private HashMap<WorkerRMIRI, Integer> resultsWokers = new HashMap<>();
    private WorkerRMIRI bestWorker = null;
    private ResultGenetic bestGenetic = null;
    private final ClientRI cliente;
    private HashMap<WorkerRMIRI, ClientRI> OnwersOfTheWorkes = new HashMap<>();
    private Map<String, ResultGenetic> resultGenetics = new HashMap<>();
    private int goal = -1;


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
    public void attach(WorkerRMIRI w, ClientRI c) throws RemoteException {

        this.observer.add(w);
        this.OnwersOfTheWorkes.put(w, c);
        triggerNotifyAll();
    }

    @Override
    public void attach(String id, ClientRI client) throws RemoteException {
        this.RabbitObservers.put(id, client);
        triggerNotifyAll();
    }

    private void triggerNotifyAll() throws RemoteException {
        if (this.observer.size() + this.RabbitObservers.size() == this.MaxWorkers)
            this.notifyall();
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
            for (WorkerRMIRI w : this.observer) {
                w.giveTask(this.JSS);
            }
            for (String id : this.RabbitObservers.keySet()) {
                notiffyGenetic(id);
            }
            this.consumeJobGroup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void notiffyGenetic(String id) {
        try {
            Scanner myReader = new Scanner(this.JSS);
            StringBuilder data = new StringBuilder();
            while (myReader.hasNextLine()) {
                data.append(myReader.nextLine()).append("\n");
            }
            new Producer(id, "JobGroup @" + this.id);
            new Producer(id, " file @ " + data);
            new Producer(id, "start");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Boolean hasTask() throws RemoteException {
        return this.JSS != null;
    }

    public void update(WorkerRMIRI w, int result) throws RemoteException {
        this.resultsWokers.put(w, result);
        if (resultsWokers.size() == observer.size()) {
            int smallers = resultsWokers.get(this.observer.get(0));
            this.bestWorker = this.observer.get(0);
            for (WorkerRMIRI worker : resultsWokers.keySet()) {
                int value = resultsWokers.get(worker);
                if (value < smallers) {
                    this.bestWorker = worker;
                    smallers = value;
                }
            }
            this.goal = Math.min(this.goal, smallers);
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
    public void sendCredits(int value, WorkerRMIRI w) throws RemoteException {
        this.OnwersOfTheWorkes.get(w).getCredits(value);
    }

    @Override
    public void createWorkers(int workers, ClientRI cliente) throws RemoteException {
        cliente.createWorkers(String.valueOf(this.id), workers, this);
    }

    private void sendResult() throws RemoteException {
        this.cliente.sendCredits(this.credits);
        distributeCredits();
        if (this.bestGenetic == null)
            this.cliente.printResult(this.JSS.getPath(), this.resultsWokers.get(this.bestWorker));
        if (this.bestWorker == null)
            this.cliente.printResult(this.JSS.getPath(), this.bestGenetic.getResult());
    }

    private File storeFile(File jss) {
        try {
            Scanner myReader = new Scanner(jss);
            StringBuilder data = new StringBuilder();
            while (myReader.hasNextLine()) {
                data.append(myReader.nextLine()).append("\n");
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
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "[Deleted the file:" + this.JSS.getName() + "]");
            jobShopSession.deleteJobGroup(this.id);
        } else {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "[Failed to delete the file]");
        }

    }

    private void distributeCredits() throws RemoteException {
        if (this.bestWorker != null)
            notiffyWInnerTABU();
        else
            notifyWinnerGEN();
    }

    private void consumeJobGroup() {
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

            String resultsQueue = String.valueOf(this.id);
            System.out.println(resultsQueue);
            channel.queueDeclare(resultsQueue, false, false, false, null);
            //channel.queueDeclare(Producer.QUEUE_NAME, true, false, false, null);
            System.out.println(" [*] Waiting for messages. To exit press CTRL+C");


            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println(" [x] JobGroup '" + message + "'");

                if (message.split(" @")[0].trim().equals("reward"))
                    sendCreditGen(message);
                StategyJobGroup(message);

            };
            channel.basicConsume(resultsQueue, true, deliverCallback, consumerTag -> {
            });

        } catch (Exception e) {
            //Logger.getLogger(Recv.class.getName()).log(Level.INFO, e.toString());
            e.printStackTrace();
        }
    }

    private void sendCreditGen(String message) throws RemoteException {
        String[] info = message.split(" @");
        this.RabbitObservers.get(info[1].trim()).getCredits(Integer.parseInt(info[2].trim()));
    }

    private void StategyJobGroup(String message) throws RemoteException {
        // todo better stategy ??
        String[] value = message.split("@");
        if (value[0].trim().equals("result Worker")) {
            String idWorker = value[1].trim();
            int geneticResult = Integer.parseInt(value[2].trim());
            if (this.goal == -1) {
                this.goal = (int) (geneticResult - (geneticResult * 0.1));
            }

            if (this.RabbitObservers.size() == 1 && this.observer.size() < 1) {
                ResultGenetic current = updateResultGenetic(idWorker, geneticResult);
                if (current.getTrie() >= 4) {
                    stopQueues();
                    this.bestWorker = null;
                    this.bestGenetic = current;
                    sendResult();
                }
            } else {
                ResultGenetic current = updateResultGenetic(idWorker, geneticResult);
                if (current.getResult() <= this.goal) {
                    stopQueues();
                    this.bestGenetic = current;
                    if (this.bestWorker != null) {
                        this.bestWorker = null;
                        sendResult();
                    }
                }
                if (current.getTrie() % 3 == 0 || (current.getResult() - 50) > this.goal) {
                    if (current.getChangestrategie() > 3 || current.getTrie() > 10) {
                        current.close();
                        new Producer(current.getId(), "stop");
                    } else {

                        new Producer(current.getId(), "Strategy @ " + current.nextStrategy().strategy);
                    }
                }
                if (this.bestWorker != null && this.resultsWokers.get(this.bestWorker) <= this.goal) {
                    stopQueues();
                    this.bestGenetic = null;
                    sendResult();
                }
                checkQueuesStuck();
                if (!checkConection() && this.bestWorker != null) {
                    this.bestGenetic = null;
                    sendResult();
                }
            }
        }
    }

    private void stopQueues() {
        for (String id : this.RabbitObservers.keySet()) {
            new Producer(id, "stop");
        }
    }


    private void notifyWinnerGEN() throws RemoteException {
        for (String id : this.RabbitObservers.keySet()) {
            if (id.equals(this.bestGenetic.getId()))
                new Producer(id, "result @ " + REWARDWINNER);
        }

        for (WorkerRMIRI workerRMIRI : this.observer) {
            workerRMIRI.getCredits(REWARDLOSER);
        }
    }

    private void notiffyWInnerTABU() throws RemoteException {
        for (WorkerRMIRI workerRMIRI : this.observer) {
            if (workerRMIRI.equals(this.bestWorker))
                workerRMIRI.getCredits(REWARDWINNER);
            else
                workerRMIRI.getCredits(REWARDLOSER);
        }
        for (String id : this.RabbitObservers.keySet()) {
            new Producer(id, "result @ " + REWARDLOSER);
        }

    }

    private ResultGenetic updateResultGenetic(String id, int result) {

        if (this.resultGenetics.containsKey(id)) {
            this.resultGenetics.get(id).atulizaResult(result);
        } else {
            this.resultGenetics.put(id, new ResultGenetic(id, result));
        }
        return this.resultGenetics.get(id);
    }

    private boolean checkConection() {
        for (ResultGenetic current : this.resultGenetics.values()) {
            if (current.getStatus() == 1)
                return true;
        }
        return false;
    }

    private void checkQueuesStuck() {
        for (ResultGenetic result : this.resultGenetics.values()) {
            String stuck = result.isqueueStuck();
            if (stuck != null) {
                new Producer(result.getId(), "stop");
            }
        }
    }
}


