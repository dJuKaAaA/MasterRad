package org.ftn.inventory.service.impl;

import io.quarkus.agroal.DataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.ftn.inventory.constant.ProductStatus;
import org.ftn.inventory.dto.InventoryResponseDto;
import org.ftn.inventory.entity.InventoryEntity;
import org.ftn.inventory.mapper.InventoryMapper;
import org.ftn.inventory.repository.InventoryRepository;
import org.ftn.inventory.service.InventoryService;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class InventoryServiceImpl implements InventoryService {
    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;

    private static final Logger LOG = Logger.getLogger(InventoryServiceImpl.class);

    @Inject
    public InventoryServiceImpl(@DataSource("inventory") InventoryRepository inventoryRepository,
                                InventoryMapper inventoryMapper) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryMapper = inventoryMapper;
    }

    @Transactional
    @Override
    public InventoryResponseDto reserve(UUID productId, int amount) {
        LOG.infof("Reserving product %s", productId);
        InventoryEntity inventory = inventoryRepository
                .find("product.id", productId)
                .firstResultOptional()
                .orElseThrow(() -> {
                    LOG.errorf("Inventory with product %s not found", productId);
                    return new NotFoundException("Inventory not found");
                });

        // Validations
        if (inventory.getAvailableStock() < amount) {
            LOG.errorf("Not enough products in stock for product %s (available: %d, wanted: %d)",
                    inventory.getProduct().getId(),
                    inventory.getAvailableStock(),
                    amount);
            throw new BadRequestException("Not enough products in stock");
        }
        if (inventory.getProduct().getStatus() == ProductStatus.DISCONTINUED) {
            LOG.errorf("Reserving product %s failed because it is discontinued", inventory.getProduct().getId());
            throw new BadRequestException("Product is discontinued");
        }

        inventory.decreaseAvailableStock(amount);
        inventory.setLastUpdatedAt(Instant.now());
        inventoryRepository.persist(inventory);
        LOG.infof("Successfully reserved product %s", inventory.getProduct().getId());
        return inventoryMapper.toDto(inventory);
    }

    @Transactional
    @Override
    public void release(UUID productId, int amount) {
        Optional<InventoryEntity> optionalInventory = inventoryRepository
                .find("product.id", productId)
                .firstResultOptional();
        if (optionalInventory.isPresent()) {
            LOG.infof("Releasing product %s", productId);
            InventoryEntity inventory = optionalInventory.get();
            inventory.increaseAvailableStock(amount);
            inventory.setLastUpdatedAt(Instant.now());
            inventoryRepository.persist(inventory);
            LOG.infof("Successfully released product %s", productId);
        }
    }
}
