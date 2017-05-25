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
    private Map<Integer, CacheObject> tweets;

    public CacheSystem() {
        tweets = new ConcurrentHashMap();
        new Thread (new updateCache()).start();
    }

    @Override
    public boolean inCache(int hash) {
        return tweets.containsKey(hash);
    }

    @Override
    public List<String> getTweets(int hash) {
        return tweets.get(hash).getTweets();
    }

    @Override
    public void store(int hash, List<String> tweets) {
        this.tweets.put(hash, new CacheObject(hash, tweets, System.currentTimeMillis()));

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
    public void delete(int hash) {
        tweets.remove(hash);
    }

    private class CacheObject {

        private final int hash;
        private final List<String> tweets;
        private int hits;
        private final Long firstAccess;

        public CacheObject(int hash, List<String> tweets, Long firstAccess) {
            this.hash = hash;
            this.tweets = tweets;
            hits = 1;
            this.firstAccess = firstAccess;
        }

        public int getHash() {
            return hash;
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
