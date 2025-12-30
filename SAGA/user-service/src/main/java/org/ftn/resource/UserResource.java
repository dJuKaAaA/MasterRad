package org.ftn.resource;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.ftn.dto.UserResponseDto;
import org.ftn.dto.UserCreateRequestDto;
import org.ftn.service.UserService;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.Collection;
import java.util.UUID;

import static org.ftn.constant.Roles.ADMIN;

@Path("/")
public class UserResource {
    private final UserService userService;

    @Inject
    public UserResource(UserService userService) {
        this.userService = userService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ADMIN})
    public Collection<UserResponseDto> getAll() {
        return userService.getAll();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ADMIN})
    public UserResponseDto get(@PathParam("id") UUID userId) {
        return userService.get(userId);
    }

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    @PermitAll
    public void create(@Valid UserCreateRequestDto body) {
        userService.create(body);
    }
}
