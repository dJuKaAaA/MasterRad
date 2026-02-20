package org.ftn.payment.dto;

import java.util.Collection;

public record PageResponse<T>(int page,
                              int totalPages,
                              long size,
                              long totalSize,
                              Collection<T> items) {
}
