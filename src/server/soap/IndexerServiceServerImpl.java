/*
 * @author: Miguel Anciaes n43367 (m.anciaes@campus.fct.unl.pt)
 * @author: Ricardo Amaral n43368 (rm.amaral@campus.fct.unl.pt)
 */
package server.soap;

import api.Document;
import api.Endpoint;
import api.Serializer;
import api.ServerConfig;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.jws.WebService;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import sys.storage.LocalVolatileStorage;
import api.soap.IndexerService;
import static api.soap.IndexerService.NAME;
import static api.soap.IndexerService.NAMESPACE;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HttpsURLConnection;
import org.apache.kafka.clients.producer.ProducerRecord;
import server.soap.IndexerServiceServer.InsecureHostnameVerifier;

@WebService(
        serviceName = IndexerService.NAME,
        targetNamespace = IndexerService.NAMESPACE,
        endpointInterface = IndexerService.INTERFACE)

public class IndexerServiceServerImpl implements IndexerService {

    private LocalVolatileStorage storage = new LocalVolatileStorage(); //Documents "database"
    private String rendezUrl; //RendezVous location
    private int i;
    private static final String KEYWORD_SPLIT = "[ \\+]";
    private static final int SUCCESS_NOCONTENT = 204;

    private Producer<String, byte[]> producer;
    private long lastOffset;

    public IndexerServiceServerImpl() {
        Properties properties = new Properties();

        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka1:9092,kafka2:9092,kafka3:9092");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        //properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");

        producer = new KafkaProducer<>(properties);

        storage = new LocalVolatileStorage();
        new Thread(new kafkaReplication(this)).start();
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
                System.out.println("NAO DA!!!!!!!!!");
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

            producer.send(new ProducerRecord<String, byte[]>("Operation", "add", Serializer.serialize(doc)));
            try {
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch (InterruptedException ex) {
               
            }
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

        //Getting all indexers registered in rendezvous 
        Client client = ClientBuilder.newBuilder().hostnameVerifier(new IndexerServiceServer.InsecureHostnameVerifier()).build();

        Endpoint[] endpoints = null;
        for (int retry = 0; retry < 3; retry++) {
            try {
                WebTarget target = client.target(rendezUrl);
                endpoints = target.path("/")
                        .request()
                        .accept(MediaType.APPLICATION_JSON)
                        .get(Endpoint[].class);
                if (endpoints != null) {
                    break;
                }
            } catch (ProcessingException ex) {
                ex.printStackTrace();
                //retry up to three times
            }
        }

        boolean removed = false;
        //Removing the asked document from all indexers
        for (int i = 0; i < endpoints.length; i++) {
            try {
                Endpoint endpoint = endpoints[i];
                String url = endpoint.getUrl();
                Map<String, Object> map = endpoint.getAttributes();

                //Defensive progamming checks if server is soap or rest and ignores other types
                if (map.containsKey("type")) {
                    if (map.get("type").equals("soap")) {
                        if (removeSoap(id, url)) {
                            removed = true;
                        }
                    }
                    if (map.get("type").equals("rest")) {
                        if (removeRest(id, url)) {
                            removed = true;
                        }
                    }
                } else { //if no type tag exists - treat as rest server
                    if (removeRest(id, url)) {
                        removed = true;
                    }
                }
            } catch (Exception e) {
                //Do nothing... continue to remove on other indexers
            }
        }
        try {
            producer.send(new ProducerRecord<String, byte[]>("Operation", "add", Serializer.serialize(id)));
            //System.err.println(status ? "Document added successfully " : "An error occured. Document was not stored");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return removed;
    }

    public void setUrl(String rendezVousURL) {
        this.rendezUrl = rendezVousURL;
    }

    @Override
    public boolean removeDoc(String id) {
        return storage.remove(id);
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

    private boolean removeRest(String id, String url) {
        for (int retry = 0; retry < 3; retry++) {
            try {
                //Getting all indexers registered in rendezvous 
                Client client = ClientBuilder.newBuilder().hostnameVerifier(new IndexerServiceServer.InsecureHostnameVerifier()).build();
                WebTarget target = client.target(url);
                Response response = target.path("/remove/" + id).request().delete();

                return response.getStatus() == SUCCESS_NOCONTENT;

            } catch (ProcessingException x) {
                //retry method up to three times
            }
        }
        return false;
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

//        Properties props = new Properties();
//        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka1:9092,kafka2:9092,kafka3:9092");
//        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
//        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test" + System.nanoTime());
//        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
//        //props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,  "org.apache.kafka.common.serialization.StringDeserializer");
//        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArrayDeserializer");
//
//        try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(props)) {
//            consumer.subscribe(Arrays.asList("Snapshots"));
//
//            ConsumerRecords<String, byte[]> rec = consumer.poll(1000);
//            System.err.println("UVOU");
//            try {
//                if (rec.isEmpty()) {
//                    System.err.println("Empty");
//
//                    return new LocalVolatileStorage();
//                } else {
//                    System.err.println("Not empty");
//                    LocalVolatileStorage st = new LocalVolatileStorage();
//                    long offset = -1;
//                    Iterator<ConsumerRecord<String, byte[]>> it = rec.iterator();
//                    while (it.hasNext()) {
//                        ConsumerRecord<String, byte[]> r = it.next();
//                        if (r.offset() > offset) {
//                            offset = r.offset();
//                            st = ((Snapshot) Serializer.deserialize(r.value())).getStorage();
//                        }
//                    }
//
//                    return st;
//                }
//            } catch (IOException | ClassNotFoundException ex) {
//                ex.printStackTrace();
//            }
//        }
        return new LocalVolatileStorage();
    }

    private void addKafka(String id, Document doc, long offset) {

        if (storage.store(id, doc)) {
            System.out.println("ADD KAFKASSSS");
            lastOffset = offset;
        } else {
            System.err.println("NO ADD KAFKA: " + doc.hashCode());
        }
    }

    private void removeKafka(String id, long offset) {
        if (storage.remove(id)) {
            System.out.println("true");
            lastOffset = offset;
        }
    }

    static class kafkaReplication implements Runnable {

        IndexerServiceServerImpl isi;

        public kafkaReplication(IndexerServiceServerImpl implement) {
            isi = implement;
        }

        @Override
        public void run() {

            Properties props = new Properties();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka1:9092,kafka2:9092,kafka3:9092");
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            props.put(ConsumerConfig.GROUP_ID_CONFIG, "test" + System.nanoTime());
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
            //props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,  "org.apache.kafka.common.serialization.StringDeserializer");
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArrayDeserializer");

            try (KafkaConsumer<String, byte[]> consumer
                    = new KafkaConsumer<>(props)) {
                consumer.subscribe(Arrays.asList("Operation"));
                while (true) {
                    ConsumerRecords<String, byte[]> rec = consumer.poll(10);
                    rec.forEach(r -> {
                        try {
                            String op = r.key();
                            switch (op) {
                                case "add":
                                    Document doc = ((Document) Serializer.deserialize(r.value()));
                                    isi.addKafka(doc.id(), doc, r.offset());
                                    break;
                                case "remove":
                                    String id = ((String) Serializer.deserialize(r.value()));
                                    isi.removeKafka(id, r.offset());
                                    break;
                                default:
                                    break;
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
