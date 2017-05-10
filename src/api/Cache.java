/*
 * @author: Miguel Anciaes n43367 (m.anciaes@campus.fct.unl.pt)
 * @author: Ricardo Amaral n43368 (rm.amaral@campus.fct.unl.pt)
 */

package api;

import java.util.List;

public interface Cache {
    
    boolean inCache (String keywords /*or String url*/);
    
    List<String> getTweets (String keywords /*or String url*/);
    
    void store (String keywords /*or String url*/, List<String> tweets);
    
    void updateCache ();
    
    void delete (String keywords /*or String url*/);
    
}
