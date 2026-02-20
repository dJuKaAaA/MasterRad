package org.ftn.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.ftn.dto.KeycloakAuthResponseDto;
import org.ftn.exception.RestClientExceptionMapper;

@RegisterProvider(RestClientExceptionMapper.class)
@RegisterRestClient(configKey = "keycloak-client")
public interface KeycloakClient {

    @POST
    @Path("/protocol/openid-connect/token")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    KeycloakAuthResponseDto authenticate(
            @FormParam("grant_type") String grantType,
            @FormParam("client_id") String clientId,
            @FormParam("client_secret") String clientSecret,
            @FormParam("username") String username,
            @FormParam("password") String password
    );
}
