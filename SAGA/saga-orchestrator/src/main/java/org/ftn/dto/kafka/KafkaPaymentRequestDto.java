package org.ftn.dto.kafka;

import org.ftn.client.dto.PaymentRequestDto;

import java.util.UUID;

public record KafkaPaymentRequestDto(UUID userId,
                                     UUID sagaId,
                                     UUID orderId,
                                     UUID productId,
                                     Integer amount,
                                     PaymentRequestDto paymentRequestDto) {
}
