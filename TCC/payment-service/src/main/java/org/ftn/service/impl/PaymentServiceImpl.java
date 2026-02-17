package org.ftn.service.impl;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.ftn.dto.PageResponse;
import org.ftn.dto.PaymentResponseDto;
import org.ftn.entity.PaymentEntity;
import org.ftn.mapper.PaymentMapper;
import org.ftn.repository.PaymentRepository;
import org.ftn.repository.SpringPaymentRepository;
import org.ftn.service.PaymentService;
import org.jboss.logging.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    private static final Logger LOG = Logger.getLogger(PaymentServiceImpl.class);

    @Inject
    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              PaymentMapper paymentMapper) {
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
    }

    @Override
    public PageResponse<PaymentResponseDto> getAll(UUID userId, int page, int size) {
        PanacheQuery<PaymentEntity> pageQuery = paymentRepository
                .find("payer.userId", userId)
                .page(page, size);
        int totalPages = pageQuery.pageCount();
        long totalSize = paymentRepository.count("payer.userId", userId);

        List<PaymentResponseDto> paymentPages = pageQuery
                .list()
                .stream()
                .map(paymentMapper::toDto)
                .toList();

        LOG.infof("Fetched %d/%d payments for user %s, page %d/%d, page size %d",
                paymentPages.size(),
                totalSize,
                userId,
                page,
                totalPages,
                size
        );

        return new PageResponse<>(page, totalPages, paymentPages.size(), totalSize, paymentPages);
    }

    @Override
    public PageResponse<PaymentResponseDto> getAll(int page, int size) {
        PanacheQuery<PaymentEntity> pageQuery = paymentRepository
                .findAll()
                .page(page, size);
        int totalPages = pageQuery.pageCount();
        long totalSize = paymentRepository.count();

        List<PaymentResponseDto> paymentPages = pageQuery
                .list()
                .stream()
                .map(paymentMapper::toDto)
                .toList();

        LOG.infof("Fetched %d/%d payments, page %d/%d, page size %d",
                paymentPages.size(),
                totalSize,
                page,
                totalPages,
                size
        );

        return new PageResponse<>(page, totalPages, paymentPages.size(), totalSize, paymentPages);
    }

    @Override
    public PaymentResponseDto get(UUID id) {
        return paymentRepository
                .findByIdOptional(id)
                .map(paymentMapper::toDto)
                .orElseThrow(() -> {
                    LOG.errorf("Payment %s not found", id);
                    return new NotFoundException("Payment not found");
                });
    }

    @Override
    public PaymentResponseDto get(UUID id, UUID userId) {
        PaymentEntity payment = paymentRepository
                .findByIdOptional(id)
                .orElseThrow(() -> {
                    LOG.errorf("Payment %s not found", id);
                    return new NotFoundException("Payment not found");
                });
        if (!payment.getPayer().getUserId().equals(userId)) {
            LOG.infof("User %s mismatch with %s from payment %s", userId, payment.getPayer().getUserId(), payment.getId(), userId);
            throw new ForbiddenException("User id mismatch for payment");
        }

        LOG.infof("Fetched payment %s for user %s", payment.getId(), userId);
        return paymentMapper.toDto(payment);
    }
}
