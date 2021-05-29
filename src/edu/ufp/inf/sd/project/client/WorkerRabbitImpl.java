package edu.ufp.inf.sd.project.client;

import com.rabbitmq.client.*;
import edu.ufp.inf.sd.project.consumer.Consumer;
import edu.ufp.inf.sd.project.producer.Producer;
import edu.ufp.inf.sd.project.util.geneticalgorithm.CrossoverStrategies;
import edu.ufp.inf.sd.project.util.geneticalgorithm.GeneticAlgorithmJSSP;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


public class WorkerRabbitImpl implements WorkerRabbitRI, Runnable {

    private final String idRabbit;
    private final String idGenetic = UUID.randomUUID().toString();
    private String idJobGrooup;
    private String resultQueue;
    private String path;

    public WorkerRabbitImpl() {
        this.idRabbit = UUID.randomUUID().toString();
    }

    public void InitConsumer() {
        try {
            /* Open a connection and a channel, and declare the queue from which to consume.
            Declare the queue here, as well, because we might start the client before the publisher. */
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            //Use same username/passwd as the for accessing Management UI @ http://localhost:15672/
            //Default credentials are: guest/guest (change accordingly)
            factory.setUsername("guest");
            factory.setPassword("guest");
            //factory.setPassword("guest4rabbitmq");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();


            String resultsQueue = this.idRabbit;
            System.out.println(resultsQueue);
            channel.queueDeclare(resultsQueue, false, false, false, null);
            //channel.queueDeclare(Producer.QUEUE_NAME, true, false, false, null);
            System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

            /* The server pushes messages asynchronously, hence we provide a
            DefaultConsumer callback that will buffer the messages until we're ready to use them.
            Consumer client = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String message=new String(body, "UTF-8");
                    System.out.println(" [x] Received '" + message + "'");
                }
            };
            channel.basicConsume(Producer.QUEUE_NAME, true, client );
            */

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println(" [x] Received '" + message.split("@")[0] + "'");
                // executing the messages
                switchComands(message);
            };
            channel.basicConsume(resultsQueue, true, deliverCallback, consumerTag -> {
            });


        } catch (Exception e) {
            //Logger.getLogger(Recv.class.getName()).log(Level.INFO, e.toString());
            e.printStackTrace();
        }
    }

    public void connectionGenectic(String queue) {
        try {
            /* Open a connection and a channel, and declare the queue from which to consume.
            Declare the queue here, as well, because we might start the client before the publisher. */
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            //Use same username/passwd as the for accessing Management UI @ http://localhost:15672/
            //Default credentials are: guest/guest (change accordingly)
            factory.setUsername("guest");
            factory.setPassword("guest");
            //factory.setPassword("guest4rabbitmq");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            String resultsQueue = queue + "_results";
            System.out.println(resultsQueue);
            channel.queueDeclare(resultsQueue, false, false, false, null);
            //channel.queueDeclare(Producer.QUEUE_NAME, true, false, false, null);
            System.out.println(" [*] Waiting for messages. To exit press CTRL+C");


            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println(" [x] RESULT '" + message + "'");
                parseResult(message);
            };
            channel.basicConsume(resultsQueue, true, deliverCallback, consumerTag -> {
            });

        } catch (Exception e) {
            //Logger.getLogger(Recv.class.getName()).log(Level.INFO, e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        InitConsumer();
    }

    private void switchComands(String message) {
        String[] comand = message.split("@");
        switch (comand[0].trim()) {
            case "start":
                runGN();
                break;
            case "file":
                storeFile(comand[1].trim());
                break;
            case "JobGroup":
                this.idJobGrooup = comand[1].trim();
                break;
            case "Strategy":
                new Producer(this.idGenetic, comand[1].trim());
                break;
            case "stop":
                stopQueue();
                break;
            case "result":
                getCredits(comand[1].trim());
                break;
            default:
                System.out.println(message);
                break;
        }

    }

    private void getCredits(String reward) {
        new Producer(this.idJobGrooup, "reward @ " + this.idRabbit + " @" + reward);
    }

    private void runGN() {
        try {
            String ID_QUEUE = this.idGenetic;

            this.resultQueue = ID_QUEUE + "_results";

            Logger.getLogger(this.getClass().getName()).log(Level.INFO,
                    "GA is running for {0}, check queue {1}",
                    new Object[]{this.path, this.resultQueue});

            new Thread(this::runGenetic).start();

            connectionGenectic(ID_QUEUE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getPersonalId() throws RemoteException {
        return this.idRabbit;
    }

    private void sendMessage(String message) {
        new Producer(this.idGenetic, message);
    }

    private void stopQueue() {
        new Producer(this.idGenetic, "stop");
    }

    private void runGenetic() {
        new GeneticAlgorithmJSSP(this.path, this.idGenetic, CrossoverStrategies.ONE).run();
    }

    private void storeFile(String data) {
        try {
            String namefile = new File(" ").getAbsolutePath().trim() + this.idRabbit + ".txt";
            this.path = namefile;
            FileWriter myfile = new FileWriter(namefile);
            myfile.write(data);
            myfile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void parseResult(String message) {
        String[] result = message.split("=");
        if (result[0].trim().equals("Makespan"))
            new Producer(this.idJobGrooup, "result Worker @ " + this.idRabbit + " @ " + result[1].trim());
    }

}
