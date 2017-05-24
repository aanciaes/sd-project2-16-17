/*
 * @author: Miguel Anciaes n43367 (m.anciaes@campus.fct.unl.pt)
 * @author: Ricardo Amaral n43368 (rm.amaral@campus.fct.unl.pt)
 */
package server.rest;

import api.Document;
import api.Endpoint;
import api.ServerConfig;
import api.Snapshot;
import api.SnapshotSerializer;
import api.rest.IndexerServiceAPI;
import api.soap.IndexerAPI;
import static api.soap.IndexerAPI.NAME;
import static api.soap.IndexerAPI.NAMESPACE;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
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

/**
 *
 * @author miguel
 */
public class IndexerServiceResources implements IndexerServiceAPI {

    private final LocalVolatileStorage storage = new LocalVolatileStorage();
    private String rendezUrl; //rebdezvous location
    private static final String KEYWORD_SPLIT = "[ \\+]";

    private Producer<String, byte[]> producer;
    private long lastOffset;

    public IndexerServiceResources() {

        Properties properties = new Properties();

        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka1:9092,kafka2:9092,kafka3:9092");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        //properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
        producer = new KafkaProducer<>(properties);
        
        new Thread(new test(this)).start();
    }

    @Override
    public List<String> search(String keywords) {

        try {
            List<String> list = new ArrayList<String>();
            list.add("badjoraz");
            list.add("ola");
            producer.send(new ProducerRecord<String, byte[]>("Operation", "add", SnapshotSerializer.serialize(new Document(keywords, list))));
        } catch (IOException ex) {
            ex.printStackTrace();
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

        if (!RendezVousServer.SECRET.equals(secret)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        boolean status = storage.store(id, doc);
        if (!status) {
            //If document already exists in storage
            throw new WebApplicationException(CONFLICT);
        }
        //System.err.println(status ? "Document added successfully " : "An error occured. Document was not stored");
    }

    @Override
    public void remove(String id, String secret) {
        if (!RendezVousServer.SECRET.equals(secret)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
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
                //retry up to three times
            }
        }
        //

        boolean removed = false;
        //Removing the asked document from all indexers
        for (int i = 0; i < endpoints.length; i++) {

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
        }
        if (!removed) { //No document removed
            throw new WebApplicationException(CONFLICT);
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
            Service service = Service.create(wsURL, QNAME);
            IndexerAPI indexer = service.getPort(IndexerAPI.class);
            return indexer.removeDoc(id);
        } catch (MalformedURLException ex) {
            return false;
        }
    }

    private boolean removeRest(String id, String url) throws WebApplicationException {
        for (int retry = 0; retry < 3; retry++) {
            try {
                Client client = ClientBuilder.newBuilder().hostnameVerifier(new IndexerServiceServer.InsecureHostnameVerifier()).build();
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
    
    public IndexerServiceResources getResourcesClass(){
        return this;
    }

    private void addKafka(String id, Document doc, long offset) {
            
            if(storage.store(id, doc)){
                System.out.println("true");
                lastOffset = offset;
            }
    }
    
    private void removeKafka(String id, long offset){
        if(storage.remove(id)){
            System.out.println("true");
            lastOffset = offset;
        }
    }

    static class test implements Runnable {
         IndexerServiceResources isr;
        public test(IndexerServiceResources resources){
            isr = resources;
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
                    ConsumerRecords<String, byte[]> rec = consumer.poll(1000);
                    rec.forEach(r -> {
                        try {
                            String op = r.key();
                            switch(op){
                                case "add":                            
                                     Document doc = ((Document)SnapshotSerializer.deserialize(r.value()));
                                     isr.addKafka(doc.id(), doc, r.offset());
                                break;
                                case "remove":
                                    String id = ((String)SnapshotSerializer.deserialize(r.value()));
                                     isr.removeKafka(id, r.offset());
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
