package org.ftn.client;

import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.ftn.client.dto.OrderRequestDto;
import org.ftn.client.dto.VoteResponseDto;
import org.ftn.client.exception.RestClientExceptionMapper;

import java.util.UUID;

@RegisterProvider(RestClientExceptionMapper.class)
@RegisterRestClient(configKey = "order-client")
public interface OrderClient {
    @POST
    @Path("/2pc/prepare")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ClientHeaderParam(name = "Authorization", value = "Bearer {token}")
    VoteResponseDto prepare(@Valid OrderRequestDto body,
                            @HeaderParam("token") String token);

    @PATCH
    @Path("/{id}/2pc/commit/lock/{lockId}")
    @ClientHeaderParam(name = "Authorization", value = "Bearer {token}")
    void commit(@PathParam("id") UUID id,
                @PathParam("lockId") UUID lockId,
                @HeaderParam("token") String token);

    @PATCH
    @Path("/{id}/2pc/rollback/lock/{lockId}")
    @ClientHeaderParam(name = "Authorization", value = "Bearer {token}")
    void rollback(@PathParam("id") UUID id,
                  @PathParam("lockId") UUID lockId,
                  @HeaderParam("token") String token);
}
