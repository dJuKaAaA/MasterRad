package org.ftn.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import org.ftn.dto.CreateOrderRequestDto;
import org.ftn.service.CoordinatorService;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.UUID;

import static org.ftn.constant.Roles.CUSTOMER;

@Path("/")
public class SpringCoordinatorResource {
    private final CoordinatorService coordinatorService;

    @Inject
    public SpringCoordinatorResource(CoordinatorService coordinatorService) {
        this.coordinatorService = coordinatorService;
    }

    @POST
    @Path("/create-order")
    @ResponseStatus(201)
    @RolesAllowed({CUSTOMER})
    @Consumes(MediaType.APPLICATION_JSON)
    public void createOrder(@Valid CreateOrderRequestDto body,
                            @Context SecurityContext context) {
        coordinatorService.createTransaction(body, UUID.fromString(context.getUserPrincipal().getName()));
    }

    // TODO: Make a cancel order endpoint and functionality
}
