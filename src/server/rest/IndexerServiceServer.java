/*
 * @author: Miguel Anciaes n43367 (m.anciaes@campus.fct.unl.pt)
 * @author: Ricardo Amaral n43368 (rm.amaral@campus.fct.unl.pt)
 */
package server.rest;

import api.Endpoint;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

public class IndexerServiceServer {

    //Configuration IP, listen on all IPv4 addresses on local machine
    private static final String ZERO_IP = "0.0.0.0";
    private static final int PORT = 8081;

    //Multicast address, port and message
    private static final String MULTICAST_ADDRESS = "238.69.69.69";
    private static final int MULTICAST_PORT = 6969;
    private static final String MULTICAST_MESSAGE = "rendezvous";

    private static final String HEARTBEAT_MESSAGE = "IAmAlive";
    private static final int SOCKET_TIMEOUT = 1000;

    //Time before server raises exception
    private static final int CONNECT_TIMEOUT = 1000;
    private static final int READ_TIMEOUT = 1000;

    //this.endpoint
    private static Endpoint endpoint;
    //Rendezvous address
    private static URI rendezVousAddr;

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            rendezVousAddr = UriBuilder.fromUri(args[0]).build();
        }

        //Create endpoint
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("type", "rest");

        String hostAddress = InetAddress.getLocalHost().getHostAddress();
        endpoint = new Endpoint(UriBuilder.fromUri(String.format("http://%s/indexer", hostAddress))
                .port(PORT).build().toString(), attributes);
        //

        //Set up server
        URI configURI = UriBuilder.fromUri(String.format("http://%s/", ZERO_IP)).port(PORT).build();
        //Saves config instance so can insert rendezvous address later
        //Avoids multicast requests on remove document function
        ResourceConfig config = new ResourceConfig();

        //Set timeouts
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.property(ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT);
        clientConfig.property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT);

        IndexerServiceResources indexerResources = new IndexerServiceResources();
        config.register(indexerResources);
        JdkHttpServerFactory.createHttpServer(configURI, config);

        System.err.println("REST IndexerService Server ready @ " + endpoint.getUrl());
        //

        //Discovering RendezVousServer
        //Setting up multicast request.
        MulticastSocket socket = new MulticastSocket();

        //Send multicast request with MESSAGE - Send up to three times
        for (int retry = 0; retry < 3; retry++) {

            try {
                sendMulticastPacket(socket, MULTICAST_MESSAGE);

                byte[] buffer = new byte[65536];
                DatagramPacket url_packet = new DatagramPacket(buffer, buffer.length);
                socket.setSoTimeout(SOCKET_TIMEOUT);

                socket.receive(url_packet);
                String rendezVousURL = new String(url_packet.getData(), 0, url_packet.getLength());

                int status = registerRendezVous(rendezVousURL);
                if (status == 204) {
                    indexerResources.setUrl(rendezVousURL); //Sets rendezvous location on resources
                    System.err.println("Service registered succesfully");
                    break;
                }
                System.err.println("An error occured while registering on the RendezVousServer. HTTP Error code: " + status);

            } catch (SocketTimeoutException e) {
                //No server responded within given time
            } catch (IOException ex) {
                //IO error
            }
        }

        //Creating keepAlive thread
        new Thread(new HeartBeat()).start();
    }

    /**
     * Tries to register this endpoint on rendezvous server
     *
     * @param url rendezvous location
     * @return return http message or 0 if some error occured
     */
    private static int registerRendezVous(String url) {

        for (int retry = 0; retry < 3; retry++) {
            ClientConfig config = new ClientConfig();
            Client client = ClientBuilder.newClient(config);

            rendezVousAddr = UriBuilder.fromUri(url).build();

            WebTarget target = client.target(rendezVousAddr);

            try {
                Response response = target.path("/" + endpoint.generateId())
                        .request()
                        .post(Entity.entity(endpoint, MediaType.APPLICATION_JSON));
                return response.getStatus();

            } catch (ProcessingException ex) {
                //
            }
        }
        return 0;
    }

    /**
     * Sends a packet to the muticast address and port defined above
     *
     * @param socket multicast socket
     * @param message message to send in packet
     * @throws IOException
     */
    private static void sendMulticastPacket(MulticastSocket socket, String message) throws IOException {

        final InetAddress multiAddress = InetAddress.getByName(MULTICAST_ADDRESS);
        if (!multiAddress.isMulticastAddress()) {
            System.out.println("Use range : 224.0.0.0 -- 239.255.255.255");
        }

        byte[] input = (message).getBytes();
        DatagramPacket packet = new DatagramPacket(input, input.length);

        packet.setAddress(multiAddress);
        packet.setPort(MULTICAST_PORT);

        socket.send(packet);
    }

    /**
     * Thread class that handles the heartbeat system
     */
    static class HeartBeat implements Runnable {

        @Override
        public void run() {
            while (true) {

                try {
                    MulticastSocket socket = new MulticastSocket();
                    //heartbeat message identifier + sender endpoint id
                    //Identifiyng the sender of the message
                    String message = HEARTBEAT_MESSAGE + "/" + endpoint.generateId();

                    sendMulticastPacket(socket, message);

                    Thread.sleep(3000);

                } catch (IOException | InterruptedException ex) {
                    //Some error occured
                }
            }
        }
    }
}
