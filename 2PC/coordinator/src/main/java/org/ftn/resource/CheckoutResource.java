package org.ftn.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.ftn.dto.CreateOrderRequestDto;
import org.ftn.service.CoordinatorService;

import java.util.UUID;

import static org.ftn.constant.Roles.CUSTOMER;

@Path("/")
public class CheckoutResource {
    private final CoordinatorService coordinatorService;
    private final JsonWebToken jwt;

    @Inject
    public CheckoutResource(CoordinatorService coordinatorService,
                            JsonWebToken jwt) {
        this.coordinatorService = coordinatorService;
        this.jwt = jwt;
    }

    @POST
    @RolesAllowed({CUSTOMER})
    @Path("create-order")
    public Response createOrder(@Valid CreateOrderRequestDto requestBody) {
        coordinatorService.createOrder(requestBody, UUID.fromString(jwt.getSubject()));
        return Response.ok().build();
    }
}
