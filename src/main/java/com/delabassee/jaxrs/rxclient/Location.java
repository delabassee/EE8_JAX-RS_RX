package com.delabassee.jaxrs.rxclient;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import javax.json.JsonObject;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;

/**
 * Simple pipeline that invokes 2 services using JAX-RS 2.1 Rx invoker
 * No security, no error handling, ...
 * @author davidd
 */
@Path("/")
public class Location {

    @Context
    private UriInfo context;

    public Location() {
    }

    @Path("location")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public void getJson(@Suspended final AsyncResponse async) throws NoSuchAlgorithmException, KeyManagementException {

        Client client = getUnsecureClient();

        LocationDetails locDetails = new LocationDetails();

        CompletionStage<JsonObject> cfIp = client.target("http://api.ipify.org/")
                .queryParam("format", "json")
                .request()
                .rx()
                .get(JsonObject.class);

        CompletionStage<CompletionStage<JsonObject>> cfLoc = cfIp.thenApply((ip) -> {
            String myIp = ip.getString("ip");
            //LOGGER.info(" -> " + ip);
            CompletionStage<JsonObject> cfGeoloc = client.target("https://ipvigilante.com")
                    .path("json").path(myIp)
                    .request()
                    .rx()
                    .get(JsonObject.class);
            return cfGeoloc;
        }).thenApply((cft) -> {
            try {
                JsonObject location = cft.toCompletableFuture().get();
                locDetails.setCity(location.getValue("/data/city_name").toString());
                locDetails.setCountry(location.getValue("/data/country_name").toString());
                //LOGGER.info(" -> cft : " + locDetails.getCity() + " " + locDetails.getCountry());
                async.resume(locDetails.get().orElse("???"));
            } catch (InterruptedException | ExecutionException ex) {
                //LOGGER.info(" -> whenComplete : " + ex);
            }
            return null;
        }
        );

    }
        
    private Client getUnsecureClient() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustManager = new X509TrustManager[]{new X509TrustManager() {
            // This client will trust anybody regardless of its Cert!!          
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustManager, null);
        Client client = ClientBuilder.newBuilder()
                .property("FOLLOW_REDIRECTS", true)
                .sslContext(sslContext).build();
        return client;
    }
    
}
