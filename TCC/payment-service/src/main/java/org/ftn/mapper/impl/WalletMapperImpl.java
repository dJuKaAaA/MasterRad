package org.ftn.mapper.impl;

import jakarta.enterprise.context.ApplicationScoped;
import org.ftn.dto.WalletResponseDto;
import org.ftn.entity.WalletEntity;
import org.ftn.mapper.WalletMapper;

@ApplicationScoped
public class WalletMapperImpl implements WalletMapper {
    @Override
    public WalletResponseDto toDto(WalletEntity entity) {
        return new WalletResponseDto(
                entity.getId(),
                entity.getUserId(),
                entity.getBalance()
        );
    }
}
