/*
 * @author: Miguel Anciaes n43367 (m.anciaes@campus.fct.unl.pt)
 * @author: Ricardo Amaral n43368 (rm.amaral@campus.fct.unl.pt)
 */
package server.soap;

import api.Document;
import api.Endpoint;
import api.soap.IndexerAPI;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.jws.WebService;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import org.glassfish.jersey.client.ClientConfig;
import sys.storage.LocalVolatileStorage;

@WebService(
        serviceName = IndexerAPI.NAME,
        targetNamespace = IndexerAPI.NAMESPACE,
        endpointInterface = IndexerAPI.INTERFACE)

public class IndexerServiceServerImpl implements IndexerAPI {

    private final LocalVolatileStorage storage = new LocalVolatileStorage(); //Documents "database"
    private String rendezUrl; //RendezVous location

    private static final String KEYWORD_SPLIT = "\\+";
    private static final int SUCCESS_NOCONTENT = 204;

    @Override
    public List<String> search(String keywords) throws InvalidArgumentException {
        if (keywords == null) {
            throw new InvalidArgumentException();
        }
        try {
            //Split query words
            String[] split = keywords.split(KEYWORD_SPLIT);

            //Convert to list
            List<String> query = Arrays.asList(split);
            List<Document> documents = storage.search(query);
            List<String> response = new ArrayList<>();

            for (int i = 0; i < documents.size(); i++) {
                String url = documents.get(i).getUrl();
                response.add(i, url);
            }
            return response;
        } catch (Exception e) {
            return new ArrayList<>(); // On error, return empty list
        }
    }

    @Override
    public boolean add(Document doc) throws InvalidArgumentException {
        if (doc == null) {
            throw new InvalidArgumentException();
        }
        try {
            boolean status = storage.store(doc.id(), doc);
            System.err.println(status ? "Document added successfully " : "An error occured. Document was not stored");
            return status;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean remove(String id) throws InvalidArgumentException {
        if (id == null) {
            throw new InvalidArgumentException();
        }

        ClientConfig config = new ClientConfig();
        Client client = ClientBuilder.newClient(config);

        Endpoint[] endpoints = null;
        for (int retry = 0; retry < 3; retry++) {
            try {
                WebTarget target = client.target(rendezUrl);
                endpoints = target.path("/")
                        .request()
                        .accept(MediaType.APPLICATION_JSON)
                        .get(Endpoint[].class);
                if (endpoints != null) {
                    break;
                }
            } catch (ProcessingException ex) {
                //retry up to three times
            }
        }

        boolean removed = false;
        //Removing the asked document from all indexers
        for (int i = 0; i < endpoints.length; i++) {
            try {
                Endpoint endpoint = endpoints[i];
                String url = endpoint.getUrl();
                Map<String, Object> map = endpoint.getAttributes();

                //Defensive progamming checks if server is soap or rest and ignores other types
                if (map.containsKey("type")) {
                    if (map.get("type").equals("soap")) {
                        if (removeSoap(id, url)) {
                            removed = true;
                        }
                    }
                    if (map.get("type").equals("rest")) {
                        if (removeRest(id, url)) {
                            removed = true;
                        }
                    }
                } else { //if no type tag exists - treat as rest server
                    if (removeRest(id, url)) {
                        removed = true;
                    }
                }
            } catch (Exception e) {
                //Do nothing... continue to remove on other indexers
            }
        }
        return removed;

    }

    public void setUrl(String rendezVousURL) {
        this.rendezUrl = rendezVousURL;
    }

    @Override
    public boolean removeDoc(String id) {
        return storage.remove(id);
    }

    public boolean removeSoap(String id, String url) {

        try {
            URL wsURL = new URL(url);
            QName QNAME = new QName(NAMESPACE, NAME);
            Service service = Service.create(wsURL, QNAME);
            IndexerAPI indexer = service.getPort(IndexerAPI.class);
            return indexer.removeDoc(id);
        } catch (MalformedURLException | InvalidArgumentException ex) {
            return false;
        }
    }

    private boolean removeRest(String id, String url) {
        for (int retry = 0; retry < 3; retry++) {
            try {
                ClientConfig config = new ClientConfig();
                Client client = ClientBuilder.newClient(config);
                WebTarget target = client.target(url);
                Response response = target.path("/remove/" + id).request().delete();

                return response.getStatus() == SUCCESS_NOCONTENT;

            } catch (ProcessingException x) {
                //retry method up to three times
            }
        }
        return false;
    }

}
