/*
 * @author: Miguel Anciaes n43367 (m.anciaes@campus.fct.unl.pt)
 * @author: Ricardo Amaral n43368 (rm.amaral@campus.fct.unl.pt)
 */
package server.soap;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.xml.ws.Endpoint;

/**
 *
 * @author rmamaral
 */
public class IndexerServiceServer {

    private static final String ZERO_IP = "0.0.0.0";
    //Multicast addresses, port and messages
    private static final String MULTICAST_ADDRESS = "238.69.69.69";
    private static final int MULTICAST_PORT = 6969;
    private static final String MESSAGE = "rendezvous";
    private static final String HEARTBEATMESSAGE = "IAmAlive";

    //Time before server raises exception
    private static final String SOAP_CONN_TIMEOUT = "1000";
    private static final String SOAP_RECV_TIMEOUT = "1000";
    private static final int TIMEOUT = 1000;

    //this.endpoint
    private static api.Endpoint endpoint;
    //rendezvous location
    private static URI rendezVousAddr;

    public static void main(String[] args) throws Exception {

        int port = 8080;
        if (args.length > 0) {
            rendezVousAddr = UriBuilder.fromUri(args[0]).build();
        }

        //Set server type
        String hostIP = InetAddress.getLocalHost().getHostAddress();
        String hostAddress = UriBuilder.fromUri(String.format("https://%s/indexer", hostIP)).port(port).build().toString();

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("type", "soap");
        endpoint = new api.Endpoint(hostAddress, attributes);

//        //Set up Server
//        String configURI = String.format("https://%s:%d/indexer", ZERO_IP, port);

        //set timeouts
        System.setProperty("javax.xml.ws.client.connectionTimeout", SOAP_CONN_TIMEOUT);
        System.setProperty("javax.xml.ws.client.receiveTimeout", SOAP_RECV_TIMEOUT);

        // Create the HTTPS server using the ssl context.
        HttpsConfigurator configurator = new HttpsConfigurator(SSLContext.getDefault());
        HttpsServer httpsServer = HttpsServer.create(new InetSocketAddress(ZERO_IP, port), -1);
        httpsServer.setHttpsConfigurator(configurator);
        HttpContext httpContext = httpsServer.createContext("/indexer");
        httpsServer.start();

        //Saves config instance so can insert rendezvous address later
        //Avoids multicast requests on remove document function
        IndexerServiceServerImpl indexerServiceImpl = new IndexerServiceServerImpl();
        
        Endpoint ed = Endpoint.create(indexerServiceImpl);
        ed.publish(httpContext);
        //
        System.err.println("SOAP SSL IndexerService Server ready @ " + endpoint.getUrl());

        //Discovering RendezVousServer
        //Setting up multicast request.
        final int portMulti = 6969;
        final InetAddress multiAddress = InetAddress.getByName("238.69.69.69");
        if (!multiAddress.isMulticastAddress()) {
            System.out.println("Use range : 224.0.0.0 -- 239.255.255.255");
        }

        MulticastSocket socket = new MulticastSocket();

        //Send multicast request with MESSAGE - Send up to three times
        for (int retry = 0; retry < 3; retry++) {
            sendMulticastPacket(socket, MESSAGE);

            byte[] buffer = new byte[65536];
            DatagramPacket url_packet = new DatagramPacket(buffer, buffer.length);
            socket.setSoTimeout(TIMEOUT);

            try {
                socket.receive(url_packet);
                String rendezVousURL = new String(url_packet.getData(), 0, url_packet.getLength());

                int status = registerRendezVous(rendezVousURL);
                if (status == 204) {
                    indexerServiceImpl.setUrl(rendezVousURL);
                    System.err.println("Service registered succesfully");
                    break;
                }
                System.err.println("An error occured while registering on the RendezVousServer. HTTP Error code: " + status);

            } catch (SocketTimeoutException e) {
                //No server responded within given time
            }
        }

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
            Client client = ClientBuilder.newBuilder().hostnameVerifier(new InsecureHostnameVerifier()).build();

            rendezVousAddr = UriBuilder.fromUri(url).build();

            WebTarget target = client.target(rendezVousAddr);

            try {
                Response response = target.path("/" + endpoint.generateId()).queryParam("secret",server.rest.RendezVousServer.SECRET)
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
     * Thread class that handles heartbeat function
     */
    static class HeartBeat implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    MulticastSocket socket = new MulticastSocket();

                    String message = HEARTBEATMESSAGE + "/" + endpoint.generateId();
                    sendMulticastPacket(socket, message);
                    Thread.sleep(3000);

                } catch (IOException | InterruptedException ex) {
                }
            }
        }
    }

    static public class InsecureHostnameVerifier implements HostnameVerifier {
		@Override
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	}
    
}
