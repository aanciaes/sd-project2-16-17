/*
 * @author: Miguel Anciaes n43367 (m.anciaes@campus.fct.unl.pt)
 * @author: Ricardo Amaral n43368 (rm.amaral@campus.fct.unl.pt)
 */
package server.rest;

import api.Endpoint;
import api.Zookeeper;

import javax.ws.rs.WebApplicationException;

import api.rest.RendezVousAPI;
import com.google.gson.Gson;
import java.util.List;

import static javax.ws.rs.core.Response.Status.*;

/**
 * Rendezvous Restful service implementation Replicated using ZooKeeper service
 */
public class RendezVousResources implements RendezVousAPI {

    private Zookeeper zk;

    public RendezVousResources() {

        try {
            this.zk = new Zookeeper("zoo1,zoo2,zoo3");
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Zookeeper failed to connect!");
        }
    }

    @Override
    public Endpoint[] endpoints() {
        List<String> asList = zk.listValues();
        Endpoint[] end = new Endpoint[asList.size()];

        for (int i = 0; i < asList.size(); i++) {
            end[i] = new Gson().fromJson(asList.get(i), Endpoint.class);
        }

        return end;
    }

    @Override
    public void register(String id, String secret, Endpoint endpoint) {
        System.err.printf("register: %s <%s>\n", id, endpoint);

        if (!RendezVousServer.SECRET.equals(secret)) {
            throw new WebApplicationException(FORBIDDEN);
        }

        if (!zk.saveValue(id, endpoint)) {
            throw new WebApplicationException(CONFLICT);
        }

    }

    @Override
    public void unregister(String id, String secret) {
        System.err.printf("deleting: %s\n", id);

        if (!RendezVousServer.SECRET.equals(secret)) {
            throw new WebApplicationException(FORBIDDEN);
        }

        if (!zk.removeValue(id)) {
            throw new WebApplicationException(NOT_FOUND);
        }
    }
}
