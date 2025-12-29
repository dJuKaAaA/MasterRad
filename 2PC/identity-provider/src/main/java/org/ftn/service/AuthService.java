package org.ftn.service;

import org.ftn.dto.LoginRequestDto;
import org.ftn.dto.TokenResponseDto;

public interface AuthService {
    TokenResponseDto login(LoginRequestDto body);
}
