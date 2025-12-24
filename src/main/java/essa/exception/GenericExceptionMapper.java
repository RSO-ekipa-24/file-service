package essa.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.Map;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {
    
    private static final Logger LOG = Logger.getLogger(GenericExceptionMapper.class);
    
    @Override
    public Response toResponse(Exception e) {
        LOG.error("Unhandled exception", e);
        
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of(
                        "error", "Internal server error",
                        "message", e.getMessage(),
                        "type", e.getClass().getSimpleName()))
                .build();
    }
}