package org.ftn.dto;

import java.time.Instant;
import java.util.UUID;

public record CheckoutErrorDto(UUID id,
                               String message,
                               int status,
                               Instant timestamp) {
}
