package edu.ufp.inf.sd.project.producer;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProducerFANOUT {
    private final String QUEUE_NAME;


    public ProducerFANOUT(String queue_name, String message) {
        QUEUE_NAME = queue_name;
        InitConnection(message);
    }


    private void InitConnection(String message) {


        /* Create a connection to the server (abstracts the socket connection,
           protocol version negotiation and authentication, etc.) */
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("guest");
        factory.setPassword("guest");

        String exchangeName = this.QUEUE_NAME;

        /* try-with-resources\. will close resources automatically in reverse order... avoids finally */
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()
        ) {

            // System.out.println("[*] Declare exchange:'"+exchangeName+"'of type "
            //        + BuiltinExchangeType.FANOUT);


            channel.exchangeDeclare(exchangeName, BuiltinExchangeType.FANOUT);


            String routingKey = "";
            channel.basicPublish(exchangeName, routingKey, null, message.getBytes(StandardCharsets.UTF_8));
            System.out.println("[*] Sent:'" + message.split("@")[1].trim() + "'");
        } catch (IOException | TimeoutException e) {
            Logger.getLogger(Producer.class.getName()).log(Level.INFO, e.toString());
        }
    }
}
