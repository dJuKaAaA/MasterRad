package org.ftn.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class PrepareExceptionMapper implements ExceptionMapper<PrepareException> {
    @Override
    public Response toResponse(PrepareException e) {
        return Response
                .status(e.getStatus())
                .entity(new ErrorResponseDto(e.getMessage(), e.getStatus()))
                .build();
    }
}
