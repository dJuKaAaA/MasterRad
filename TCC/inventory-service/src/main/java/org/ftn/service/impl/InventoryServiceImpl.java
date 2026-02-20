package org.ftn.service.impl;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import org.ftn.constant.ProductStatus;
import org.ftn.dto.*;
import org.ftn.entity.InventoryEntity;
import org.ftn.mapper.InventoryMapper;
import org.ftn.mapper.ProductMapper;
import org.ftn.repository.InventoryRepository;
import org.ftn.service.InventoryService;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class InventoryServiceImpl implements InventoryService {
    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;
    private final ProductMapper productMapper;

    private static final Logger LOG = Logger.getLogger(InventoryServiceImpl.class);

    @Inject
    public InventoryServiceImpl(InventoryRepository inventoryRepository,
                                InventoryMapper inventoryMapper,
                                ProductMapper productMapper) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryMapper = inventoryMapper;
        this.productMapper = productMapper;
    }

    @Override
    public PageResponse<InventoryResponseDto> getAll(UUID merchantId, int page, int size) {
        PanacheQuery<InventoryEntity> pageQuery = inventoryRepository
                .find("product.merchantId = ?1", merchantId)
                .page(page, size);
        int totalPages = pageQuery.pageCount();
        long totalSize = inventoryRepository.count("product.merchantId = ?1", merchantId);

        List<InventoryResponseDto> inventoriesPage = pageQuery
                .list()
                .stream()
                .map(inventoryMapper::toDto)
                .toList();

        LOG.infof("Fetched %d/%d inventories for merchant %s, page %d/%d, page size %d",
                inventoriesPage.size(),
                totalSize,
                merchantId,
                page,
                totalPages,
                size
        );
        return new PageResponse<>(page, totalPages, inventoriesPage.size(), totalSize, inventoriesPage);
    }

    @Override
    public PageResponse<InventoryResponseDto> getAll(int page, int size) {
        PanacheQuery<InventoryEntity> pageQuery = inventoryRepository
                .findAll()
                .page(page, size);
        int totalPages = pageQuery.pageCount();
        long totalSize = inventoryRepository.count();

        List<InventoryResponseDto> inventoriesPage = pageQuery
                .list()
                .stream()
                .map(inventoryMapper::toDto)
                .toList();

        LOG.infof("Fetched %d/%d inventories, page %d/%d, page size %d",
                inventoriesPage.size(),
                totalSize,
                page,
                totalPages,
                size
        );
        return new PageResponse<>(page, totalPages, inventoriesPage.size(), totalSize, inventoriesPage);
    }

    @Override
    public InventoryResponseDto get(UUID id) {
        return inventoryRepository
                .findByIdOptional(id)
                .map(inventoryMapper::toDto)
                .orElseThrow(() -> {
                    LOG.errorf("Inventory %s not found", id);
                    return new NotFoundException("Inventory not found");
                });
    }

    @Override
    public InventoryResponseDto get(UUID id, UUID merchantId) {
        InventoryEntity inventory = inventoryRepository
                .findByIdOptional(id)
                .orElseThrow(() -> {
                    LOG.errorf("Inventory %s not found", id);
                    return new NotFoundException("Inventory not found");
                });
        if (!inventory.getProduct().getMerchantId().equals(merchantId)) {
            LOG.errorf("Merchant id %s mismatch with %s doesn't match for inventory %s",
                    merchantId,
                    inventory.getProduct().getMerchantId(),
                    inventory.getId());
            throw new ForbiddenException("Merchant id doesn't match for inventory");
        }

        LOG.infof("Fetched inventory %s for merchant %s",
                inventory.getId(),
                inventory.getProduct().getId());
        return inventoryMapper.toDto(inventory);
    }

    @Transactional
    @Override
    public InventoryResponseDto create(InventoryRequestDto dto) {
        LOG.infof("Creating inventory");
        if (dto.product().merchantId() == null) {
            throw new BadRequestException("Merchant id omitted");
        }
        InventoryEntity inventory = inventoryMapper.toEntity(dto);
        inventoryRepository.persist(inventory);
        LOG.infof("Created inventory %s", inventory.getId());
        return inventoryMapper.toDto(inventory);
    }

    @Override
    public InventoryResponseDto create(InventoryRequestDto dto, UUID merchantIdFromJwt) {
        LOG.infof("Creating inventory");
        InventoryEntity inventory = inventoryMapper.toEntity(dto);
        inventory.getProduct().setMerchantId(merchantIdFromJwt);
        inventoryRepository.persist(inventory);
        LOG.infof("Created inventory %s", inventory.getId());
        return inventoryMapper.toDto(inventory);
    }

    @Transactional
    @Override
    public void delete(UUID id) {
        LOG.infof("Deleting inventory %s", id);
        InventoryEntity inventory = inventoryRepository
                .findByIdOptional(id)
                .orElseThrow(() -> {
                    Log.errorf("Inventory not found %s", id);
                    return new NotFoundException("Inventory not found");
                });

        inventoryRepository.delete(inventory);
        LOG.infof("Deleted inventory %s successfully", id);
    }

    @Transactional
    @Override
    public ProductResponseDto discontinueProduct(UUID productId) {
        LOG.infof("Discontinuing product %s", productId);
        InventoryEntity inventory = inventoryRepository
                .find("product.id", productId)
                .firstResultOptional()
                .orElseThrow(() -> {
                    LOG.errorf("Product %s not found", productId);
                    return new NotFoundException("Product not found");
                });
        if (inventory.getProduct().getStatus() == ProductStatus.DISCONTINUED) {
            LOG.errorf("Product %s is already discontinued", productId);
            throw new BadRequestException("Product is already discontinued");
        }

        inventory.getProduct().setStatus(ProductStatus.DISCONTINUED);
        inventoryRepository.persist(inventory);
        LOG.infof("Product %s successfully discontinued", productId);
        return productMapper.toDto(inventory.getProduct());
    }

    @Transactional
    @Override
    public InventoryResponseDto replenishStock(UUID id, int amount) {
        LOG.infof("Replenishing stocks for inventory %s", id);
        if (amount < 1) {
            LOG.errorf("Invalid replenish amount: %d", amount);
            throw new BadRequestException("Replenish amount must be at least 1");
        }

        InventoryEntity inventory = inventoryRepository
                .findByIdOptional(id)
                .orElseThrow(() -> {
                    LOG.errorf("Inventory %s not found", id);
                    return new NotFoundException("Inventory not found");
                });

        inventory.increaseAvailableStock(amount);
        inventoryRepository.persist(inventory);
        LOG.infof("Successfully increased stock by %d for inventory %s", amount, inventory.getId());
        return inventoryMapper.toDto(inventory);
    }

    @Transactional
    @Override
    public ProductResponseDto updateProduct(UUID productId, ProductRequestDto productRequestDto) {
        LOG.infof("Updating product %s", productId);
        InventoryEntity inventory = inventoryRepository
                .find("product.id", productId)
                .firstResultOptional()
                .orElseThrow(() -> {
                    LOG.errorf("Product %s not found", productId);
                    return new NotFoundException("Product not found");
                });

        if (inventory.getProduct().getStatus() == ProductStatus.DISCONTINUED) {
            LOG.errorf("Failed to update product %s as it is discontinued", productId);
            throw new BadRequestException("Cannot update discontinued product");
        }

        inventory.getProduct().setName(productRequestDto.name());
        inventory.getProduct().setDescription(productRequestDto.description());
        inventory.getProduct().setPrice(productRequestDto.price());
        inventoryRepository.persist(inventory);

        LOG.infof("Successfully updated product %s", inventory.getProduct().getId());
        return productMapper.toDto(inventory.getProduct());
    }

}
