/*
 * @author: Miguel Anciaes n43367 (m.anciaes@campus.fct.unl.pt)
 * @author: Ricardo Amaral n43368 (rm.amaral@campus.fct.unl.pt)
 */
package server.rest;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.core.UriBuilder;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

public class RendezVousServer {

    //Configuration IP, listen on all IPv4 addresses on local machine
    private static final String ZERO_IP = "0.0.0.0";
    //Failure detection set to 5 seconds
    private static final int TIMEOUT = 5000;
    //Multicast Address, port and messages
    private static final String MULTICAST_ADDRESS = "238.69.69.69";
    private static final int MULTICAST_PORT = 6969;
    private static final String MULTICAST_MESSAGE = "rendezvous";
    private static final String KEEPALIVE_MESSAGE = "IAmAlive";

    //Time before server raises exception
    private static final int CONNECT_TIMEOUT = 1000;
    private static final int READ_TIMEOUT = 1000;

    //base url of this server - contains "http", ip address, port and base path
    private static URI baseUri;

    //Failure detection map
    private static Map<String, Long> servers;

    private static RendezVousResources resources;

    public static void main(String[] args) throws Exception {
        servers = new ConcurrentHashMap<>();

        int port = 8080;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        //Setting server up
        String hostIP = InetAddress.getLocalHost().getHostAddress();
        baseUri = UriBuilder.fromUri(String.format("http://%s/contacts", hostIP)).port(port).build();

        URI configAddr = UriBuilder.fromUri(String.format("http://%s/", ZERO_IP)).port(port).build();

        ResourceConfig config = new ResourceConfig();

        //Set timeouts
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.property(ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT);
        clientConfig.property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT);

        //Saving instance so no client is needed when calling itself on remove indexer function
        resources = new RendezVousResources();
        config.register(resources);
        JdkHttpServerFactory.createHttpServer(configAddr, config);

        System.err.println("REST RendezVous Server ready @ " + baseUri);
        //

        //Creating Multicast Socket
        final InetAddress address_multi = InetAddress.getByName(MULTICAST_ADDRESS);
        if (!address_multi.isMulticastAddress()) {
            System.out.println("Use range : 224.0.0.0 -- 239.255.255.255");
            System.exit(1);
        }

        MulticastSocket socket = new MulticastSocket(MULTICAST_PORT);
        socket.joinGroup(address_multi);

        //Creating and start keepAlive thread
        new Thread(new DetectFail()).start();

        //Waiting for a client request
        while (true) {
            byte[] buffer = new byte[65536];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            processMessage(packet, socket);
        }
    }

    /**
     * Processes the messages received in the multicast socket Checks whether or
     * not the message is meant to this server. Depending on the message
     * received it replies with rendezvous location or performs heartbeat
     * function
     *
     * @param packet Datagram Packet send by someone and received in the
     * multicast socket
     * @param socket Multicast Socket
     */
    private static void processMessage(DatagramPacket response, MulticastSocket socket) {
        String request = new String(response.getData(), 0, response.getLength());
        //In the "IamAlive" message, the endpoint id will come embedded in the message
        String[] split = request.split("/");

        switch (split[0]) {
            case MULTICAST_MESSAGE:
                multicastMessage(response, socket);
                break;
            case KEEPALIVE_MESSAGE:
                keepAliveMessage(split[1]);
                break;
            default:    //Ignore message
                break;
        }
    }

    /**
     * Processes multicast request Sends the rendezvous location to request
     * sender
     *
     * @param packet Packet received containing the request and headers
     * @param socket Multicast Socket for sending the response
     */
    private static void multicastMessage(DatagramPacket packet, MulticastSocket socket) {
        try {
            byte[] input = baseUri.toString().getBytes();
            DatagramPacket reply = new DatagramPacket(input, input.length);

            //set reply packet destination
            reply.setAddress(packet.getAddress());
            reply.setPort(packet.getPort());

            socket.send(reply);
        } catch (IOException ex) {
            System.err.println("Error processing message from client. No reply was sent");
        }
    }

    /**
     * Inserts or replaces entry with given id with new message timestamp
     *
     * @param id Endpoint ID
     */
    private static void keepAliveMessage(String id) {
        servers.put(id, System.currentTimeMillis());
    }

    /**
     * Thread class that handles all the indexer failure detection
     */
    static class DetectFail implements Runnable {

        @Override
        public void run() {

            while (true) {
                for (String key : servers.keySet()) {
                    if (System.currentTimeMillis() - servers.get(key) > TIMEOUT) {
                        deleteServer(key);
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                }
            }
        }

        /**
         * Deletes the Indexer Server with given key
         *
         * @param key
         */
        private static void deleteServer(String key) {
            servers.remove(key);

            resources.unregister(key);

            /**
             * for (int retry = 0; retry < 3; retry++) {
             *
             * ClientConfig config = new ClientConfig(); Client client =
             * ClientBuilder.newClient(config);
             *
             * URI rendezVousAddr = UriBuilder.fromUri(baseUri).build();
             *
             * WebTarget target = client.target(rendezVousAddr);
             *
             * try {
             *
             * Response response = target.path("/" + key) .request() .delete();
             * if (response.getStatus() == HTTP_SUCCESS_NOCONTENT) { break; }
             *
             * } catch (ProcessingException ex) { //retry } }
             */
        }
    }
}
