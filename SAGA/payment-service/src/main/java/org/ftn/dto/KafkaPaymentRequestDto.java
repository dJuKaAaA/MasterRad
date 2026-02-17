package org.ftn.dto;

import java.util.UUID;

public record KafkaPaymentRequestDto(UUID userId,
                                     UUID sagaId,
                                     UUID orderId,
                                     UUID productId,
                                     Integer amount,
                                     PaymentRequestDto paymentRequestDto) {
}
