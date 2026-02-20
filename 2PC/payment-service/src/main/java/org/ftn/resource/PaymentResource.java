package org.ftn.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import org.ftn.dto.PageResponse;
import org.ftn.dto.PaymentResponseDto;
import org.ftn.resource.param.PaginationParam;
import org.ftn.service.PaymentService;

import java.util.UUID;

import static org.ftn.constant.Roles.*;

@Path("/")
public class PaymentResource {
    private final PaymentService paymentService;


    @Inject
    public PaymentResource(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ADMIN})
    public PageResponse<PaymentResponseDto> getAll(@BeanParam PaginationParam paginationParam) {
        return paymentService.getAll(paginationParam.getPage(), paginationParam.getSize());
    }

    @GET
    @Path("/mine")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({CUSTOMER})
    public PageResponse<PaymentResponseDto> getAllForCustomer(@BeanParam PaginationParam paginationParam,
                                                              @Context SecurityContext context) {
        return paymentService.getAll(
                UUID.fromString(context.getUserPrincipal().getName()),
                paginationParam.getPage(),
                paginationParam.getSize()
        );
    }

    @GET
    @Path("/user/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ADMIN})
    public PageResponse<PaymentResponseDto> getAllForCustomer(@BeanParam PaginationParam paginationParam,
                                                              @PathParam("userId") UUID userId) {
        return paymentService.getAll(userId, paginationParam.getPage(), paginationParam.getSize());
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ADMIN})
    public PaymentResponseDto get(@PathParam("id") UUID id) {
        return paymentService.get(id);
    }

    @GET
    @Path("/{id}/mine")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({CUSTOMER})
    public PaymentResponseDto get(@PathParam("id") UUID id,
                                  @Context SecurityContext context) {
        return paymentService.get(id, UUID.fromString(context.getUserPrincipal().getName()));
    }
}
