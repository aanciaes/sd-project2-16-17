/*
 * @author: Miguel Anciaes n43367 (m.anciaes@campus.fct.unl.pt)
 * @author: Ricardo Amaral n43368 (rm.amaral@campus.fct.unl.pt)
 */
package server.soap;

import static api.soap.IndexerService.*;

import api.Document;
import api.Serializer;
import api.ServerConfig;
import api.Snapshot;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javax.jws.WebService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import sys.storage.LocalVolatileStorage;
import api.soap.IndexerService;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;

@WebService(
        serviceName = IndexerService.NAME,
        targetNamespace = IndexerService.NAMESPACE,
        endpointInterface = IndexerService.INTERFACE)

public class IndexerServiceServerImpl implements IndexerService {

    private LocalVolatileStorage storage = new LocalVolatileStorage(); //Documents "database"
    private static final String KEYWORD_SPLIT = "[ \\+]";
    private static final int SUCCESS_NOCONTENT = 204;
    private static final String DUMMY = "dummy";
    public static final String SNAPSHOT_TOPIC = "Snapshots";
    public static final String OPERATION_TOPIC = "Operation";
    public static final String ADD_OP = "add";
    public static final String REMOVE_OP = "remove";

    private static final long SNAP_TIME = 120000; //2 minutes

    private Producer<String, byte[]> producer;
    private long lastOffset;
    private long lastSnapshot;

    public IndexerServiceServerImpl() {

        lastOffset = 0;
        lastSnapshot = 0;

        producer = new KafkaProducer<>(setProducerProperties());
        producer.send(new ProducerRecord<String, byte[]>(OPERATION_TOPIC, DUMMY, DUMMY.getBytes()));
        producer.send(new ProducerRecord<String, byte[]>(SNAPSHOT_TOPIC, DUMMY, DUMMY.getBytes()));

        storage = new LocalVolatileStorage();
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
    public List<String> search(String keywords) throws InvalidArgumentException {

        if (keywords == null) {
            throw new InvalidArgumentException();
        }
        try {

            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException ex) {
                //ignore
            }

            //Split query words
            String[] split = keywords.split(KEYWORD_SPLIT);

            //Convert to list
            List<String> query = Arrays.asList(split);
            List<Document> documents = storage.search(query);
            List<String> response = new ArrayList<>();

            for (int i = 0; i < documents.size(); i++) {
                String url = documents.get(i).getUrl();
                response.add(i, url);
            }
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>(); // On error, return empty list
        }
    }

    @Override
    public boolean add(Document doc, String secret) throws InvalidArgumentException, SecurityException {

        if (doc == null) {
            throw new InvalidArgumentException();
        }
        if (!IndexerServiceServer.SECRET.equals(secret)) {
            throw new SecurityException();
        }

        boolean status = storage.store(doc.id(), doc);
        System.out.println(status ? "Document added successfully " : "An error occured. Document was not stored");
        try {
            producer.send(new ProducerRecord<String, byte[]>(OPERATION_TOPIC, ADD_OP, Serializer.serialize(doc)));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return status;
    }

    @Override
    public boolean remove(String id, String secret) throws InvalidArgumentException, SecurityException {
        if (id == null) {
            throw new InvalidArgumentException();
        }

        if (!IndexerServiceServer.SECRET.equals(secret)) {
            throw new SecurityException();
        }

        boolean removed = removeDoc(id);

        if (removed) {
            try {
                producer.send(new ProducerRecord<String, byte[]>(OPERATION_TOPIC, REMOVE_OP, Serializer.serialize(id)));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return removed;
    }

    public boolean removeDoc(String id) {
        return storage.remove(id);
    }

    @Override
    public void configure(String secret, ServerConfig config) throws SecurityException, InvalidArgumentException {

        if (secret == null || config == null) {
            throw new InvalidArgumentException();
        }
        if (!IndexerServiceServer.SECRET.equals(secret)) {
            throw new SecurityException();
        }
    }

    private LocalVolatileStorage replicationInit() {

        try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(setConsumerProperties())) {
            consumer.subscribe(Arrays.asList(SNAPSHOT_TOPIC));

            ConsumerRecords<String, byte[]> rec = consumer.poll(1000);

            Snapshot snap = null;
            long offset = -1;
            Iterator<ConsumerRecord<String, byte[]>> it = rec.iterator();
            while (it.hasNext()) {
                ConsumerRecord<String, byte[]> r = it.next();
                if (!r.key().equals(DUMMY)) {
                    if (r.offset() > offset) {
                        offset = r.offset();
                        snap = ((Snapshot) Serializer.deserialize(r.value()));
                    }
                }
            }
            if (snap != null) {
                lastOffset = snap.getOffset();
                System.err.println(lastOffset);
                return snap.getStorage();
            }
        } catch (ClassNotFoundException | IOException ex) {
            ex.printStackTrace();
        }
        return new LocalVolatileStorage();
    }

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

    private void addKafka(String id, Document doc, long offset) {
        storage.store(id, doc);
        lastOffset = offset;
    }

    private void removeKafka(String id, long offset) {
        storage.remove(id);
        lastOffset = offset;
    }

    private long getLastOffset() {
        return lastOffset;
    }

    static class kafkaReplication implements Runnable {

        IndexerServiceServerImpl isi;

        public kafkaReplication(IndexerServiceServerImpl implement) {
            isi = implement;
        }

        @Override
        public void run() {

            try (KafkaConsumer<String, byte[]> consumer
                    = new KafkaConsumer<>(isi.setConsumerProperties())) {
                consumer.subscribe(Arrays.asList(OPERATION_TOPIC));
                while (true) {

                    isi.sendSnapshot();

                    ConsumerRecords<String, byte[]> rec = consumer.poll(10);
                    rec.forEach(r -> {
                        try {
                            String op = r.key();

                            if (r.offset() > isi.getLastOffset()) {
                                switch (op) {
                                    case ADD_OP:
                                        Document doc = ((Document) Serializer.deserialize(r.value()));
                                        isi.addKafka(doc.id(), doc, r.offset());
                                        break;
                                    case REMOVE_OP:
                                        String id = ((String) Serializer.deserialize(r.value()));
                                        isi.removeKafka(id, r.offset());
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
