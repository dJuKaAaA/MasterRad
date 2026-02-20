package org.ftn.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import org.ftn.constant.OrderStatus;
import org.ftn.dto.OrderRequestDto;
import org.ftn.dto.OrderResponseDto;
import org.ftn.dto.PageResponse;
import org.ftn.resource.param.PaginationParam;
import org.ftn.service.OrderService;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.UUID;

import static org.ftn.constant.Roles.*;

@Path("/")
public class OrderResource {
    private final OrderService orderService;

    @Inject
    public OrderResource(OrderService orderService) {
        this.orderService = orderService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ADMIN})
    public PageResponse<OrderResponseDto> getAll(@BeanParam PaginationParam paginationParam) {
        return orderService.getAll(paginationParam.getPage(), paginationParam.getSize());
    }

    @GET
    @Path("/status/{status}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ADMIN})
    public PageResponse<OrderResponseDto> getAll(@BeanParam PaginationParam paginationParam,
                                                 @PathParam("status") OrderStatus status) {
        return orderService.getAll(paginationParam.getPage(), paginationParam.getSize(), status);
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
                                                         @BeanParam PaginationParam paginationParam) {
        return orderService.getAllByUserId(userId, paginationParam.getPage(), paginationParam.getSize());
    }

    @GET
    @Path("/mine")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({CUSTOMER})
    public PageResponse<OrderResponseDto> getAllForCustomer(@BeanParam PaginationParam paginationParam,
                                                            @Context SecurityContext context) {
        return orderService.getAllByUserId(
                UUID.fromString(context.getUserPrincipal().getName()),
                paginationParam.getPage(),
                paginationParam.getSize()
        );
    }

    @GET
    @Path("/product/{productId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ADMIN, MERCHANT})
    public PageResponse<OrderResponseDto> getAllByProductId(@PathParam("productId") UUID productId,
                                                            @BeanParam PaginationParam paginationParam) {
        return orderService.getAllByProductId(productId, paginationParam.getPage(), paginationParam.getSize());
    }

}
