package org.ftn.service.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ServerErrorException;
import org.ftn.constant.PaymentStatus;
import org.ftn.constant.Vote;
import org.ftn.dto.*;
import org.ftn.entity.PaymentEntity;
import org.ftn.entity.WalletEntity;
import org.ftn.mapper.PaymentWithLockMapper;
import org.ftn.repository.PaymentRepository;
import org.ftn.repository.SpringPaymentRepository;
import org.ftn.repository.SpringWalletRepository;
import org.ftn.repository.WalletRepository;
import org.ftn.service.Payment2PCService;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class Payment2PCServiceImpl implements Payment2PCService {
    private final PaymentRepository paymentRepository;
    private final WalletRepository walletRepository;
    private final PaymentWithLockMapper paymentWithLockMapper;

    private static final Logger LOG = Logger.getLogger(Payment2PCServiceImpl.class);

    @Inject
    public Payment2PCServiceImpl(PaymentRepository paymentRepository,
                                 WalletRepository walletRepository,
                                 PaymentWithLockMapper paymentWithLockMapper) {
        this.paymentRepository = paymentRepository;
        this.walletRepository = walletRepository;
        this.paymentWithLockMapper = paymentWithLockMapper;
    }

    @Transactional
    @Override
    public VoteResponse prepare(PaymentWithLockRequestDto dto) {
        LOG.info("Creating payment");
        Optional<WalletEntity> optionalWallet = walletRepository
                .find("userId", dto.userId())
                .firstResultOptional();
        if (optionalWallet.isEmpty()) {
            LOG.errorf("Wallet for user % not found");
            return new VoteResponse(Vote.NO, new ErrorResponseDto("User's wallet not found", 404));
        }
        WalletEntity wallet = optionalWallet.get();

        PaymentEntity payment = paymentWithLockMapper.toEntity(dto);
        if (payment.getTotalPrice().compareTo(wallet.getBalance()) > 0) {
            LOG.errorf("Insufficient balance for purchasing product %s", dto.productId());
            return new VoteResponse(Vote.NO, new ErrorResponseDto("Insufficient balance", 400));
        }

        payment.setPayer(wallet);
        payment.setLocked(true);
        payment.setStatus(PaymentStatus.PENDING);

        paymentRepository.persist(payment);

        LOG.infof("Successfully created payment %s", payment.getId());
        return new VoteResponse(Vote.YES, paymentWithLockMapper.toDto(payment));
    }

    @Transactional
    @Override
    public void commit(UUID id, UUID lockId) {
        LOG.infof("Committing payment %s", id);
        PaymentEntity payment = paymentRepository
                .findByIdOptional(id)
                .orElseThrow(() -> {
                    LOG.errorf("Payment %s not found while attempting commit", id);
                    return new ServerErrorException("Payment not found while attempting commit", 500);
                });
        if (!payment.tryUnlock(lockId)) {
            LOG.errorf("Payment %s is currently locked", payment.getId());
            throw new ServerErrorException("Payment is currently locked", 409);
        }

        payment.getPayer().pay(payment.getTotalPrice());
        payment.setPayedAt(Instant.now());
        payment.setStatus(PaymentStatus.SUCCESS);

        LOG.infof("Successful commit for payment %s", payment.getId());
        paymentRepository.persist(payment);
    }

    @Transactional
    @Override
    public void rollback(UUID id, UUID lockId) {
        Optional<PaymentEntity> optionalPayment = paymentRepository
                .findByIdOptional(id);

        if (optionalPayment.isPresent()) {
            LOG.infof("Rolling back payment %s", id);
            PaymentEntity payment = optionalPayment.get();
            if (!payment.tryUnlock(lockId)) {
                LOG.errorf("Payment %s is currently locked", payment.getId());
                throw new ServerErrorException("Payment is currently locked", 409);
            }

            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.persist(payment);
            LOG.infof("Successful rollback for payment %s", payment.getId());
        }

    }
}
