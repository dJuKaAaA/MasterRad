package org.ftn.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.ftn.client.dto.KeycloakAuthResponse;
import org.ftn.client.exception.RestClientExceptionMapper;

import java.util.Map;

@RegisterProvider(RestClientExceptionMapper.class)
@RegisterRestClient(configKey = "keycloak-client")
public interface KeycloakClient {

    @POST
    @Path("/realms/distributed-transactions/protocol/openid-connect/token")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    KeycloakAuthResponse authenticate(
            @FormParam("grant_type") String grantType,
            @FormParam("client_id") String clientId,
            @FormParam("client_secret") String clientSecret,
            @FormParam("username") String username,
            @FormParam("password") String password
    );
}
