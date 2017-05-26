/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import com.google.gson.Gson;

public class Zookeeper {

    private ZooKeeper zookeeper;
    private static final int TIMEOUT = 5000;
    private CountDownLatch connectedSignal = new CountDownLatch(1);

    public Zookeeper(String servers) throws Exception {
        this.connect(servers, TIMEOUT);
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

    public boolean saveValue(String path, Endpoint value) {
        try {
            //int i = path.lastIndexOf(basePath);
            System.out.println("Path: " + path);
            int i = path.lastIndexOf('/');
            String parent = i < 0 ? path : path.substring(0, i);

            System.out.println("Parent: " + parent);

            Stat stat = getZooKeeper().exists(parent, false);

            if (stat == null) {
                System.out.println("Does not exist");
                getZooKeeper().create("/sd", "./sd/rendezvous".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                getZooKeeper().create(parent, "./sd/rendezvous".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                System.err.println("OUT");
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

    public List<byte[]> listValues(String path) {
        try {
            List<byte[]> res = new ArrayList<byte[]>();
            for (String child : getZooKeeper().getChildren(path, false)) {
                String nodePath = path + "/" + child;
                byte[] data = getZooKeeper().getData(nodePath, false, new Stat());
                //String json = new Gson().toJson(data);
                res.add(data);
            }
            return res;
        } catch (Exception x) {
            x.printStackTrace();
        }
        return Collections.emptyList();
    }
}
