package org.ftn.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.ftn.client.InventoryClient;
import org.ftn.client.OrderClient;
import org.ftn.client.PaymentClient;
import org.ftn.client.dto.*;
import org.ftn.dto.CreateOrderRequestDto;
import org.ftn.utils.ServiceAccountTokenProvider;

import java.net.URI;
import java.util.UUID;

import static org.ftn.constant.Roles.CUSTOMER;

@Path("/lra")
public class LRACoordinatorResource {
    private final OrderClient orderClient;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final ServiceAccountTokenProvider tokenProvider;
    private final JsonWebToken jwt;

    @Inject
    public LRACoordinatorResource(@RestClient OrderClient orderClient,
                                  @RestClient InventoryClient inventoryClient,
                                  @RestClient PaymentClient paymentClient,
                                  ServiceAccountTokenProvider tokenProvider,
                                  JsonWebToken jwt) {
        this.orderClient = orderClient;
        this.inventoryClient = inventoryClient;
        this.paymentClient = paymentClient;
        this.tokenProvider = tokenProvider;
        this.jwt = jwt;
    }

    @POST
    @Path("/create-order")
    @RolesAllowed({CUSTOMER})
    @LRA(value = LRA.Type.REQUIRED, end = false)
    public Response createOrder(@Valid CreateOrderRequestDto body,
                                @HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        String accessToken = tokenProvider.getAccessToken();
        UUID userId = UUID.fromString(jwt.getSubject());

        OrderResponseDto orderResponseDto = orderClient.lraTry(
                new OrderRequestDto(
                        body.productId(),
                        body.amount(),
                        userId,
                        UUID.randomUUID()   // TODO: Remove txId from this
                ),
                accessToken
        );

        InventoryResponseDto inventoryResponseDto = inventoryClient.lraTry(
                body.productId(),
                body.amount(),
                accessToken
        );

        PaymentResponseDto paymentResponseDto = paymentClient.lraTry(
                new PaymentRequestDto(
                        inventoryResponseDto.product().price(),
                        body.productId(),
                        body.amount(),
                        userId,
                        UUID.randomUUID()   // TODO: Remove txId from this
                ),
                accessToken
        );

        // if all OK → close LRA (confirm)
        return Response.ok().build();
    }
}
