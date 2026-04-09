package org.ftn.resource;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.ftn.dto.CreateOrderRequestDto;
import org.ftn.service.CheckoutErrorService;
import org.ftn.service.CoordinatorService;
import org.jboss.logging.Logger;

import java.util.UUID;

import static org.ftn.constant.Roles.CUSTOMER;

@Path("/")
public class CheckoutResource {
    private final CoordinatorService coordinatorService;
    private final CheckoutErrorService checkoutErrorService;
    private final JsonWebToken jwt;
    private final ManagedExecutor executor;

    private static final Logger LOG = Logger.getLogger(CheckoutResource.class);

    @Inject
    public CheckoutResource(CoordinatorService coordinatorService,
                            CheckoutErrorService checkoutErrorService,
                            JsonWebToken jwt,
                            ManagedExecutor executor) {
        this.coordinatorService = coordinatorService;
        this.checkoutErrorService = checkoutErrorService;
        this.jwt = jwt;
        this.executor = executor;
    }

    @POST
    @RolesAllowed({CUSTOMER})
    @Path("create-order")
    public Response createOrder(@Valid CreateOrderRequestDto requestBody) {
        UUID subject = UUID.fromString(jwt.getSubject());
        executor.runAsync(() -> {
            try {
                coordinatorService.createOrder(requestBody, subject);
            } catch (Exception e) {
                LOG.errorf("Something went wrong with the checkout: %s", ExceptionUtils.getRootCauseMessage(e));
                if (e instanceof WebApplicationException we) {
                    checkoutErrorService.save(requestBody, subject, we.getMessage(), we.getResponse().getStatus());
                }
            }
        });
        return Response.status(202).entity("Order created!").build();
    }
}
