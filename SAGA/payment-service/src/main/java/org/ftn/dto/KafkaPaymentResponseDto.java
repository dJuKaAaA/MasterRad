package org.ftn.dto;

import java.util.UUID;

public record KafkaPaymentResponseDto(UUID userId,
                                      UUID sagaId,
                                      UUID orderId,
                                      UUID productId,
                                      Integer amount,
                                      PaymentResponseDto paymentResponseDto,
                                      KafkaErrorDto error) {
}
