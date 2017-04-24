/*
 * @author: Miguel Anciaes n43367 (m.anciaes@campus.fct.unl.pt)
 * @author: Ricardo Amaral n43368 (rm.amaral@campus.fct.unl.pt)
 */
package server.soap;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.core.UriBuilder;
import javax.xml.ws.Endpoint;

public class RendezVousServer {

    //Failure detection set to 5 seconds
    private static final int TIMEOUT = 5000;
    //Multicast Address, port and messages
    private static final String MULTICAST_ADDRESS = "238.69.69.69";
    private static final int MULTICAST_PORT = 6969;
    private static final String MULTICAST_MESSAGE = "rendezvous";
    private static final String KEEPALIVE_MESSAGE = "IAmAlive";

    //Configuration IP, listen on all IPv4 addresses on local machine
    private static final String ZERO_IP = "0.0.0.0";
    //base url of this server - contains "http", ip address, port and base path
    private static URI baseUri;
    //Failure detection map
    private static Map<String, Long> servers;

    private static RendezVousServiceImpl rendezvousImpl;

    public static void main(String[] args) throws Exception {

        servers = new ConcurrentHashMap<>();

        int port = 8080;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        //Setting server up
        String hostIP = InetAddress.getLocalHost().getHostAddress();
        baseUri = UriBuilder.fromUri(String.format("http://%s/contacts", hostIP)).port(port).build();

        //Set up Server
        String configURI = String.format("http://%s:%d/contacts", ZERO_IP, port);
        //Saves config instance so can insert rendezvous address later
        //Avoids multicast requests on remove document function
        rendezvousImpl = new RendezVousServiceImpl();
        Endpoint.publish(configURI, rendezvousImpl);
        //
        System.err.println("SOAP IndexerService Server ready @ " + baseUri);

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
            rendezvousImpl.unregister(key);
        }
    }
}
