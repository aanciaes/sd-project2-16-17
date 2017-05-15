/*
 * @author: Miguel Anciaes n43367 (m.anciaes@campus.fct.unl.pt)
 * @author: Ricardo Amaral n43368 (rm.amaral@campus.fct.unl.pt)
 */
package server.rest;

import api.Endpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.WebApplicationException;

import api.rest.RendezVousAPI;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.*;

/**
 * Rendezvous Restful service implementation
 */
public class RendezVousResources implements RendezVousAPI {

    private Map<String, Endpoint> db = new ConcurrentHashMap<>();

    @Override
    public Endpoint[] endpoints() {
        return db.values().toArray(new Endpoint[db.size()]);
    }

    @Override
    public void register(String id, String secret, Endpoint endpoint) {
        System.err.printf("register: %s <%s>\n", id, endpoint);

        if (!RendezVousServer.SECRET.equals(secret)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        if (db.containsKey(id)) {
            throw new WebApplicationException(CONFLICT);
        } else {
            db.put(id, endpoint);
        }
    }

    @Override
    public void unregister(String id, String secret) {
        System.err.printf("deleting: %s\n", id);

        if (!RendezVousServer.SECRET.equals(secret)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        if (!db.containsKey(id)) {
            throw new WebApplicationException(NOT_FOUND);
        } else {
            db.remove(id);
        }
    }
}
