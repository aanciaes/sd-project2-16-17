/*
 * @author: Miguel Anciaes n43367 (m.anciaes@campus.fct.unl.pt)
 * @author: Ricardo Amaral n43368 (rm.amaral@campus.fct.unl.pt)
 */
package server.rest;

import api.Endpoint;
import api.SecureKeys;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.WebApplicationException;

import api.rest.RendezVousAPI;

import static javax.ws.rs.core.Response.Status.*;
import utils.Security;

/**
 * Rendezvous Restful service implementation
 */
public class RendezVousResources implements RendezVousAPI {

    private Map<String, Endpoint> db;
    
    private Map<String, String> keys;
    private String publicKey;
    
    public RendezVousResources () {
        db= new ConcurrentHashMap<>();
        keys=new ConcurrentHashMap<>();
        publicKey=Security.generateToken();
    }

    @Override
    public Endpoint[] endpoints() {
        return db.values().toArray(new Endpoint[db.size()]);
    }

    @Override
    public void register(String id, String secret, Endpoint endpoint) {
        System.err.printf("register: %s <%s>\n", id, endpoint);

        if (!keys.get(id).equals(secret)) {
            throw new WebApplicationException(FORBIDDEN);
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
            throw new WebApplicationException(FORBIDDEN);
        }

        if (!db.containsKey(id)) {
            throw new WebApplicationException(NOT_FOUND);
        } else {
            db.remove(id);
            keys.remove(id);
        }
    }

    @Override
    public SecureKeys requestAccess(String id) {
        if(keys.containsKey(id)){
            throw new WebApplicationException(CONFLICT);
        }else{
            SecureKeys sk = new SecureKeys(Security.generateToken(), publicKey);
            keys.put(id, sk.getPrivateKey());
            return sk;
        }
    }
    
    public boolean checkAccess (String id, String key){
        return keys.get(id).equals(key);
    }
}
