/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import com.google.gson.Gson;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.zookeeper.KeeperException;

public class Zookeeper {

    private ZooKeeper zookeeper;
    private static final int TIMEOUT = 5000;
    private CountDownLatch connectedSignal = new CountDownLatch(1);
    private static final String BASE_PATH = "/sd/endpoints";

    public Zookeeper(String servers) throws Exception {
        this.connect(servers, TIMEOUT);
        addRootNodes();
    }

    private void addRootNodes() {
        try {
            Stat stat = getZooKeeper().exists(BASE_PATH, false);

            if (stat == null) {
                getZooKeeper().create("/sd", ".root".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                getZooKeeper().create(BASE_PATH, ".root".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (KeeperException | InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    private ZooKeeper getZooKeeper() {
        if (zookeeper == null || !zookeeper.getState().equals(ZooKeeper.States.CONNECTED)) {
            throw new IllegalStateException("ZooKeeper is not connected.");
        }
        return zookeeper;
    }

    private void connect(String host, int timeout) throws IOException, InterruptedException {
        zookeeper = new ZooKeeper(host, timeout, new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                if (event.getState().equals(Watcher.Event.KeeperState.SyncConnected)) {
                    connectedSignal.countDown();
                }
            }

        });
        connectedSignal.await();
    }

    public boolean saveValue(String id, Endpoint value) {
        try {
            String path = BASE_PATH + "/" + id;
            
            Stat stat = getZooKeeper().exists(BASE_PATH, false);

            if (stat == null) {
                addRootNodes();
            }

            byte[] data = new Gson().toJson(value).getBytes();
            
            stat = getZooKeeper().exists(path, false);
            if (stat == null) {
                getZooKeeper().create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                return true;
            } else {
                //getZooKeeper().setData(path, data, -1);
                return false;
            }

        } catch (Exception x) {
            System.out.println("SAVE");
            x.printStackTrace();
            return false;
        }
    }
    
    public boolean removeValue (String id){
        try {
            String path = BASE_PATH + "/" + id;
            Stat stat = getZooKeeper().exists(path, false);
            
            if (stat == null) {
                return false;
            }
            
            getZooKeeper().delete(path, getZooKeeper().exists(path,true).getVersion());
            return true;
        } catch (KeeperException | InterruptedException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public List<String> listValues() {
        try {
            List<String> res = new ArrayList<String>();
            for (String child : getZooKeeper().getChildren(BASE_PATH, false)) {
                String nodePath = BASE_PATH + "/" + child;
                byte[] data = getZooKeeper().getData(nodePath, false, new Stat());
                String json = new String(data);
                res.add(json);
            }
            return res;
        } catch (Exception x) {
            x.printStackTrace();
        }
        return new ArrayList<String>();
    }
}
