package org.ftn.client;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.ftn.client.dto.LoginRequestDto;
import org.ftn.client.dto.TokenResponseDto;
import org.ftn.client.exception.RestClientExceptionMapper;

@RegisterRestClient(configKey = "identity-provider-client")
@RegisterProvider(RestClientExceptionMapper.class)
public interface IdentityProviderClient {
    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    TokenResponseDto login(@Valid LoginRequestDto body);
}
