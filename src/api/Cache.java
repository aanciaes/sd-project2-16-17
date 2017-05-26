/*
 * @author: Miguel Anciaes n43367 (m.anciaes@campus.fct.unl.pt)
 * @author: Ricardo Amaral n43368 (rm.amaral@campus.fct.unl.pt)
 */
package api;

import java.util.List;

public interface Cache {

    /**
     * Checks if certain query is in cache based on the keywords hash
     *
     * @param hash combined hash of all keywords
     * @return true if query is in cache
     */
    boolean inCache(int hash);

    /**
     * Get tweets in cache of certain query
     *
     * @pre inCache(hash)
     * @param hash combined hash of all keywords
     * @return List of tweets in cache
     */
    List<String> getTweets(int hash /*or String url*/);

    /**
     * Stores a list of tweets in cache associated with a certain query
     *
     * @param hash combined hash of all keywords
     * @param tweets List of tweets
     */
    void store(int hash /*or String url*/, List<String> tweets);

    /**
     * Updates the cache. Checks which tweets have expired
     */
    void updateCache();

    /**
     * Deletes from cache the tweets associated with a certain query
     *
     * @param hash combined hash of all keywords
     */
    void delete(int hash /*or String url*/);

}
