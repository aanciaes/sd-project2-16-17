package server.rest.proxy;

import api.Cache;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author miguel
 */
public class CacheSystem implements Cache {

    public static final long MINUTE = 60000;

    //Storing pictures
    private Map<String, CacheObject> tweets;

    public CacheSystem() {
        tweets = new ConcurrentHashMap();
    }

    @Override
    public boolean inCache(String keywords) {
        return tweets.containsKey(keywords);
    }

    @Override
    public List<String> getTweets(String keywords) {
        return tweets.get(keywords).getTweets();
    }

    @Override
    public void store(String keywords, List<String> tweets) {
        if (inCache(keywords)) {
            this.tweets.get(keywords).hit();
        } else {
            this.tweets.put(keywords, new CacheObject(keywords, tweets, System.currentTimeMillis()));
        }
    }

    @Override
    public void updateCache() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(String keywords) {
        tweets.remove(keywords);
    }

    private class CacheObject {

        private final String keywords;
        private final List<String> tweets;
        private int hits;
        private Long lastAccess;

        public CacheObject(String keywords, List<String> tweets, Long lastAccess) {
            this.keywords = keywords;
            this.tweets = tweets;
            hits = 1;
            this.lastAccess = lastAccess;
        }

        public String getKeywords() {
            return keywords;
        }

        public List<String> getTweets() {
            return tweets;
        }

        public int getHits() {
            return hits;
        }

        public Long getLastAccess() {
            return lastAccess;
        }

        public void hit() {
            hits++;
            this.lastAccess = System.currentTimeMillis();
        }
    }
}
