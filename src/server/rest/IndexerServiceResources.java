/*
 * @author: Miguel Anciaes n43367 (m.anciaes@campus.fct.unl.pt)
 * @author: Ricardo Amaral n43368 (rm.amaral@campus.fct.unl.pt)
 */
package server.rest;

import api.Document;
import api.ServerConfig;
import api.Serializer;
import api.Snapshot;
import api.rest.IndexerServiceAPI;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import sys.storage.LocalVolatileStorage;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class IndexerServiceResources implements IndexerServiceAPI {

    private LocalVolatileStorage storage = new LocalVolatileStorage();
    private static final String KEYWORD_SPLIT = "[ \\+]";
    private static final String DUMMY = "dummy";

    public static final String SNAPSHOT_TOPIC = "Snapshots";
    public static final String OPERATION_TOPIC = "Operation";
    public static final String ADD_OP = "add";
    public static final String REMOVE_OP = "remove";

    private static final long SNAP_TIME = 120000; //2 minutes

    private Producer<String, byte[]> producer;
    private long lastOffset;
    private long lastSnapshot;

    public IndexerServiceResources() {

        lastOffset = 0;
        lastSnapshot = 0;

        producer = new KafkaProducer<>(setProducerProperties());
        producer.send(new ProducerRecord<String, byte[]>(OPERATION_TOPIC, DUMMY, DUMMY.getBytes()));
        producer.send(new ProducerRecord<String, byte[]>(SNAPSHOT_TOPIC, DUMMY, DUMMY.getBytes()));

        storage = replicationInit(); //Initialize snapshot lookup

        //initialize kafka service
        new Thread(new kafkaReplication(this)).start();

    }

    private Properties setProducerProperties() {
        Properties properties = new Properties();

        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka1:9092,kafka2:9092,kafka3:9092");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");

        return properties;
    }

    private Properties setConsumerProperties() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka1:9092,kafka2:9092,kafka3:9092");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test" + System.nanoTime());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArrayDeserializer");

        return props;
    }

    @Override
    public List<String> search(String keywords) {

        //Sleeps to allow kafka messages to finish propagation
        try {
            TimeUnit.MILLISECONDS.sleep(25);
        } catch (InterruptedException ex) {
            //ignore
        }

        //split query words
        String[] words = keywords.split(KEYWORD_SPLIT);

        //Convert to List
        List<String> wordsLst = Arrays.asList(words);

        List<Document> documents = storage.search(wordsLst);
        List<String> response = new ArrayList<>();

        //Convert to List<String>
        for (Document doc : documents) {
            String url = doc.getUrl();
            response.add(url);
        }

        return response;
    }

    @Override
    public void add(String id, String secret, Document doc) {

        if (!IndexerServiceServer.SECRET.equals(secret)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        boolean status = storage.store(id, doc);
        if (!status) {
            //If document already exists in storage
            throw new WebApplicationException(CONFLICT);
        }

        //send kafka message
        try {
            producer.send(new ProducerRecord<String, byte[]>(OPERATION_TOPIC, ADD_OP, Serializer.serialize(doc)));
        } catch (IOException ex) {
            ex.printStackTrace();
        }//
    }

    @Override
    public void remove(String id, String secret) {
        if (!IndexerServiceServer.SECRET.equals(secret)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        removeDoc(id); //if remove fails, it throws web apllication error and does not send kafka message

        //sends kafka message
        try {
            producer.send(new ProducerRecord<String, byte[]>(OPERATION_TOPIC, REMOVE_OP, Serializer.serialize(id)));
        } catch (IOException ex) {
            ex.printStackTrace();
        }//

    }

    @Override
    public void removeDoc(String id) {

        boolean status = storage.remove(id);
        if (!status) {
            throw new WebApplicationException(NOT_FOUND);
        }

        System.out.println(status ? "Document removed." : "Document doesn't exist.");
    }

    @Override
    public void configure(String secret, ServerConfig config) {
        //Do nothing, return success code
        if (!RendezVousServer.SECRET.equals(secret)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
    }

    //method to add a document with request comming from kafka service (No further kafka propagation)
    private void addKafka(String id, Document doc, long offset) {
        storage.store(id, doc);
        lastOffset = offset;
    }

    //method to remove a document with request comming from kafka service (No further kafka propagation)
    private void removeKafka(String id, long offset) {
        storage.remove(id);
        lastOffset = offset;
    }

    //return last operation made
    private long getLastOffset() {
        return lastOffset;
    }

    /**
     * Checks for existing snapshots. It will update its storage with
     * information from snapshots or creates a new one if no information is
     * available
     *
     * @return storage
     */
    private LocalVolatileStorage replicationInit() {

        try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(setConsumerProperties())) {
            consumer.subscribe(Arrays.asList(SNAPSHOT_TOPIC));

            ConsumerRecords<String, byte[]> rec = consumer.poll(1000);

            Snapshot snap = null;
            long offset = -1;
            Iterator<ConsumerRecord<String, byte[]>> it = rec.iterator();
            //Checking for lastet offset
            while (it.hasNext()) {
                ConsumerRecord<String, byte[]> r = it.next();
                if (!r.key().equals(DUMMY)) { //ignoring dummies
                    if (r.offset() > offset) {
                        offset = r.offset();
                        snap = ((Snapshot) Serializer.deserialize(r.value()));
                    }
                }
            }
            if (snap != null) { //found snapshot
                lastOffset = snap.getOffset();
                System.err.println(lastOffset);
                return snap.getStorage();
            }
        } catch (ClassNotFoundException | IOException ex) {
            ex.printStackTrace();
        }

        //No Snapshot found
        return new LocalVolatileStorage();
    }

    /**
     * Sends a new snapshot representing the indexer current state to kafka
     * service
     */
    public void sendSnapshot() {
        if (System.currentTimeMillis() - lastSnapshot > SNAP_TIME) {
            try {
                producer.send(new ProducerRecord<String, byte[]>(SNAPSHOT_TOPIC, "snapshot", Serializer.serialize(new Snapshot(storage, lastOffset))));
                lastSnapshot = System.currentTimeMillis();
                System.err.println("Snapshot sent");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Class that handles kafka replication message forwarding
     */
    static class kafkaReplication implements Runnable {

        //reference to indexer
        IndexerServiceResources isr;

        public kafkaReplication(IndexerServiceResources resources) {
            isr = resources;
        }

        @Override
        public void run() {

            try (KafkaConsumer<String, byte[]> consumer
                    = new KafkaConsumer<>(isr.setConsumerProperties())) {
                consumer.subscribe(Arrays.asList(OPERATION_TOPIC));
                while (true) {
                    isr.sendSnapshot(); //sees if its time to send snapshot

                    ConsumerRecords<String, byte[]> rec = consumer.poll(10);
                    rec.forEach(r -> {
                        try {
                            String op = r.key();

                            if (r.offset() > isr.getLastOffset()) { //only catches recent messages
                                switch (op) {
                                    case ADD_OP:
                                        Document doc = ((Document) Serializer.deserialize(r.value()));
                                        isr.addKafka(doc.id(), doc, r.offset());
                                        break;
                                    case REMOVE_OP:
                                        String id = ((String) Serializer.deserialize(r.value()));
                                        isr.removeKafka(id, r.offset());
                                        break;
                                    default:
                                        break;
                                }
                            }
                        } catch (IOException | ClassNotFoundException ex) {
                            ex.printStackTrace();
                        }
                    });
                }
            }
        }
    }
}
