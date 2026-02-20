package org.ftn.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.ftn.dto.CreateOrderRequestDto;
import org.ftn.service.CoordinatorService;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.net.URI;
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
    @ResponseStatus(201)
    @RolesAllowed({CUSTOMER})
    @Consumes(MediaType.APPLICATION_JSON)
    public void createOrder(@Valid CreateOrderRequestDto body) {
        coordinatorService.createTransaction(body, UUID.fromString(jwt.getSubject()));
    }

    // TODO: Make a cancel order endpoint and functionality
}
