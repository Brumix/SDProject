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
    private Map<String, String> RabbitObservers = new HashMap<>();
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
        this.consumeJobGroup();
    }

    @Override
    /**
     * Associa Worker a um jobgroup
     */
    public void attach(WorkerRMIRI w, ClientRI c) throws RemoteException {

        this.observer.add(w);
        this.OnwersOfTheWorkes.put(w, c);
        triggerNotifyAll();
    }

    /**
     * @param id
     * @param client
     * @throws RemoteException
     */
    public void attach(String id, String client) throws RemoteException {
        this.RabbitObservers.put(id, client);
        triggerNotifyAll();
    }

    /**
     * Notifica todos os workers de um jobgroup
     *
     * @throws RemoteException
     */
    private void triggerNotifyAll() throws RemoteException {
        if (this.observer.size() + this.RabbitObservers.size() == this.MaxWorkers)
            this.notifyall();
    }

    @Override
    public void print() {
        System.out.println((long) observer.size());
    }

    @Override
    /**
     * Imprime o id do jobgroup e o caminho para o ficheiro
     */
    public String JobGroupStr() throws RemoteException {
        return "JobGroup :" + this.id + " with the work " + this.JSS + "\n";
    }

    /**
     * da um trabalho e notifica o rabbit a cada worker associado com cada padrao
     */
    @Override
    public void notifyall() {
        try {
            for (WorkerRMIRI w : this.observer) {
                w.giveTask(this.JSS);
            }
            for (String id : this.RabbitObservers.keySet()) {
                notiffyGenetic(id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * notifica o produtor  para começar a trabalhar
     *
     * @param id
     */
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
    /**
     * verifica se o jobgroup possui algum trabalho
     */
    public Boolean hasTask() throws RemoteException {
        return this.JSS != null;
    }

    /**
     * verifica qual o melhor resultado
     *
     * @param result
     * @throws RemoteException
     */
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
            //modificado
            if (this.goal == -1)
                this.goal = smallers;
            else
                this.goal = Math.min(this.goal, smallers);
            cleanUp();
        }
        if (this.bestGenetic != null) {
            if (this.bestGenetic.getResult() < this.resultsWokers.get(this.bestWorker)) {
                this.bestWorker = null;
            } else {
                this.bestGenetic = null;
            }
            sendResult();
        }
    }

    /**
     * retorna  o cliente
     *
     * @return
     * @throws RemoteException
     */
    @Override
    public ClientRI getClient() throws RemoteException {
        return this.cliente;
    }

    @Override
    public int getId() throws RemoteException {
        return this.id;
    }

    /**
     * serve para identificar o job group
     *
     * @return
     * @throws RemoteException
     */
    @Override
    public String whoIam() throws RemoteException {
        StringBuilder builder = new StringBuilder();
        builder.append(this.id);
        builder.append(" com o job ");
        builder.append(this.JSS.getPath());
        return builder.toString();
    }

    /**
     * envia para o client os seus respetivos creditos por cada worker
     *
     * @param value
     * @param w
     * @throws RemoteException
     */
    @Override
    public void sendCredits(int value, WorkerRMIRI w) throws RemoteException {
        this.OnwersOfTheWorkes.get(w).getCredits(value);
    }

    /**
     * diz ao cliente que tem q iniciar x workers
     *
     * @param workers
     * @param cliente
     * @throws RemoteException
     */
    @Override
    public void createWorkers(int workers, ClientRI cliente) throws RemoteException {
        cliente.createWorkers(String.valueOf(this.id), workers, this);
    }

    /**
     * manda o resultado vencedor para o cliente
     *
     * @throws RemoteException
     */
    private void sendResult() throws RemoteException {
        this.cliente.sendCredits(this.credits);
        distributeCredits();
        if (this.bestGenetic == null)
            this.cliente.printResult(this.JSS.getPath(), this.resultsWokers.get(this.bestWorker));
        if (this.bestWorker == null)
            new Producer(this.RabbitObservers.get(this.bestGenetic.getId()), "winner @ " + this.JSS.getPath() + " @ " + this.bestGenetic.getResult());
    }

    /**
     * Guarda ficheiro do lado do servidor
     *
     * @param jss
     * @return
     */
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

    /**
     * Elimina o jobgroup e a respetiva sessao
     *
     * @throws RemoteException
     */
    private void cleanUp() throws RemoteException {
        if (this.JSS.delete()) {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "[Deleted the file:" + this.JSS.getName() + "]");
            jobShopSession.deleteJobGroup(this.id);
        } else {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "[Failed to delete the file]");
        }

    }

    /**
     * atribuir creditos de compensação aos workers
     *
     * @throws RemoteException
     */
    private void distributeCredits() throws RemoteException {
        if (this.bestWorker != null)
            notiffyWInnerTABU();
        else
            notifyWinnerGEN();
    }

    /**
     * Cria conecao com a queue
     */
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

                if (message.split(" @")[0].trim().equals("client")) {
                    String[] info = message.split("@");
                    System.out.println(Arrays.toString(info));

                    this.attach(info[2].trim(), info[1].trim());
                }

                if (message.split(" @")[0].trim().equals("reward"))
                    sendCreditGen(message);
                try {
                    StategyJobGroup(message);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            };
            channel.basicConsume(resultsQueue, true, deliverCallback, consumerTag -> {
            });

        } catch (Exception e) {
            //Logger.getLogger(Recv.class.getName()).log(Level.INFO, e.toString());
            e.printStackTrace();
        }
    }

    /**
     * send message to the client saing the winner
     *
     * @param message
     * @throws RemoteException
     */
    private void sendCreditGen(String message) throws RemoteException {
        String[] info = message.split(" @");
        new Producer(this.RabbitObservers.get(info[1].trim()), "GetCredits @ " + info[2].trim());
    }

    /**
     * Strategy for managing the genetic Algorithm
     *
     * @param message msage from the queue
     * @throws RemoteException
     * @throws InterruptedException
     */
    private void StategyJobGroup(String message) throws RemoteException, InterruptedException {
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
                        // o jobgroup e que esta a dizer ao worker que estrategia optar
                        new Producer(current.getId(), "Strategy @ " + current.nextStrategy().strategy);
                    }
                }
                if (this.bestWorker != null && this.resultsWokers.get(this.bestWorker) <= this.goal) {
                    stopQueues();
                    Thread.sleep(2000);
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

    /**
     * envia mensagem de stop para parar
     */
    private void stopQueues() {
        for (String id : this.RabbitObservers.keySet()) {
            new Producer(id, "stop");
        }
    }

    /**
     * notificar todos os workers do jobgroup
     *
     * @throws RemoteException
     */
    private void notifyWinnerGEN() throws RemoteException {
        for (String id : this.RabbitObservers.keySet()) {
            if (id.equals(this.bestGenetic.getId()))
                new Producer(id, "result @ " + REWARDWINNER);
            else
                new Producer(id, "result @ " + REWARDLOSER);
        }

        for (WorkerRMIRI workerRMIRI : this.observer) {
            workerRMIRI.getCredits(REWARDLOSER);
        }
    }

    /**
     * Notificar todos os workers do jobgroup e atribuir repetivas recompensas
     *
     * @throws RemoteException
     */
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

    /**
     * atualiza resultados do algorimto genetico
     *
     * @param id
     * @param result
     * @return
     */
    private ResultGenetic updateResultGenetic(String id, int result) {

        if (this.resultGenetics.containsKey(id)) {
            this.resultGenetics.get(id).atulizaResult(result);
        } else {
            this.resultGenetics.put(id, new ResultGenetic(id, result));
        }
        return this.resultGenetics.get(id);
    }

    /**
     * verifica se existe queues ativas
     *
     * @return
     */

    private boolean checkConection() {
        for (ResultGenetic current : this.resultGenetics.values()) {
            if (current.getStatus() == 1)
                return true;
        }
        return false;
    }

    /**
     * verifica se a funçao ficou presa
     */
    private void checkQueuesStuck() {
        for (ResultGenetic result : this.resultGenetics.values()) {
            String stuck = result.isqueueStuck();
            if (stuck != null) {
                new Producer(result.getId(), "stop");
            }
        }
    }
}


