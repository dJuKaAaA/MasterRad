package org.ftn.client.exception;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

public class RestClientExceptionMapper implements ResponseExceptionMapper<ClientErrorException> {

    @Override
    public ClientErrorException toThrowable(Response response) {
        return new ClientErrorException(response.readEntity(String.class), response.getStatus());
    }
}
