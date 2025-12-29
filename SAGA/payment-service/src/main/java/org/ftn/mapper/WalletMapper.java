package org.ftn.mapper;

import org.ftn.dto.WalletResponseDto;
import org.ftn.entity.WalletEntity;

public interface WalletMapper {
    WalletResponseDto toDto(WalletEntity entity);
}
