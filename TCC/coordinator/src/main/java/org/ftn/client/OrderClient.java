package org.ftn.client;

import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.ftn.client.dto.OrderRequestDto;
import org.ftn.client.dto.OrderResponseDto;
import org.ftn.client.exception.RestClientExceptionMapper;

import java.util.UUID;

@RegisterRestClient(configKey = "order-client")
public interface OrderClient {
    @POST
    @Path("/tcc/try")
    @ClientHeaderParam(name = "Authorization", value = "Bearer {token}")
    OrderResponseDto tccTry(@Valid OrderRequestDto body,
                            @HeaderParam("token") String token);

    @PATCH
    @Path("/tcc/commit/{id}")
    @ClientHeaderParam(name = "Authorization", value = "Bearer {token}")
    void tccCommit(@PathParam("id") UUID id,
                   @HeaderParam("token") String token);

    @PATCH
    @Path("/tcc/cancel/{id}")
    @ClientHeaderParam(name = "Authorization", value = "Bearer {token}")
    void tccCancel(@PathParam("id") UUID id,
                   @HeaderParam("token") String token);
}

