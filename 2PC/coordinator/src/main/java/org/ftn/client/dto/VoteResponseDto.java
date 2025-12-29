package org.ftn.client.dto;

public record VoteResponseDto(Vote vote,
                              Object body) {

    public static OrderResponseDto getOrderResponse(VoteResponseDto voteResponseDto) {
        if (voteResponseDto.body() instanceof OrderResponseDto inventory) {
            return inventory;
        }

        return null;
    }

    public static InventoryResponseDto getInventoryResponse(VoteResponseDto voteResponseDto) {
        if (voteResponseDto.body() instanceof InventoryResponseDto response) {
            return response;
        }

        return null;

    }
    public static PaymentResponseDto getPaymentResponse(VoteResponseDto voteResponseDto) {
        if (voteResponseDto.body() instanceof PaymentResponseDto response) {
            return response;
        }

        return null;
    }

    public static  ErrorResponseDto getErrorResponse(VoteResponseDto voteResponseDto) {
        if (voteResponseDto.body() instanceof ErrorResponseDto response) {
            return response;
        }

        return null;
    }
}
