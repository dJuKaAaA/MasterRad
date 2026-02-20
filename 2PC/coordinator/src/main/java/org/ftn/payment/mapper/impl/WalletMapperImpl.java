package org.ftn.payment.mapper.impl;

import jakarta.enterprise.context.ApplicationScoped;
import org.ftn.payment.dto.WalletResponseDto;
import org.ftn.payment.entity.WalletEntity;
import org.ftn.payment.mapper.WalletMapper;

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
