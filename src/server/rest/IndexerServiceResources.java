/*
 * @author: Miguel Anciaes n43367 (m.anciaes@campus.fct.unl.pt)
 * @author: Ricardo Amaral n43368 (rm.amaral@campus.fct.unl.pt)
 */
package server.rest;

import api.Document;
import api.Endpoint;
import api.rest.IndexerServiceAPI;
import api.soap.IndexerAPI;
import static api.soap.IndexerAPI.NAME;
import static api.soap.IndexerAPI.NAMESPACE;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import org.glassfish.jersey.client.ClientConfig;
import sys.storage.LocalVolatileStorage;

/**
 *
 * @author miguel
 */
public class IndexerServiceResources implements IndexerServiceAPI {

    private final LocalVolatileStorage storage = new LocalVolatileStorage();
    private String rendezUrl; //rebdezvous location

    @Override
    public List<String> search(String keywords) {

        //split query words
        String[] words = keywords.split("\\+");

        //Convert to List
        List<String> wordsLst = Arrays.asList(words);

        List<Document> documents = storage.search(wordsLst);
        List<String> response = new ArrayList<>();

        //Convert to List<String>
        for (Document doc : documents) {
            String url = doc.getUrl();
            response.add(url);
        }

        return response;
    }

    @Override
    public void add(String id, Document doc) {
        boolean status = storage.store(id, doc);
        if (!status) {
            //If document already exists in storage
            throw new WebApplicationException(CONFLICT);
        }
        //System.err.println(status ? "Document added successfully " : "An error occured. Document was not stored");
    }

    @Override
    public void remove(String id) {
        //Getting all indexers registered in rendezvous
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
        //

        boolean removed = false;
        //Removing the asked document from all indexers
        for (int i = 0; i < endpoints.length; i++) {

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
        }
        if (!removed) { //No document removed
            throw new WebApplicationException(CONFLICT);
        }
    }

    @Override
    public void removeDoc(String id) {

        boolean status = storage.remove(id);
        if (!status) {
            throw new WebApplicationException(NOT_FOUND);
        }

        System.out.println(status ? "Document removed." : "Document doesn't exist.");
    }

    public void setUrl(String rendezVousURL) {
        this.rendezUrl = rendezVousURL;
    }

    public boolean removeSoap(String id, String url) {
        try {
            URL wsURL = new URL(url);
            QName QNAME = new QName(NAMESPACE, NAME);
            Service service = Service.create(wsURL, QNAME);
            IndexerAPI indexer = service.getPort(IndexerAPI.class);
            return indexer.removeDoc(id);
        } catch (IndexerAPI.InvalidArgumentException | MalformedURLException ex) {
            return false;
        }
    }

    private boolean removeRest(String id, String url) throws WebApplicationException {
        for (int retry = 0; retry < 3; retry++) {
            try {
                ClientConfig config = new ClientConfig();
                Client client = ClientBuilder.newClient(config);
                WebTarget newTarget = client.target(url);
                Response response = newTarget.path("/remove/" + id).request().delete();

                return response.getStatus() == 204;
            } catch (ProcessingException x) {
                //retry method up to three times
            }
        }
        return false;
    }
}
