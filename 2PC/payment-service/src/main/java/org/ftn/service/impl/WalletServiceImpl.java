package org.ftn.service.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.ftn.dto.WalletResponseDto;
import org.ftn.entity.WalletEntity;
import org.ftn.mapper.WalletMapper;
import org.ftn.repository.WalletRepository;
import org.ftn.service.WalletService;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class WalletServiceImpl implements WalletService {
    private final WalletRepository walletRepository;
    private final WalletMapper walletMapper;

    private static final Logger LOG = Logger.getLogger(WalletServiceImpl.class);

    @Inject
    public WalletServiceImpl(WalletRepository walletRepository,
                             WalletMapper walletMapper) {
        this.walletRepository = walletRepository;
        this.walletMapper = walletMapper;
    }

    @Transactional
    @Override
    public WalletResponseDto createForUser(UUID userId) {
        LOG.infof("Creating wallet for user %s", userId);
        Optional<WalletEntity> optionalWallet = walletRepository
                .find("userId", userId)
                .firstResultOptional();
        if (optionalWallet.isPresent()) {
            LOG.errorf("User %s already has a wallet", userId);
            throw new BadRequestException("User already has a wallet");
        }

        WalletEntity wallet = new WalletEntity();
        wallet.setUserId(userId);
        wallet.setBalance(BigDecimal.ZERO);
        walletRepository.persist(wallet);
        LOG.infof("Successfully created wallet for user %s", userId);
        return walletMapper.toDto(wallet);
    }

    @Override
    public WalletResponseDto getForUser(UUID userId) {
        return walletRepository
                .find("userId", userId)
                .firstResultOptional()
                .map(walletMapper::toDto)
                .orElseThrow(() -> {
                    LOG.errorf("Wallet for user %s not found", userId);
                    return new NotFoundException("Wallet not found");
                });
    }

    @Override
    public WalletResponseDto increaseBalanceForUser(UUID userId, BigDecimal balance) {
        LOG.infof("Increasing balance of wallet for user %s", userId);
        if (balance.compareTo(BigDecimal.ONE) < 0) {
            LOG.errorf("Failed while increasing user balance: Balance must be at least 1", userId);
            throw new BadRequestException("Balance must be at least 1");
        }

        WalletEntity wallet = walletRepository
                .find("userId", userId)
                .firstResultOptional()
                .orElseThrow(() -> {
                    LOG.errorf("Wallet %s not found", userId);
                    return new NotFoundException("Wallet not found");
                });
        wallet.setBalance(wallet.getBalance().add(balance));
        LOG.infof("Successfully increased balance of wallet for user %s", userId);
        return walletMapper.toDto(wallet);
    }

}
