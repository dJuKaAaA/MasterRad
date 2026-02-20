package org.ftn.service.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.ftn.dto.CreateOrderRequestDto;
import org.ftn.inventory.dto.InventoryResponseDto;
import org.ftn.inventory.service.InventoryService;
import org.ftn.order.dto.OrderRequestDto;
import org.ftn.order.dto.OrderResponseDto;
import org.ftn.order.service.OrderService;
import org.ftn.payment.dto.PaymentRequestDto;
import org.ftn.payment.dto.PaymentResponseDto;
import org.ftn.payment.service.PaymentService;
import org.ftn.service.CoordinatorService;

import java.util.UUID;

@ApplicationScoped
public class CoordinatorServiceImpl implements CoordinatorService {
    private OrderService orderService;
    private InventoryService inventoryService;
    private PaymentService paymentService;

    @Inject
    public CoordinatorServiceImpl(OrderService orderService, InventoryService inventoryService, PaymentService paymentService) {
        this.orderService = orderService;
        this.inventoryService = inventoryService;
        this.paymentService = paymentService;
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
}
