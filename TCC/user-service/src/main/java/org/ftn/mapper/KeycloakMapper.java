package org.ftn.mapper;

import org.ftn.dto.UserResponseDto;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;

public interface KeycloakMapper {
    UserResponseDto toDto(UserRepresentation user);
    List<UserResponseDto> toDto(List<UserRepresentation> users);
}
