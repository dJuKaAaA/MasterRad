package org.ftn.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import org.ftn.dto.WalletResponseDto;
import org.ftn.service.WalletService;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.awt.*;
import java.math.BigDecimal;
import java.util.UUID;

import static org.ftn.constant.Roles.CUSTOMER;

@Path("/wallet")
public class WalletResource {
    private final WalletService walletService;

    public WalletResource(WalletService walletService) {
        this.walletService = walletService;
    }

    @POST
    @Path("/create")
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    @RolesAllowed({CUSTOMER})
    public WalletResponseDto create(@Context SecurityContext context) {
        return walletService.createForUser(UUID.fromString(context.getUserPrincipal().getName()));
    }

    @GET
    @Path("/mine")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({CUSTOMER})
    public WalletResponseDto getForUser(@Context SecurityContext context) {
        return walletService.getForUser(UUID.fromString(context.getUserPrincipal().getName()));
    }

    @PATCH
    @Path("/mine/increase-balance/{balance}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({CUSTOMER})
    public WalletResponseDto increaseBalanceForUser(@PathParam("balance") BigDecimal balance,
                                                    @Context SecurityContext context) {
        return walletService.increaseBalanceForUser(UUID.fromString(context.getUserPrincipal().getName()), balance);
    }
}
