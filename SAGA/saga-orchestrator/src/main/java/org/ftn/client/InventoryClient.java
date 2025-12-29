package org.ftn.client;

import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.ftn.client.dto.InventoryResponseDto;

import java.util.UUID;

@RegisterRestClient(configKey = "inventory-client")
public interface InventoryClient {

    @PATCH
    @Path("/api/inventory/saga/reserve/{productId}/{amount}")
    @ClientHeaderParam(name = "Authorization", value = "Bearer {token}")
    InventoryResponseDto reserve(@PathParam("productId") UUID productId, @PathParam("amount") int amount, @HeaderParam("token") String token);

    @PATCH
    @Path("/api/inventory/saga/release/{productId}/{amount}")
    @ClientHeaderParam(name = "Authorization", value = "Bearer {token}")
    void release(@PathParam("productId") UUID productId, @PathParam("amount") int amount, @HeaderParam("token") String token);
}
