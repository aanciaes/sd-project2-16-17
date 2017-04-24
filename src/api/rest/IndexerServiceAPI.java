/*
 * @author: Miguel Anciaes n43367 (m.anciaes@campus.fct.unl.pt)
 * @author: Ricardo Amaral n43368 (rm.amaral@campus.fct.unl.pt)
 */
package api.rest;

import api.Document;
import api.ServerConfig;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/indexer")
public interface IndexerServiceAPI {

    /**
     * Should return HTTP code 403 on security issue.
     */
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    List<String> search(@QueryParam("query") String keywords);

    /**
     * Configure server for accessing third-party service. Servers that do not
     * act as a proxy should do nothing and return 200.
     *
     * secret: for protecting access to this function passed as a query
     * parameter config: configuration for remote access Should return HTTP code
     * 403 on security issue.
     */
    @PUT
    @Path("/configure")
    @Consumes(MediaType.APPLICATION_JSON)
    void configure(@QueryParam("secret") String secret, ServerConfig config);

    /**
     * Secret for protecting access to this function passed as a query parameter
     * Should return HTTP code 403 on security issue.
     */
    @POST
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    void add(@PathParam("id") String id, @QueryParam("secret") String secret, Document doc);

    /**
     * Secret for protecting access to this function passed as a query parameter
     * Should return HTTP code 403 on security issue.
     */
    @DELETE
    @Path("/{id}")
    void remove(@PathParam("id") String id, @QueryParam("secret") String secret);

    @DELETE
    @Path("/remove/{id}")
    void removeDoc(@PathParam("id") String id);

}
