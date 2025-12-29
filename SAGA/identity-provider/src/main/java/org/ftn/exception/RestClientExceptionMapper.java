package org.ftn.exception;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.ftn.service.impl.AuthServiceImpl;
import org.jboss.logging.Logger;

public class RestClientExceptionMapper implements ResponseExceptionMapper<ClientErrorException> {
    private static final Logger LOG = Logger.getLogger(RestClientExceptionMapper.class);

    @Override
    public ClientErrorException toThrowable(Response response) {
        LOG.errorf("Error: %s with status: %d", response.readEntity(String.class), response.getStatus());
        return new ClientErrorException(response.readEntity(String.class), response.getStatus());
    }
}
