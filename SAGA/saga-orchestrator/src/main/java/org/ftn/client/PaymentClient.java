package org.ftn.client;

import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.ftn.client.dto.PaymentRequestDto;
import org.ftn.client.dto.PaymentResponseDto;

import java.util.UUID;

@RegisterRestClient(configKey = "payment-client")
public interface PaymentClient {

    @POST
    @Path("/saga")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ClientHeaderParam(name = "Authorization", value = "Bearer {token}")
    PaymentResponseDto process(@Valid PaymentRequestDto body, @HeaderParam("token") String token);

    @PATCH
    @Path("/saga/{id}")
    @ClientHeaderParam(name = "Authorization", value = "Bearer {token}")
    void refund(@PathParam("id") UUID id, @HeaderParam("token") String token);
}
