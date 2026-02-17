package org.ftn.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ftn.client.dto.VoteResponseDto;

@ApplicationScoped
public class VoteResponseConverter {
    private final ObjectMapper objectMapper;

    @Inject
    public VoteResponseConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T convert(VoteResponseDto voteResponseDto, Class<T> clazz) {
        try {
            return objectMapper.convertValue(
                    voteResponseDto.body(),
                    clazz
            );
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
