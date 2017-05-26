package api.soap;
 
import api.Document;
import api.ServerConfig;
import java.util.List;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.WebFault;

@WebService
public interface IndexerService {
    
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

    @WebFault
    class SecurityException extends Exception {

        private static final long serialVersionUID = 1L;

        public SecurityException() {
            super("");
        }        
        public SecurityException(String msg) {
            super(msg);
        }
    }

    static final String NAME="IndexerService";
    static final String NAMESPACE="http://sd2017";
    static final String INTERFACE="api.soap.IndexerService";

    /* keywords contains a list of works separated by '+'
     * returns the list of urls of the documents stored in this server that contain all the keywords 
     * throws IllegalArgumentException if keywords is null
     * throws SecurityException on security problem
     */
    @WebMethod
    List<String> search(String keywords) throws InvalidArgumentException ;

    /*
     * secret: for protecting access to this function passed as a query parameter
     * config: configuration for remote access
     * throws IllegalArgumentException if some parameter is null
     * throws SecurityException on security problem
     */
    @WebMethod
    void configure(String secret, ServerConfig config) throws InvalidArgumentException, SecurityException ;

    /*
     * secret protects access to this function.
     * return true if document was added, false if the document already exists in this server.
     * throws IllegalArgumentException if doc is null
     * throws SecurityException on security problem
     */
    @WebMethod
    boolean add(Document doc, String secret) throws InvalidArgumentException, SecurityException ;

    /*
     * secret protects access to this function.
     * return true if document was removed, false if was not found in the system.
     * throws IllegalArgumentException if id is null
     * throws SecurityException on security problem
     */
    @WebMethod
    boolean remove(String id, String secret) throws InvalidArgumentException, SecurityException ;
    
    /*
     * Auxiliar method to delete document only on this server
     * return true if document was removed, false it not
     */
    @WebMethod
    boolean removeDoc(String id);
}