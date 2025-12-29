package org.ftn.service.impl;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.ftn.constant.PaymentStatus;
import org.ftn.dto.PaymentRequestDto;
import org.ftn.dto.PaymentResponseDto;
import org.ftn.entity.PaymentEntity;
import org.ftn.entity.WalletEntity;
import org.ftn.mapper.PaymentMapper;
import org.ftn.repository.PaymentRepository;
import org.ftn.repository.WalletRepository;
import org.ftn.service.PaymentSagaService;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PaymentSagaServiceImpl implements PaymentSagaService {
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final WalletRepository walletRepository;

    private static final Logger LOG = Logger.getLogger(PaymentSagaServiceImpl.class);

    @Inject
    public PaymentSagaServiceImpl(PaymentRepository paymentRepository,
                                  PaymentMapper paymentMapper,
                                  WalletRepository walletRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
        this.walletRepository = walletRepository;
    }

    @Transactional
    @Override
    public PaymentResponseDto process(PaymentRequestDto dto) {
        LOG.info("Processing payment");
        WalletEntity wallet = walletRepository
                .find("userId", dto.userId())
                .firstResultOptional()
                .orElseThrow(() -> {
                    LOG.errorf("Wallet for user % not found");
                    return new NotFoundException("User's wallet not found");
                });
        PaymentEntity payment = paymentMapper.toEntity(dto);
        if (payment.getTotalPrice().compareTo(wallet.getBalance()) > 0) {
            LOG.errorf("Insufficient balance for purchasing product %s", dto.productId());
            throw new BadRequestException("Insufficient balance");
        }

        payment.setPayer(wallet);
        payment.setPayedAt(Instant.now());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.getPayer().pay(payment.getTotalPrice());
        paymentRepository.persist(payment);

        LOG.infof("Successfully processed payment %s", payment.getId());
        return paymentMapper.toDto(payment);
    }

    @Transactional
    @Override
    public void refund(UUID id) {
        Optional<PaymentEntity> optionalPayment = paymentRepository
                .findByIdOptional(id);
        if (optionalPayment.isPresent()) {
            Log.infof("Refunding payment %s", id);
            PaymentEntity payment = optionalPayment.get();
            payment.getPayer().refund(payment.getTotalPrice());
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.persist(payment);
            Log.infof("Successfully refunded payment %s", payment.getId());
        }
    }
}
