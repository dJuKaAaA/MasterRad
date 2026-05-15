package org.ftn.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import net.bytebuddy.implementation.bind.annotation.Default;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.ftn.constant.CoordinatorTransactionState;
import org.ftn.dto.CoordinatorTransactionDto;
import org.ftn.dto.CreateOrderRequestDto;
import org.ftn.service.CoordinatorService;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.UUID;

import static org.ftn.constant.Roles.CUSTOMER;

@Path("/")
public class CoordinatorResource {
    private final CoordinatorService coordinatorService;
    private final JsonWebToken jwt;

    @Inject
    public CoordinatorResource(CoordinatorService coordinatorService,
                               JsonWebToken jwt) {
        this.coordinatorService = coordinatorService;
        this.jwt = jwt;
    }

    @POST
    @Path("/create-order")
    @ResponseStatus(202)
    @RolesAllowed({CUSTOMER})
    @Consumes(MediaType.APPLICATION_JSON)
    public CoordinatorTransactionDto createOrder(@Valid CreateOrderRequestDto body) {
        return coordinatorService.createTransaction(body, UUID.fromString(jwt.getSubject()));
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public CoordinatorTransactionDto getTransaction(@PathParam("id") UUID id) {
        return coordinatorService.getTransaction(id);
    }

    @GET
    @Path("{id}/state")
    @Produces(MediaType.APPLICATION_JSON)
    public CoordinatorTransactionState getState(@PathParam("id") UUID id) {
        return coordinatorService.getState(id);
    }
}
