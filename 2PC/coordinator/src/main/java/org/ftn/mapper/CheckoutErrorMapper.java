package org.ftn.mapper;

import org.ftn.dto.CheckoutErrorDto;
import org.ftn.entity.CheckoutErrorEntity;

public interface CheckoutErrorMapper {
    CheckoutErrorDto toDto(CheckoutErrorEntity entity);
}
