package org.ftn.service.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.ftn.dto.CheckoutErrorDto;
import org.ftn.dto.CreateOrderRequestDto;
import org.ftn.entity.CheckoutErrorEntity;
import org.ftn.repository.CheckoutErrorRepository;
import org.ftn.service.CheckoutErrorService;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class CheckoutErrorServiceImpl implements CheckoutErrorService {
    private final CheckoutErrorRepository checkoutErrorRepository;

    @Inject
    public CheckoutErrorServiceImpl(CheckoutErrorRepository checkoutErrorRepository) {
        this.checkoutErrorRepository = checkoutErrorRepository;
    }

    @Transactional
    @Override
    public void save(CreateOrderRequestDto requestDto, UUID userId, String message, int status) {
        CheckoutErrorEntity checkoutError = new CheckoutErrorEntity();
        checkoutError.setMessage(message);
        checkoutError.setStatus(status);
        checkoutError.setTimestamp(Instant.now());
        checkoutError.setProductId(requestDto.productId());
        checkoutError.setAmount(requestDto.amount());
        checkoutError.setUserId(userId);

        checkoutErrorRepository.persist(checkoutError);
    }

}
