package org.ftn.service.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import org.ftn.constant.ProductStatus;
import org.ftn.dto.InventoryResponseDto;
import org.ftn.entity.InventoryEntity;
import org.ftn.mapper.InventoryMapper;
import org.ftn.repository.InventoryRepository;
import org.ftn.service.InventoryTCCService;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class InventoryTCCServiceImpl implements InventoryTCCService {
    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;

    private static final Logger LOG = Logger.getLogger(InventoryTCCServiceImpl.class);

    @Inject
    public InventoryTCCServiceImpl(InventoryRepository inventoryRepository, InventoryMapper inventoryMapper) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryMapper = inventoryMapper;
    }

    @Transactional
    @Override
    public InventoryResponseDto tccTry(UUID productId, int amount) {
        LOG.infof("Reserving product %s", productId);
        Optional<InventoryEntity> optionalInventory = inventoryRepository
                .find("product.id", productId)
                .firstResultOptional();
        if (optionalInventory.isEmpty()) {
            LOG.errorf("Product %s not found", productId);
            throw new NotFoundException("Product not found");
        }

        InventoryEntity inventory = optionalInventory.get();

        // Validations
        if (inventory.getAvailableStock() - inventory.getReservedAmount() < amount) {
            LOG.errorf("All of the products are reserved",
                    inventory.getId(),
                    inventory.getAvailableStock(),
                    amount);
            throw new BadRequestException("All of the products are reserved");
        }

        if (inventory.getAvailableStock() < amount) {
            LOG.errorf("Failed to reserve product due to insufficient stocks (available: %d, wanted: %d)",
                    inventory.getId(),
                    inventory.getAvailableStock(),
                    amount);
            throw new BadRequestException("Insufficient stocks");
        }
        if (inventory.getProduct().getStatus() == ProductStatus.DISCONTINUED) {
            LOG.errorf("Failed to reserve product %s because it is discontinued", inventory.getProduct().getId());
            throw new BadRequestException("Product discontinued");
        }

        inventory.increaseReservedStock(amount);
        inventoryRepository.persist(inventory);
        LOG.infof("Product %s reserved", inventory.getProduct().getId());

        return inventoryMapper.toDto(inventory);
    }

    @Transactional
    @Override
    public void tccCommit(UUID productId, int amount) {
        LOG.infof("Committing reservation for product %s", productId);
        InventoryEntity inventory = inventoryRepository
                .find("product.id", productId)
                .firstResultOptional()
                .orElseThrow(() -> {
                    LOG.errorf("Inventory with product %s not found while attempting commit", productId);
                    return new ServerErrorException("Inventory not found while attempting commit", 500);
                });

        inventory.decreaseAvailableStock(amount);
        inventory.decreaseReservedStock(amount);
        inventory.setLastUpdatedAt(Instant.now());

        LOG.infof("Successful commit for product %s", inventory.getProduct().getId());
        inventoryRepository.persist(inventory);
    }

    @Transactional
    @Override
    public void tccCancel(UUID productId, int amount) {
        Optional<InventoryEntity> optionalInventory = inventoryRepository
                .find("product.id", productId)
                .firstResultOptional();

        if (optionalInventory.isPresent()) {
            LOG.infof("Rolling back reservation for product %s", productId);
            InventoryEntity inventory = optionalInventory.get();
            inventory.decreaseReservedStock(amount);

            inventoryRepository.persist(inventory);
            LOG.infof("Successful rollback for inventory %s", inventory.getId());
        }
    }
}
