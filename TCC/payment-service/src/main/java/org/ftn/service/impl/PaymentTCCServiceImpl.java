package org.ftn.service.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import org.ftn.constant.PaymentStatus;
import org.ftn.dto.PaymentRequestDto;
import org.ftn.dto.PaymentResponseDto;
import org.ftn.entity.PaymentEntity;
import org.ftn.entity.WalletEntity;
import org.ftn.mapper.PaymentMapper;
import org.ftn.repository.PaymentRepository;
import org.ftn.repository.WalletRepository;
import org.ftn.service.PaymentTCCService;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PaymentTCCServiceImpl implements PaymentTCCService {
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final WalletRepository walletRepository;

    private static final Logger LOG = Logger.getLogger(PaymentTCCServiceImpl.class);

    @Inject
    public PaymentTCCServiceImpl(PaymentRepository paymentRepository, PaymentMapper paymentMapper, WalletRepository walletRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
        this.walletRepository = walletRepository;
    }

    @Transactional
    @Override
    public PaymentResponseDto tccTry(PaymentRequestDto dto) {
        LOG.info("Creating payment");
        Optional<WalletEntity> optionalWallet = walletRepository
                .find("userId", dto.userId())
                .firstResultOptional();
        if (optionalWallet.isEmpty()) {
            LOG.errorf("Wallet for user % not found");
            throw new NotFoundException("User's wallet not found");
        }

        WalletEntity wallet = optionalWallet.get();

        PaymentEntity payment = paymentMapper.toEntity(dto);
        if (payment.getTotalPrice().compareTo(wallet.getBalance()) > 0) {
            LOG.errorf("Insufficient balance for purchasing product %s", dto.productId());
            throw new BadRequestException("Insufficient balance");
        }

        payment.setPayer(wallet);
        payment.setStatus(PaymentStatus.PENDING);

        paymentRepository.persist(payment);

        LOG.infof("Successfully created payment %s", payment.getId());

        return paymentMapper.toDto(payment);
    }

    @Override
    public void tccCommit(UUID id) {
        LOG.infof("Committing payment %s", id);
        PaymentEntity payment = paymentRepository
                .findByIdOptional(id)
                .orElseThrow(() -> {
                    LOG.errorf("Payment %s not found while attempting commit", id);
                    return new ServerErrorException("Payment not found while attempting commit", 500);
                });

        payment.getPayer().pay(payment.getTotalPrice());
        payment.setPayedAt(Instant.now());
        payment.setStatus(PaymentStatus.SUCCESS);

        LOG.infof("Successful commit for payment %s", payment.getId());
        paymentRepository.persist(payment);
    }

    @Override
    public void tccCancel(UUID id) {
        Optional<PaymentEntity> optionalPayment = paymentRepository
                .findByIdOptional(id);

        if (optionalPayment.isPresent()) {
            LOG.infof("Rolling back payment %s", id);
            PaymentEntity payment = optionalPayment.get();

            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.persist(payment);
            LOG.infof("Successful rollback for payment %s", payment.getId());
        }

    }
}
