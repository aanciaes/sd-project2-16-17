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

    private static final long TTL = 10000;

    //Storing tweets
    private Map<String, CacheObject> tweets;

    public CacheSystem() {
        tweets = new ConcurrentHashMap();
        new Thread (new updateCache()).start();
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
        this.tweets.put(keywords, new CacheObject(keywords, tweets, System.currentTimeMillis()));

    }

    @Override
    public void updateCache() {
        tweets.forEach((k, v) -> {
            if (System.currentTimeMillis() - v.getFirstAccess() > TTL) {
                delete(k);
            }
        });
    }

    @Override
    public void delete(String keywords) {
        tweets.remove(keywords);
    }

    private class CacheObject {

        private final String keywords;
        private final List<String> tweets;
        private int hits;
        private final Long firstAccess;

        public CacheObject(String keywords, List<String> tweets, Long firstAccess) {
            this.keywords = keywords;
            this.tweets = tweets;
            hits = 1;
            this.firstAccess = firstAccess;
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

        public Long getFirstAccess() {
            return firstAccess;
        }

        public void hit() {
            hits++;
        }
    }
    
    /**
     * Thread class that deletes old objects from cache
     */
    class updateCache implements Runnable {

        @Override
        public void run() {
            while (true) {

                try {
                   updateCache();
                   Thread.sleep(3000);
                } catch (InterruptedException ex) {
                    //Some error occured
                }
            }
        }
    }
}
