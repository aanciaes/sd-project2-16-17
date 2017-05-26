/*
 * @author: Miguel Anciaes n43367 (m.anciaes@campus.fct.unl.pt)
 * @author: Ricardo Amaral n43368 (rm.amaral@campus.fct.unl.pt)
 */
package api;

import java.io.Serializable;
import sys.storage.LocalVolatileStorage;

/**
 * Class representing a snapshot of an indexer in a certain timestamp
 */
public class Snapshot implements Serializable {

    private LocalVolatileStorage storage;
    private long offset; //offset of last operation performed in this snapshot

    public Snapshot() {

    }

    public Snapshot(LocalVolatileStorage actual_storage, long offset) {
        this.storage = actual_storage;
        this.offset = offset;
    }

    //return storage
    public LocalVolatileStorage getStorage() {
        return storage;
    }

    //return las operation offset
    public long getOffset() {
        return offset;
    }
}
