package org.ftn.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.ftn.dto.PageResponse;
import org.ftn.dto.PaymentRequestDto;
import org.ftn.dto.PaymentResponseDto;
import org.ftn.resource.param.PaginationParam;
import org.ftn.service.PaymentService;
import org.ftn.service.PaymentTCCService;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.net.ConnectException;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.ftn.constant.Roles.*;

@Path("/")
public class PaymentResource {
    private final PaymentService paymentService;
    private final PaymentTCCService paymentTCCService;


    @Inject
    public PaymentResource(PaymentService paymentService,
                           PaymentTCCService paymentTCCService) {
        this.paymentService = paymentService;
        this.paymentTCCService = paymentTCCService;
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

    @Retry(maxRetries = 2, delay = 100, retryOn = {ConnectException.class})
    @POST
    @Path("/tcc/try")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    @RolesAllowed({COORDINATOR})
    public PaymentResponseDto prepare(@Valid PaymentRequestDto body) {
        return paymentTCCService.tccTry(body);
    }

    @Retry(
            maxRetries = 10,
            delay = 1,
            delayUnit = ChronoUnit.SECONDS,
            jitter = 500,
            jitterDelayUnit = ChronoUnit.MILLIS
    )
    @PATCH
    @Path("/tcc/commit/{id}")
    @RolesAllowed({COORDINATOR})
    public void commit(@PathParam("id") UUID id) {
        paymentTCCService.tccCommit(id);
    }

    @Retry(
            maxRetries = 10,
            delay = 1,
            delayUnit = ChronoUnit.SECONDS,
            jitter = 500,
            jitterDelayUnit = ChronoUnit.MILLIS
    )
    @PATCH
    @Path("/tcc/cancel/{id}")
    @RolesAllowed({COORDINATOR})
    public void rollback(@PathParam("id") UUID id) {
        paymentTCCService.tccCancel(id);
    }
}
