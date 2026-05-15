package org.ftn.service.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.ftn.constant.CoordinatorTransactionState;
import org.ftn.dto.CheckoutDto;
import org.ftn.dto.CreateOrderRequestDto;
import org.ftn.entity.CoordinatorTransactionEntity;
import org.ftn.inventory.dto.InventoryResponseDto;
import org.ftn.inventory.service.InventoryService;
import org.ftn.order.dto.OrderRequestDto;
import org.ftn.order.dto.OrderResponseDto;
import org.ftn.order.service.OrderService;
import org.ftn.payment.dto.PaymentRequestDto;
import org.ftn.payment.dto.PaymentResponseDto;
import org.ftn.payment.service.PaymentService;
import org.ftn.repository.CoordinatorTransactionRepository;
import org.ftn.service.CoordinatorService;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CoordinatorServiceImpl implements CoordinatorService {
    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final CoordinatorTransactionRepository txRepository;

    @Inject
    public CoordinatorServiceImpl(OrderService orderService, InventoryService inventoryService, PaymentService paymentService,
                                  CoordinatorTransactionRepository txRepository) {
        this.orderService = orderService;
        this.inventoryService = inventoryService;
        this.paymentService = paymentService;
        this.txRepository = txRepository;
    }

    @Transactional
    @Override
    public void createOrder(CreateOrderRequestDto dto, UUID userId) {
        OrderResponseDto orderResponseDto = orderService.createOrder(
                new OrderRequestDto(
                        dto.productId(),
                        dto.amount(),
                        userId
                )
        );
        InventoryResponseDto inventoryResponseDto = inventoryService.reserve(
                dto.productId(),
                dto.amount()
        );
        PaymentResponseDto paymentResponseDto = paymentService.process(
                new PaymentRequestDto(
                        inventoryResponseDto.product().price(),
                        dto.productId(),
                        dto.amount(),
                        userId
                )
        );
    }

    @Transactional
    @Override
    public UUID update(UUID txId, CoordinatorTransactionState state) {
        Optional<CoordinatorTransactionEntity> optionalTx = txRepository
                .findByIdOptional(txId);
        CoordinatorTransactionEntity tx;

        if (optionalTx.isPresent()) {
            tx = optionalTx.get();
        } else {
            tx = new CoordinatorTransactionEntity();
            txRepository.persist(tx);
        }

        tx.setState(state);

        return tx.getId();
    }

    @Override
    public CheckoutDto get(UUID txId) {
        return txRepository
                .findByIdOptional(txId)
                .map(tx -> new CheckoutDto(tx.getId(), tx.getState()))
                .orElseThrow(() -> new NotFoundException("Checkout not found"));
    }

    @Override
    public CoordinatorTransactionState getState(UUID txId) {
        return txRepository
                .findByIdOptional(txId)
                .orElseThrow(() -> new NotFoundException("Checkout not found"))
                .getState();
    }
}
