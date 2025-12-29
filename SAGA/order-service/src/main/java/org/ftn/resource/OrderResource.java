package org.ftn.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import org.ftn.constant.OrderStatus;
import org.ftn.dto.OrderRequestDto;
import org.ftn.dto.OrderResponseDto;
import org.ftn.dto.PageResponse;
import org.ftn.service.OrderSagaService;
import org.ftn.service.OrderService;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.UUID;

import static org.ftn.constant.Roles.*;

@Path("/")
public class OrderResource {
    private final OrderSagaService orderSagaService;
    private final OrderService orderService;

    @Inject
    public OrderResource(OrderSagaService orderSagaService,
                         OrderService orderService) {
        this.orderSagaService = orderSagaService;
        this.orderService = orderService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ADMIN})
    public PageResponse<OrderResponseDto> getAll(@QueryParam("page") @Min(0) int page,
                                                 @QueryParam("size") @Min(1) @Max(100) int size) {
        return orderService.getAll(page, size);
    }

    @GET
    @Path("/status/{status}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ADMIN})
    public PageResponse<OrderResponseDto> getAll(@QueryParam("page") @Min(0) int page,
                                                 @QueryParam("size") @Min(1) @Max(100) int size,
                                                 @PathParam("status") OrderStatus status) {
        return orderService.getAll(page, size, status);
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ADMIN})
    public OrderResponseDto get(@PathParam("id") UUID id) {
        return orderService.get(id);
    }

    @GET
    @Path("/{id}/user/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ADMIN})
    public OrderResponseDto get(@PathParam("id") UUID id,
                                @PathParam("userId") UUID userId) {
        return orderService.get(id, userId);
    }

    @GET
    @Path("/{id}/mine")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({CUSTOMER})
    public OrderResponseDto getForCustomer(@PathParam("id") UUID id,
                                           @Context SecurityContext context) {
        return orderService.get(id, UUID.fromString(context.getUserPrincipal().getName()));
    }

    @GET
    @Path("/user/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ADMIN})
    public PageResponse<OrderResponseDto> getAllByUserId(@PathParam("userId") UUID userId,
                                                         @QueryParam("page") @Min(0) int page,
                                                         @QueryParam("size") @Min(1) @Max(100) int size) {
        return orderService.getAllByUserId(userId, page, size);
    }

    @GET
    @Path("/mine")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({CUSTOMER})
    public PageResponse<OrderResponseDto> getAllForCustomer(@QueryParam("page") @Min(0) int page,
                                                            @QueryParam("size") @Min(1) @Max(100) int size,
                                                            @Context SecurityContext context) {
        return orderService.getAllByUserId(UUID.fromString(context.getUserPrincipal().getName()), page, size);
    }

    @GET
    @Path("/product/{productId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ADMIN, MERCHANT})
    public PageResponse<OrderResponseDto> getAllByProductId(@PathParam("productId") UUID productId,
                                                            @QueryParam("page") @Min(0) int page,
                                                            @QueryParam("size") @Min(1) @Max(100) int size) {
        return orderService.getAllByProductId(productId, page, size);
    }

    @POST
    @Path("/saga")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    @RolesAllowed({SAGA_ORCHESTRATOR})
    public OrderResponseDto createOrder(@Valid OrderRequestDto body) {
        return orderSagaService.createOrder(body);
    }

    @PATCH
    @Path("/saga/{id}/cancel")
    @ResponseStatus(204)
    @RolesAllowed({SAGA_ORCHESTRATOR})
    public void cancelOrder(@PathParam("id") UUID id) {
        orderSagaService.cancelOrder(id);
    }

}
