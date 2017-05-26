/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.soap;

import api.Document;
import api.soap.IndexerService;
import static api.soap.IndexerService.NAME;
import static api.soap.IndexerService.NAMESPACE;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;

/**
 *
 * @author rmamaral
 */
public class AddClient {

    public static void main(String[] args) {
        int i = 0;
        System.out.println("sysout " + 1);
        try {
            URL wsURL = new URL("https://localhost:8082/indexer?wsdl");

            System.out.println("sysout " + 2);
            HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());
            QName QNAME = new QName(NAMESPACE, NAME);
            System.out.println("sysout " + 3);
            Service service = Service.create(wsURL, QNAME);
            System.out.println("sysout " + 4);
            IndexerService documents = service.getPort(IndexerService.class);
            List<String> list = new ArrayList<String>();
            list.add("badjoraz");
            list.add("ola");

            System.out.println(documents.add(new Document("" + i, list), "secret"));
            i++;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static public class InsecureHostnameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }

    }
}
