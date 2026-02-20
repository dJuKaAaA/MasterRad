package org.ftn.client;

import jakarta.ws.rs.*;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.ftn.client.dto.InventoryResponseDto;
import org.ftn.client.exception.RestClientExceptionMapper;

import java.util.UUID;

@RegisterRestClient(configKey = "inventory-client")
public interface InventoryClient {
    @PATCH
    @Path("/tcc/try/{productId}/{amount}")
    @ClientHeaderParam(name = "Authorization", value = "Bearer {token}")
    InventoryResponseDto tccTry(@PathParam("productId") UUID productId,
                                @PathParam("amount") int amount,
                                @HeaderParam("token") String token);

    @PATCH
    @Path("/tcc/commit/{productId}/{amount}")
    @ClientHeaderParam(name = "Authorization", value = "Bearer {token}")
    void tccCommit(@PathParam("productId") UUID productId,
                   @PathParam("amount") int amount,
                   @HeaderParam("token") String token);

    @PATCH
    @Path("/tcc/cancel/{productId}/{amount}")
    @ClientHeaderParam(name = "Authorization", value = "Bearer {token}")
    void tccCancel(@PathParam("productId") UUID productId,
                   @PathParam("amount") int amount,
                   @HeaderParam("token") String token);


}
