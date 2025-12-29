package org.ftn.service.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ServerErrorException;
import org.ftn.constant.ProductStatus;
import org.ftn.constant.Vote;
import org.ftn.dto.ErrorResponseDto;
import org.ftn.dto.VoteResponse;
import org.ftn.entity.InventoryEntity;
import org.ftn.mapper.InventoryWithLockMapper;
import org.ftn.repository.InventoryRepository;
import org.ftn.service.Inventory2PCService;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class Inventory2PCServiceImpl implements Inventory2PCService {
    private final InventoryRepository inventoryRepository;
    private final InventoryWithLockMapper inventoryWithLockMapper;

    private static final Logger LOG = Logger.getLogger(Inventory2PCServiceImpl.class);

    @Inject
    public Inventory2PCServiceImpl(InventoryRepository inventoryRepository,
                                   InventoryWithLockMapper inventoryWithLockMapper) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryWithLockMapper = inventoryWithLockMapper;
    }

    @Override
    public VoteResponse prepare(UUID productId, int amount, UUID txId) {
        LOG.infof("Reserving product %s", productId);
        Optional<InventoryEntity> optionalInventory = inventoryRepository
                .find("product.id", productId)
                .firstResultOptional();
        if (optionalInventory.isEmpty()) {
            LOG.errorf("Product %s not found", productId);
            return new VoteResponse(Vote.NO, new ErrorResponseDto("Product not found", 404));
        }

        InventoryEntity inventory = optionalInventory.get();

        if (inventory.isLocked()) {
            LOG.errorf("Inventory %s is currently locked", inventory.getId());
            return new VoteResponse(Vote.NO, new ErrorResponseDto("Inventory is currently locked", 409));
        }

        // Validations
        if (inventory.getAvailableStock() < amount) {
            LOG.errorf("Failed to reserve product due to insufficient stocks (available: %d, wanted: %d)",
                    inventory.getId(),
                    inventory.getAvailableStock(),
                    amount);
            return new VoteResponse(Vote.NO, new ErrorResponseDto("Insufficient stock", 400));
        }
        if (inventory.getProduct().getStatus() == ProductStatus.DISCONTINUED) {
            LOG.errorf("Failed to reserve product %s because it is discontinued", inventory.getProduct().getId());
            return new VoteResponse(Vote.NO, new ErrorResponseDto("Product discontinued", 400));
        }

        inventory.setLocked(true);
        inventory.setLockId(txId);
        inventoryRepository.persist(inventory);
        LOG.infof("Product %s reserved", inventory.getProduct().getId());
        return new VoteResponse(Vote.YES, inventoryWithLockMapper.toDto(inventory));
    }

    @Override
    public void commit(UUID productId, int amount, UUID lockId) {
        LOG.infof("Committing reservation for product %s", productId);
        InventoryEntity inventory = inventoryRepository
                .find("product.id", productId)
                .firstResultOptional()
                .orElseThrow(() -> {
                    LOG.errorf("Inventory with product %s not found while attempting commit", productId);
                    return new ServerErrorException("Inventory not found while attempting commit", 500);
                });

        inventory.decreaseAvailableStock(amount);
        inventory.setLastUpdatedAt(Instant.now());
        if (!inventory.tryUnlock(lockId)) {
            LOG.errorf("Inventory %s is currently locked", inventory.getId());
            throw new ServerErrorException("Inventory is currently locked", 409);
        }

        LOG.infof("Successful commit for product %s", inventory.getProduct().getId());
        inventoryRepository.persist(inventory);
    }

    @Override
    public void rollback(UUID productId, int amount, UUID lockId) {
        Optional<InventoryEntity> optionalInventory = inventoryRepository
                .find("product.id", productId)
                .firstResultOptional();

        if (optionalInventory.isPresent()) {
            LOG.infof("Rolling back reservation for product %s", productId);
            InventoryEntity inventory = optionalInventory.get();
            if (!inventory.tryUnlock(lockId)) {
                LOG.errorf("Inventory %s is currently locked", inventory.getId());
                throw new ServerErrorException("Inventory is currently locked", 409);
            }

            inventoryRepository.persist(inventory);
            LOG.infof("Successful rollback for inventory %s", inventory.getId());
        }
    }
}
