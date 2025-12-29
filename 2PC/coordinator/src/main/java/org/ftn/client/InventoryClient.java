package org.ftn.client;

import jakarta.ws.rs.*;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.ftn.client.dto.VoteResponseDto;
import org.ftn.client.exception.RestClientExceptionMapper;

import java.util.UUID;

@RegisterProvider(RestClientExceptionMapper.class)
@RegisterRestClient(configKey = "inventory-client")
public interface InventoryClient {
    @PATCH
    @Path("/2pc/prepare/{productId}/{amount}/tx/{txId}")
    @ClientHeaderParam(name = "Authorization", value = "Bearer {token}")
    VoteResponseDto prepare(@PathParam("productId") UUID productId,
                            @PathParam("amount") int amount,
                            @PathParam("txId") UUID txId,
                            @HeaderParam("token") String token);

    @PATCH
    @Path("/2pc/commit/{productId}/{amount}/lock/{lockId}")
    @ClientHeaderParam(name = "Authorization", value = "Bearer {token}")
    void commit(@PathParam("productId") UUID productId,
                @PathParam("amount") int amount,
                @PathParam("lockId") UUID lockId,
                @HeaderParam("token") String token);

    @PATCH
    @Path("/2pc/rollback/{productId}/{amount}/lock/{lockId}")
    @ClientHeaderParam(name = "Authorization", value = "Bearer {token}")
    void rollback(@PathParam("productId") UUID productId,
                  @PathParam("amount") int amount,
                  @PathParam("lockId") UUID lockId,
                  @HeaderParam("token") String token);

}
