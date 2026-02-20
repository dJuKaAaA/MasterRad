package org.ftn.service.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;
import org.ftn.dto.UserCreateRequestDto;
import org.ftn.dto.UserResponseDto;
import org.ftn.mapper.KeycloakMapper;
import org.ftn.service.UserService;
import org.ftn.util.KeycloakOptionsMap;
import org.jboss.logging.Logger;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.ftn.constant.Roles.CUSTOMER;

@ApplicationScoped
public class UserServiceImpl implements UserService {
    private final Keycloak keycloak;
    private final KeycloakOptionsMap keycloakOptionsMap;
    private final KeycloakMapper keycloakMapper;

    private static final Logger LOG = Logger.getLogger(UserServiceImpl.class);

    @Inject
    public UserServiceImpl(Keycloak keycloak,
                           KeycloakOptionsMap keycloakOptionsMap,
                           KeycloakMapper keycloakMapper) {
        this.keycloak = keycloak;
        this.keycloakOptionsMap = keycloakOptionsMap;
        this.keycloakMapper = keycloakMapper;
    }

    @Override
    public UserResponseDto get(UUID id) {
        return keycloakMapper
                .toDto(keycloak
                        .realm(keycloakOptionsMap.realm())
                        .users()
                        .get(id.toString())
                        .toRepresentation());
    }

    @Override
    public Collection<UserResponseDto> getAll() {
        return keycloakMapper
                .toDto(keycloak
                        .realm(keycloakOptionsMap.realm())
                        .users()
                        .list());
    }

    @Override
    public void create(UserCreateRequestDto body) {
        UsersResource usersResource = keycloak.realm(keycloakOptionsMap.realm()).users();

        UserRepresentation user = new UserRepresentation();
        user.setUsername(body.username());
        user.setFirstName(body.firstName());
        user.setLastName(body.lastName());
        user.setEmail(body.email());
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setRealmRoles(List.of(CUSTOMER));

        String userId;
        try (Response response = usersResource.create(user)) {
            if (response.getStatus() != 201) {
                LOG.errorf("Failed to create user: HTTP %d", response.getStatus());
                throw new ClientErrorException("Failed to create user: HTTP ", response.getStatus());
            }

            userId = response.getHeaderString("Location").replaceAll(".*/([^/]+)$", "$1");

            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(body.password());
            credential.setTemporary(false);

            usersResource.get(userId).resetPassword(credential);
        }

        LOG.infof("User created successfully with id: %s", userId);
    }
}
