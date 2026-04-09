package org.ftn.mapper.impl;

import jakarta.enterprise.context.ApplicationScoped;
import org.ftn.dto.CheckoutErrorDto;
import org.ftn.entity.CheckoutErrorEntity;
import org.ftn.mapper.CheckoutErrorMapper;

@ApplicationScoped
public class CheckoutErrorMapperImpl implements CheckoutErrorMapper {
    @Override
    public CheckoutErrorDto toDto(CheckoutErrorEntity entity) {
        return new CheckoutErrorDto(
                entity.getId(),
                entity.getMessage(),
                entity.getStatus(),
                entity.getTimestamp()
        );
    }
}
