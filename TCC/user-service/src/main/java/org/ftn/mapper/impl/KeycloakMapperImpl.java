package org.ftn.mapper.impl;

import jakarta.enterprise.context.ApplicationScoped;
import org.ftn.dto.UserResponseDto;
import org.ftn.mapper.KeycloakMapper;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class KeycloakMapperImpl implements KeycloakMapper {
    @Override
    public UserResponseDto toDto(UserRepresentation user) {
        return new UserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.isEmailVerified(),
                user.isEnabled(),
                user.getCreatedTimestamp()
        );
    }

    @Override
    public List<UserResponseDto> toDto(List<UserRepresentation> users) {
        return users.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}
