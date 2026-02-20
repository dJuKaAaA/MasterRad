package org.ftn.client;

import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.ftn.client.dto.PaymentRequestDto;
import org.ftn.client.dto.PaymentResponseDto;
import org.ftn.client.dto.VoteResponseDto;
import org.ftn.client.exception.RestClientExceptionMapper;

import java.util.UUID;

@RegisterProvider(RestClientExceptionMapper.class)
@RegisterRestClient(configKey = "payment-client")
public interface PaymentClient {
    @POST
    @Path("/tcc/prepare")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ClientHeaderParam(name = "Authorization", value = "Bearer {token}")
    VoteResponseDto prepare(@Valid PaymentRequestDto body,
                            @HeaderParam("token") String token);

    @PATCH
    @Path("/{id}/tcc/commit/lock/{lockId}")
    @ClientHeaderParam(name = "Authorization", value = "Bearer {token}")
    void commit(@PathParam("id") UUID id,
                @PathParam("lockId") UUID lockId,
                @HeaderParam("token") String token);

    @PATCH
    @Path("/{id}/tcc/rollback/lock/{lockId}")
    @ClientHeaderParam(name = "Authorization", value = "Bearer {token}")
    void rollback(@PathParam("id") UUID id,
                  @PathParam("lockId") UUID lockId,
                  @HeaderParam("token") String token);

    @PUT
    @Path("/lra/try")
    @ClientHeaderParam(name = "Authorization", value = "Bearer {token}")
    PaymentResponseDto lraTry(@Valid PaymentRequestDto body,
                              @HeaderParam("token") String token);

}
