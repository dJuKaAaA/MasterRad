package org.ftn.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.ftn.constant.SagaState;
import org.ftn.dto.CreateOrderRequestDto;
import org.ftn.dto.SagaResponseDto;
import org.ftn.entity.SagaEntity;
import org.ftn.service.SagaService;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.Map;
import java.util.UUID;

import static org.ftn.constant.Roles.CUSTOMER;

@Path("/")
public class SagaResource {
    private final SagaService sagaService;
    private final JsonWebToken jwt;

    @Inject
    public SagaResource(SagaService sagaService,
                        JsonWebToken jwt) {
        this.sagaService = sagaService;
        this.jwt = jwt;
    }

    @POST
    @Path("/create-order")
    @ResponseStatus(202)
    @RolesAllowed({CUSTOMER})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public SagaResponseDto createOrder(@Valid CreateOrderRequestDto body,
                                       @HeaderParam("Idempotency-Key") UUID idempotencyKey) {
        return sagaService.createOrderTransactionAsync(idempotencyKey, body, UUID.fromString(jwt.getSubject()));
    }

    @POST
    @Path("/create-order-sync")
    @ResponseStatus(202)
    @RolesAllowed({CUSTOMER})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public SagaResponseDto createOrderSync(@Valid CreateOrderRequestDto body,
                                           @HeaderParam("Idempotency-Key") UUID idempotencyKey) {
        return sagaService.createOrderTransactionSync(idempotencyKey, body, UUID.fromString(jwt.getSubject()));
    }

    @GET
    @Path("/{id}/state")
    @RolesAllowed({CUSTOMER})
    @Produces(MediaType.TEXT_PLAIN)
    public SagaState getState(@PathParam("id") UUID id) {
        return sagaService.getState(id);
    }

    // TODO: Make a cancel order endpoint and functionality
}
