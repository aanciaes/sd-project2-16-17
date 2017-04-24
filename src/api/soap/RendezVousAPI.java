/*
 * @author: Miguel Anciaes n43367 (m.anciaes@campus.fct.unl.pt)
 * @author: Ricardo Amaral n43368 (rm.amaral@campus.fct.unl.pt)
 */
package api.soap;

import api.Endpoint;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.WebFault;

@WebService
public interface RendezVousAPI {

    @WebFault
    class InvalidArgumentException extends Exception {

        private static final long serialVersionUID = 1L;

        public InvalidArgumentException() {
            super("");
        }

        public InvalidArgumentException(String msg) {
            super(msg);
        }
    }

    final String NAME = "RendezVousService";
    final String NAMESPACE = "http://sd2017";
    final String INTERFACE = "api.soap.RendezVousAPI";

    /**
     * Devolve array com a lista de servidores de indexacao registados.
     */
    @WebMethod
    Endpoint[] endpoints();

    /**
     * Regista novo servidor de indexacao.
     */
    @WebMethod
    boolean register(String id, Endpoint endpoint);

    @WebMethod
    boolean update(String id, Endpoint endpoint);

    /**
     * De-regista servidor de indexacao, dado o seu id.
     */
    @WebMethod
    boolean unregister(String id);

}
