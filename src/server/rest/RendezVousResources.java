/*
 * @author: Miguel Anciaes n43367 (m.anciaes@campus.fct.unl.pt)
 * @author: Ricardo Amaral n43368 (rm.amaral@campus.fct.unl.pt)
 */
package server.rest;

import api.Endpoint;
import api.Serializer;
import api.Zookeeper;

import javax.ws.rs.WebApplicationException;

import api.rest.RendezVousAPI;
import java.io.IOException;
import java.util.List;

import static javax.ws.rs.core.Response.Status.*;

/**
 * Rendezvous Restful service implementation
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
        List<byte[]> asList = zk.listValues("/sd/rendezvous");
        Endpoint [] end = new Endpoint[asList.size()];
        
        
        
        try {
            System.err.println(((Endpoint) Serializer.deserialize(asList.get(0))).getUrl());
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        
        return end;
        //return asList.toArray(new Endpoint[asList.size()]);
        //return db.values().toArray(new Endpoint[db.size()]);
    }

    @Override
    public void register(String id, String secret, Endpoint endpoint) {
        System.err.printf("register: %s <%s>\n", id, endpoint);

        if (!RendezVousServer.SECRET.equals(secret)) {
            throw new WebApplicationException(FORBIDDEN);
        }

        if (!zk.saveValue("/sd/rendezvous/" + id, endpoint)) {
            throw new WebApplicationException(CONFLICT);
        }

    }

    @Override
    public void unregister(String id, String secret) {
        System.err.printf("deleting: %s\n", id);

        if (!RendezVousServer.SECRET.equals(secret)) {
            throw new WebApplicationException(FORBIDDEN);
        }

//        if (!db.containsKey(id)) {
//            throw new WebApplicationException(NOT_FOUND);
//        } else {
//            db.remove(id);
//        }
    }
}
