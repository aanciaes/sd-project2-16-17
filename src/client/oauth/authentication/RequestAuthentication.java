/*
 * @author: Miguel Anciaes n43367 (m.anciaes@campus.fct.unl.pt)
 * @author: Ricardo Amaral n43368 (rm.amaral@campus.fct.unl.pt)
 */
package client.oauth.authentication;

import api.ServerConfig;
import com.github.scribejava.apis.TwitterApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.oauth.OAuth10aService;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

/**
 * Class used to get keys and tokens from twitter API
 */
public class RequestAuthentication {

    public static void main(String... args) {
        try {
//            String url = "https://172.18.0.3:8081/indexer";
//            if (args.length > 0) {
//                url = args[0];
//            }

            // Substituir pela API key atribuida
            final String apiKey = "B0SOwwuU5bEbUcaiSKUwrQN4o";
            // Substituir pelo API secret atribuido
            final String apiSecret = "aoM3IDBZPZsruLJXz1TFfEmMUCUX9K7OjokZhThVtCmH11qmZL";

            final OAuth10aService service = new ServiceBuilder().apiKey(apiKey).apiSecret(apiSecret)
                    .build(TwitterApi.instance());
            final Scanner in = new Scanner(System.in);

            final OAuth1RequestToken requestToken = service.getRequestToken();

            // Obtain the Authorization URL
            System.out.println("A obter o Authorization URL...");
            final String authorizationUrl = service.getAuthorizationUrl(requestToken);
            System.out.println("Necessario dar permissao neste URL:");
            System.out.println(authorizationUrl);
            System.out.println("e copiar o codigo obtido para aqui:");
            System.out.print(">>");
            final String code = in.nextLine();
            in.close();

            // Trade the Request Token and Verifier for the Access Token
            System.out.println("A obter o Access Token!");
            final OAuth1AccessToken accessToken = service.getAccessToken(requestToken, code);

            // obter elementos do access token
            String token = accessToken.getToken();
            String tokenSecret = accessToken.getTokenSecret();

            ServerConfig serverConfig = new ServerConfig(apiKey, apiSecret, token, tokenSecret);
            // construir access token a partir dos elementos
            System.err.println("apiKey: " + serverConfig.getApiKey());
            System.err.println("apiSecret: " + serverConfig.getApiSecret());
            System.err.println("token: " + serverConfig.getToken());
            System.err.println("tokenSecret: " + serverConfig.getTokenSecret());

//            for (int retry = 0; retry < 3; retry++) {
//                try {
//                   Client client = ClientBuilder.newBuilder().hostnameVerifier(new IndexerServiceServer.InsecureHostnameVerifier()).build();
//                    WebTarget target = client.target(url);
//                    Response response = target.path("/configure").queryParam("secret", SECRET)
//                            .request()
//                            .put(Entity.entity(serverConfig, MediaType.APPLICATION_JSON));
//
//                    System.err.println(response.getStatus());
//                    break;
//                } catch (ProcessingException ex) {
//                    ex.printStackTrace();
//                    //retry method up to three times
//                }
//            }
        } catch (IOException | InterruptedException | ExecutionException ex) {
            ex.printStackTrace();
        }
    }
}
