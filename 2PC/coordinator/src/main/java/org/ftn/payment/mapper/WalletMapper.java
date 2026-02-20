package org.ftn.payment.mapper;

import org.ftn.payment.dto.WalletResponseDto;
import org.ftn.payment.entity.WalletEntity;

public interface WalletMapper {
    WalletResponseDto toDto(WalletEntity entity);
}
