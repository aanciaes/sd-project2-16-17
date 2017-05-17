/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.rest.proxy;

import api.Document;
import api.ServerConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.apis.TwitterApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import server.rest.RendezVousServer;

/**
 *
 * @author miguel
 */
public class TwitterProxyResources implements api.rest.IndexerServiceAPI {

    private static final String SEARCH_API_TWITTER_BASE_URL = "https://api.twitter.com/1.1/search/tweets.json?q=";
    private static final String TWEET_BASE_URL = "https://twitter.com/statuses/";
    private static final String ENCODING = "UTF-8";

    ServerConfig serverConfig;
    CacheSystem cache = new CacheSystem();

    @Override
    public List<String> search(String keywords) {
        if(serverConfig==null)
            throw new WebApplicationException(Status.FORBIDDEN);
        
        try {
            if (!cache.inCache(keywords)) {

                final OAuth10aService service = new ServiceBuilder().apiKey(serverConfig.getApiKey()).apiSecret(serverConfig.getApuSecret())
                        .build(TwitterApi.instance());

                OAuth1AccessToken accessToken = new OAuth1AccessToken(serverConfig.getToken(), serverConfig.getTokenSecret());

                OAuthRequest req = new OAuthRequest(Verb.GET,
                        SEARCH_API_TWITTER_BASE_URL + URLEncoder.encode(keywords, ENCODING));
                service.signRequest(accessToken, req);
                final com.github.scribejava.core.model.Response res = service.execute(req);

                System.err.println("URL: " + req.getCompleteUrl());
                System.err.println("REST code:" + res.getCode());

                List<String> tweets = parseJson(res.getBody());
                cache.store(keywords, tweets);
                
                System.err.println("Cache miss");
                return tweets;
            }else{
                System.err.println("Cache hit");
                return cache.getTweets(keywords);
            }

        } catch (IOException | InterruptedException | ExecutionException ex) {
            ex.printStackTrace();
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void configure(String secret, ServerConfig config) {
        if (!RendezVousServer.SECRET.equals(secret)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        serverConfig = config;
        System.err.println("Proxy Server Configured");
    }

    @Override
    public void add(String id, String secret, Document doc) {
        throw new WebApplicationException(Status.METHOD_NOT_ALLOWED);
    }

    @Override
    public void remove(String id, String secret) {
        throw new WebApplicationException(Status.METHOD_NOT_ALLOWED);
    }

    @Override
    public void removeDoc(String id) {
        throw new WebApplicationException(Status.METHOD_NOT_ALLOWED);
    }

    private List<String> parseJson(String json) throws IOException{
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        
        List<String> ids = new ArrayList(root.size());
        
        
        root.at("/statuses").forEach(tweet -> {
            String tweetId = tweet.get("id").asText();
            ids.add(TWEET_BASE_URL + tweetId);
        });
        
        
        
        
//        Object obj = new JsonParser().parse(json);
//
//        JSONObject jsonObject = (JSONObject) obj;
//        JSONArray statuses = (JSONArray) jsonObject.get("statuses");


//        for (int i = 0; i < statuses.size(); i++) {
//            JSONObject tweet = (JSONObject) statuses.get(i);
//            ids.add(TWEET_BASE_URL + tweet.get("id"));
//        }
        return ids;
    }
}
