package org.ftn.service;

import org.ftn.dto.UserCreateRequestDto;
import org.ftn.dto.UserResponseDto;

import java.util.Collection;
import java.util.UUID;

public interface UserService {
    UserResponseDto get(UUID id);
    Collection<UserResponseDto> getAll();
    void create(UserCreateRequestDto body);
}
