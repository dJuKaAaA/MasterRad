package org.ftn.dto;

import java.util.UUID;

public record KafkaPaymentErrorDto(UUID paymentId,
                                   UUID productId,
                                   UUID orderId,
                                   UUID sagaId,
                                   Integer amount,
                                   KafkaErrorDto error) {
}
