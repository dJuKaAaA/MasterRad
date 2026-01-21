package org.ftn.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.ftn.client.dto.OrderRequestDto;
import org.ftn.client.dto.OrderResponseDto;

import java.util.UUID;

@RegisterRestClient(configKey = "order-client")
public interface OrderClient {

    @POST
    @Path("/saga")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ClientHeaderParam(name = "Authorization", value = "Bearer {token}")
    OrderResponseDto createOrder(OrderRequestDto body, @HeaderParam("token") String token);

    @PATCH
    @Path("/saga/{id}/cancel")
    @ClientHeaderParam(name = "Authorization", value = "Bearer {token}")
    void cancelOrder(@PathParam("id") UUID id, @HeaderParam("token") String token);
}
