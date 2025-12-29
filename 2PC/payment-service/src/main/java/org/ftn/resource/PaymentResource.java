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
import org.ftn.dto.*;
import org.ftn.service.Payment2PCService;
import org.ftn.service.PaymentService;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.UUID;

import static org.ftn.constant.Roles.*;

@Path("/")
public class PaymentResource {
    private final PaymentService paymentService;
    private final Payment2PCService payment2PCService;


    @Inject
    public PaymentResource(PaymentService paymentService,
                           Payment2PCService payment2PCService) {
        this.paymentService = paymentService;
        this.payment2PCService = payment2PCService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ADMIN})
    public PageResponse<PaymentResponseDto> getAll(@QueryParam("page") @Min(0) int page,
                                                   @QueryParam("size") @Min(1) @Max(100) int size) {
        return paymentService.getAll(page, size);
    }

    @GET
    @Path("/mine")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({CUSTOMER})
    public PageResponse<PaymentResponseDto> getAllForCustomer(@QueryParam("page") @Min(0) int page,
                                                              @QueryParam("size") @Min(1) @Max(100) int size,
                                                              @Context SecurityContext context) {
        return paymentService.getAll(UUID.fromString(context.getUserPrincipal().getName()), page, size);
    }

    @GET
    @Path("/user/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ADMIN})
    public PageResponse<PaymentResponseDto> getAllForCustomer(@QueryParam("page") @Min(0) int page,
                                                              @QueryParam("size") @Min(1) @Max(100) int size,
                                                              @PathParam("userId") UUID userId) {
        return paymentService.getAll(userId, page, size);
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

    @POST
    @Path("/2pc/prepare")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    @RolesAllowed({COORDINATOR_2PC})
    public VoteResponse prepare(@Valid PaymentWithLockRequestDto body) {
        return payment2PCService.prepare(body);
    }

    @PATCH
    @Path("/{id}/2pc/commit/lock/{lockId}")
    @ResponseStatus(204)
    @RolesAllowed({COORDINATOR_2PC})
    public void commit(@PathParam("id") UUID id,
                       @PathParam("lockId") UUID lockId) {
        payment2PCService.commit(id, lockId);
    }

    @PATCH
    @Path("/{id}/2pc/rollback/lock/{lockId}")
    @ResponseStatus(204)
    @RolesAllowed({COORDINATOR_2PC})
    public void rollback(@PathParam("id") UUID id,
                         @PathParam("lockId") UUID lockId) {
        payment2PCService.rollback(id, lockId);
    }
}
