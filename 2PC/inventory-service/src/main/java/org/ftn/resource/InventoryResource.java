package org.ftn.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import org.ftn.dto.*;
import org.ftn.resource.param.PaginationParam;
import org.ftn.service.Inventory2PCService;
import org.ftn.service.InventoryService;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.UUID;

import static org.ftn.constant.Roles.*;

@Path("/")
public class InventoryResource {
    private final InventoryService inventoryService;
    private final Inventory2PCService inventory2PCService;

    @Inject
    public InventoryResource(InventoryService inventoryService,
                             Inventory2PCService inventory2PCService) {
        this.inventoryService = inventoryService;
        this.inventory2PCService = inventory2PCService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ADMIN})
    public PageResponse<InventoryResponseDto> getAll(@BeanParam PaginationParam paginationParam) {
        return inventoryService.getAll(paginationParam.getPage(), paginationParam.getSize());
    }

    @GET
    @Path("/mine")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({MERCHANT})
    public PageResponse<InventoryResponseDto> getAllForMerchant(@BeanParam PaginationParam paginationParam,
                                                                @Context SecurityContext context) {
        return inventoryService.getAll(
                UUID.fromString(context.getUserPrincipal().getName()),
                paginationParam.getPage(),
                paginationParam.getSize()
        );
    }

    @GET
    @Path("/merchant/{merchantId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ADMIN})
    public PageResponse<InventoryResponseDto> getAllForMerchant(@BeanParam PaginationParam paginationParam,
                                                                @PathParam("merchantId") UUID merchantId) {
        return inventoryService.getAll(merchantId, paginationParam.getPage(), paginationParam.getSize());
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ADMIN})
    public InventoryResponseDto get(@PathParam("id") UUID id) {
        return inventoryService.get(id);
    }

    @GET
    @Path("/{id}/mine")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({MERCHANT})
    public InventoryResponseDto get(@PathParam("id") UUID id,
                                    @Context SecurityContext context) {
        return inventoryService.get(id, UUID.fromString(context.getUserPrincipal().getName()));
    }

    @POST
    @ResponseStatus(201)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ADMIN, MERCHANT})
    public InventoryResponseDto create(@Valid InventoryRequestDto body) {
        return inventoryService.create(body);
    }

    @DELETE
    @Path("/{id}")
    @ResponseStatus(204)
    @RolesAllowed({ADMIN})
    public void delete(@PathParam("id") UUID id) {
        inventoryService.delete(id);
    }

    @PATCH
    @Path("/product/{productId}/discontinue")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ADMIN, MERCHANT})
    public ProductResponseDto discontinueProduct(@PathParam("productId") UUID productId) {
        return inventoryService.discontinueProduct(productId);
    }

    @PATCH
    @Path("/{id}/replenish/{amount}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ADMIN, MERCHANT})
    public InventoryResponseDto replenishStock(@PathParam("id") UUID id, @PathParam("amount") int amount) {
        return inventoryService.replenishStock(id, amount);
    }

    @PUT
    @Path("/{productId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ADMIN, MERCHANT})
    public ProductResponseDto updateProduct(@PathParam("productId") UUID productId,
                                            @Valid ProductRequestDto body) {
        return inventoryService.updateProduct(productId, body);
    }

    @PATCH
    @Path("/2pc/prepare/{productId}/{amount}/tx/{txId}")
    @RolesAllowed({COORDINATOR_2PC})
    public VoteResponse prepare(@PathParam("productId") UUID productId,
                                @PathParam("amount") int amount,
                                @PathParam("txId") UUID txId) {
        return inventory2PCService.prepare(productId, amount, txId);
    }

    @PATCH
    @Path("/2pc/commit/{productId}/{amount}/lock/{lockId}")
    @ResponseStatus(204)
    @RolesAllowed({COORDINATOR_2PC})
    public void commit(@PathParam("productId") UUID productId,
                       @PathParam("amount") int amount,
                       @PathParam("lockId") UUID lockId) {
        inventory2PCService.commit(productId, amount, lockId);
    }

    @PATCH
    @Path("/2pc/rollback/{productId}/{amount}/lock/{lockId}")
    @ResponseStatus(204)
    @RolesAllowed({COORDINATOR_2PC})
    public void rollback(@PathParam("productId") UUID productId,
                         @PathParam("amount") int amount,
                         @PathParam("lockId") UUID lockId) {
        inventory2PCService.rollback(productId, amount, lockId);
    }

}
