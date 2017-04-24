/*
 * @author: Miguel Anciaes n43367 (m.anciaes@campus.fct.unl.pt)
 * @author: Ricardo Amaral n43368 (rm.amaral@campus.fct.unl.pt)
 */
package server.soap;

/**
 *
 * @author rmamaral
 */
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jws.WebService;

import api.Endpoint;
import api.soap.RendezVousAPI;

@WebService(
        serviceName = RendezVousAPI.NAME,
        targetNamespace = RendezVousAPI.NAMESPACE,
        endpointInterface = RendezVousAPI.INTERFACE)

public class RendezVousServiceImpl implements RendezVousAPI {

    private Map<String, Endpoint> db = new ConcurrentHashMap<>();

    @Override
    public Endpoint[] endpoints() {
        return db.values().toArray(new Endpoint[db.size()]);
    }

    @Override
    public boolean register(String id, Endpoint endpoint) {
        System.err.printf("register: %s <%s>\n", id, endpoint);

        if (db.containsKey(id)) {
            return false;
        } else {
            db.put(id, endpoint);
            return true;
        }
    }

    @Override
    public boolean update(String id, Endpoint endpoint) {
        System.err.printf("update: %s <%s>\n", id, endpoint);

        if (!db.containsKey(id)) {
            return false;
        } else {
            db.put(id, endpoint);
            return true;
        }
    }

    @Override
    public boolean unregister(String id) {
        System.err.printf("deleting: %s\n", id);

        if (!db.containsKey(id)) {
            return false;
        } else {
            db.remove(id);
            return true;
        }
    }
}
