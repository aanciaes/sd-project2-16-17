/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.rest.proxy;

import api.Document;
import api.ServerConfig;
import java.util.List;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 *
 * @author miguel
 */
public class TwitterProxyResources implements api.rest.IndexerServiceAPI{
    
    ServerConfig serverConfig;

    @Override
    public List<String> search(String keywords) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void configure(String secret, ServerConfig config) {
        if(secret!=secret)
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        
        serverConfig = config;
        System.err.println(serverConfig.getApiKey());     
    }

    @Override
    public void add(String id, String secret, Document doc) {
        throw new WebApplicationException(Response.Status.METHOD_NOT_ALLOWED);
    }

    @Override
    public void remove(String id, String secret) {
        throw new WebApplicationException(Response.Status.METHOD_NOT_ALLOWED);
    }

    @Override
    public void removeDoc(String id) {
        throw new WebApplicationException(Response.Status.METHOD_NOT_ALLOWED);
    }
}
