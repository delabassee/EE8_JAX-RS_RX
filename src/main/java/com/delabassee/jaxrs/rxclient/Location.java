/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

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
