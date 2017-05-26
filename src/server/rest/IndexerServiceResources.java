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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import sys.storage.LocalVolatileStorage;
import api.soap.IndexerService;
import static api.soap.IndexerService.NAME;
import static api.soap.IndexerService.NAMESPACE;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import server.rest.IndexerServiceServer.InsecureHostnameVerifier;

/**
 *
 * @author miguel
 */
public class IndexerServiceResources implements IndexerServiceAPI {

    private LocalVolatileStorage storage = new LocalVolatileStorage();
    private String rendezUrl; //rebdezvous location
    private static final String KEYWORD_SPLIT = "[ \\+]";

    private Producer<String, byte[]> producer;
    private long lastOffset;
    private long lastSnapshot;

    public IndexerServiceResources() {
        Properties properties = new Properties();

        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka1:9092,kafka2:9092,kafka3:9092");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        //properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");

        producer = new KafkaProducer<>(properties);
        producer.send(new ProducerRecord<String, byte[]>("Operation", "dummy", "dummy".getBytes()));
        producer.send(new ProducerRecord<String, byte[]>("Snapshots", "dummy", "dummy".getBytes()));

        storage = replicationInit();
        new Thread(new kafkaReplication(this)).start();

    }

    @Override
    public List<String> search(String keywords) {

        System.err.println("SEARCH");

        try {
            TimeUnit.MILLISECONDS.sleep(25);
        } catch (InterruptedException ex) {
            System.out.println("NAO DA!!!!!!!!!");
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
            System.err.println("NO ADD: " + doc.hashCode());
            throw new WebApplicationException(CONFLICT);
        }

        try {
            producer.send(new ProducerRecord<String, byte[]>("Operation", "add", Serializer.serialize(doc)));
            //System.err.println(status ? "Document added successfully " : "An error occured. Document was not stored");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void remove(String id, String secret) {
        if (!IndexerServiceServer.SECRET.equals(secret)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        removeDoc(id);

        try {
            producer.send(new ProducerRecord<String, byte[]>("Operation", "remove", Serializer.serialize(id)));
        } catch (IOException ex) {
            Logger.getLogger(IndexerServiceResources.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public void removeDoc(String id) {

        boolean status = storage.remove(id);
        if (!status) {
            throw new WebApplicationException(NOT_FOUND);
        }

        System.out.println(status ? "Document removed." : "Document doesn't exist.");
    }

    public void setUrl(String rendezVousURL) {
        this.rendezUrl = rendezVousURL;
    }

    public boolean removeSoap(String id, String url) {
        try {
            URL wsURL = new URL(url);
            QName QNAME = new QName(NAMESPACE, NAME);

            HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());

            Service service = Service.create(wsURL, QNAME);

            IndexerService indexer = service.getPort(IndexerService.class);
            return indexer.removeDoc(id);
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private boolean removeRest(String id, String url) throws WebApplicationException {
        for (int retry = 0; retry < 3; retry++) {
            try {
                Client client = ClientBuilder.newBuilder().hostnameVerifier(new InsecureHostnameVerifier()).build();
                WebTarget target = client.target(url);
                Response response = target.path("/remove/" + id).request().delete();

                return response.getStatus() == 204;
            } catch (ProcessingException x) {
                //retry method up to three times
            }
        }
        return false;
    }

    @Override
    public void configure(String secret, ServerConfig config) {
        //Do nothing, return success code
        if (!RendezVousServer.SECRET.equals(secret)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
    }

    public IndexerServiceResources getResourcesClass() {
        return this;
    }

    private void addKafka(String id, Document doc, long offset) {

        if (storage.store(id, doc)) {
            System.err.println("ADD KAFKASSSS");
            
        } else {
            System.err.println("NO ADD KAFKA: " + doc.hashCode());
        }
        lastOffset = offset;
    }

    private void removeKafka(String id, long offset) {
        if (storage.remove(id)) {
            System.out.println("REMOVE KAFKA");
           
        }
         lastOffset = offset;
    }

    private long getLastOffset() {
        return lastOffset;
    }

    private LocalVolatileStorage replicationInit() {

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka1:9092,kafka2:9092,kafka3:9092");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test" + System.nanoTime());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        //props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,  "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArrayDeserializer");

        try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Arrays.asList("Snapshots"));

            ConsumerRecords<String, byte[]> rec = consumer.poll(1000);

            Snapshot snap = null;
            long offset = -1;
            Iterator<ConsumerRecord<String, byte[]>> it = rec.iterator();
            while (it.hasNext()) {
                ConsumerRecord<String, byte[]> r = it.next();
                if (!r.key().equals("dummy")) {
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
    
    public void sendSnapshot () {
        if(System.currentTimeMillis() - lastSnapshot > 60000){
            try {
                producer.send(new ProducerRecord<String, byte[]>("Snapshots", "snapshot", Serializer.serialize(new Snapshot(storage, lastOffset))));
                lastSnapshot=System.currentTimeMillis();
                System.err.println("Snapshot sent");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    static class kafkaReplication implements Runnable {

        IndexerServiceResources isr;

        public kafkaReplication(IndexerServiceResources resources) {
            isr = resources;
        }

        @Override
        public void run() {

            Properties props = new Properties();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka1:9092,kafka2:9092,kafka3:9092");
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            props.put(ConsumerConfig.GROUP_ID_CONFIG, "test" + System.nanoTime());
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArrayDeserializer");

            try (KafkaConsumer<String, byte[]> consumer
                    = new KafkaConsumer<>(props)) {
                consumer.subscribe(Arrays.asList("Operation"));
                while (true) {
                    
                    isr.sendSnapshot();
                    
                    ConsumerRecords<String, byte[]> rec = consumer.poll(10);
                    rec.forEach(r -> {
                        try {
                            String op = r.key();

                            if (r.offset() > isr.getLastOffset()) {
                                switch (op) {
                                    case "add":
                                        Document doc = ((Document) Serializer.deserialize(r.value()));
                                        System.err.println("OFFSET ORDER: " + r.offset());
                                        isr.addKafka(doc.id(), doc, r.offset());
                                        break;
                                    case "remove":
                                        String id = ((String) Serializer.deserialize(r.value()));
                                        System.err.println("OFFSET ORDER: " + r.offset());
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
