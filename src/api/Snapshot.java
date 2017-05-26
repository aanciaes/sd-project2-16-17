/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package api;

import java.io.Serializable;
import sys.storage.LocalVolatileStorage;

/**
 *
 * @author miguel
 */
public class Snapshot implements Serializable{
    
    private LocalVolatileStorage storage;
    private long offset;
    

    public Snapshot () {
        
    }
    
    public Snapshot(LocalVolatileStorage actual_storage, long offset) {
        this.storage = actual_storage;
        this.offset = offset;
    }

    public LocalVolatileStorage getStorage(){
        return storage;
    }
    
    public long getOffset(){
        return offset;
    }
}