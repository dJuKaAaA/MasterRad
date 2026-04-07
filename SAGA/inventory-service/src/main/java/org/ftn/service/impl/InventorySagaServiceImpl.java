package org.ftn.service.impl;

import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.ftn.constant.ProductStatus;
import org.ftn.dto.*;
import org.ftn.entity.InventoryEntity;
import org.ftn.mapper.InventoryMapper;
import org.ftn.repository.InventoryRepository;
import org.ftn.service.InventorySagaService;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class InventorySagaServiceImpl implements InventorySagaService {
    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;
    private final Emitter<KafkaInventoryResponseDto> responseEmitter;
    private final Emitter<KafkaInventoryErrorDto> errorEmitter;

    private static final Logger LOG = Logger.getLogger(InventorySagaServiceImpl.class);

    @Inject
    public InventorySagaServiceImpl(InventoryRepository inventoryRepository,
                                    InventoryMapper inventoryMapper,
                                    @Channel("inventory-service-response") Emitter<KafkaInventoryResponseDto> responseEmitter,
                                    @Channel("inventory-service-error") Emitter<KafkaInventoryErrorDto> errorEmitter) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryMapper = inventoryMapper;
        this.responseEmitter = responseEmitter;
        this.errorEmitter = errorEmitter;
    }

    @Transactional
    @Override
    public InventoryResponseDto reserve(UUID productId, int amount) {
        LOG.infof("Reserving product %s", productId);

        if (productId == null) {
            LOG.error("Product id omitted");
            throw new BadRequestException("Product id omitted");
        }

        InventoryEntity inventory = inventoryRepository
                .find("product.id", productId)
                .firstResultOptional()
                .orElseThrow(() -> {
                    LOG.errorf("Inventory with product %s not found", productId);
                    return new NotFoundException("Inventory not found");
                });

        if (amount < 1) {
            LOG.errorf("Invalid amount value: %d", amount);
            throw new BadRequestException("Amount cannot be less than 1");
        }
        if (inventory.getAvailableStock() < amount) {
            LOG.errorf("Failed to reserve product %s due to insufficient stocks (available: %d, wanted: %d)",
                    inventory.getProduct().getId(),
                    inventory.getAvailableStock(),
                    amount);
            throw new BadRequestException("Insufficient stocks");
        }
        if (inventory.getProduct().getStatus() == ProductStatus.DISCONTINUED) {
            LOG.errorf("Failed to reserve product %s because it is discontinued", inventory.getProduct().getId());
            throw new BadRequestException("Product is discontinued");
        }

        inventory.decreaseAvailableStock(amount);
        inventory.setLastUpdatedAt(Instant.now());
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
            LOG.infof("Successfully released product %s", productId);
        }
    }

    @Incoming("inventory-service-commit")
    @Blocking
    public CompletionStage<Void> reserve(Message<KafkaInventoryRequestDto> msg) {
        UUID productId = msg.getPayload().productReserveRequestDto().productId();
        int quantity = msg.getPayload().productReserveRequestDto().quantity();
        try {
            InventoryResponseDto inventoryResponseDto = reserve(productId, quantity);
            KafkaInventoryResponseDto payload = new KafkaInventoryResponseDto(
                    msg.getPayload().userId(),
                    msg.getPayload().sagaId(),
                    msg.getPayload().orderId(),
                    quantity,
                    inventoryResponseDto,
                    null
            );
            responseEmitter.send(Message.of(payload));
        } catch (ClientErrorException e) {
            KafkaInventoryResponseDto payload = new KafkaInventoryResponseDto(
                    msg.getPayload().userId(),
                    msg.getPayload().sagaId(),
                    msg.getPayload().orderId(),
                    quantity,
                    null,
                    new KafkaErrorDto(e.getMessage(), e.getResponse().getStatus())
            );
            responseEmitter.send(Message.of(payload));
        }
        return msg.ack();
    }

    @Incoming("inventory-service-rollback")
    @Blocking
    public CompletionStage<Void> release(Message<KafkaInventoryErrorDto> msg) {
        UUID productId = msg.getPayload().productId();
        int quantity = msg.getPayload().amount();
        release(productId, quantity);
        errorEmitter.send(msg);
        return msg.ack();
    }
}
