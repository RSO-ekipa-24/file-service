package essa.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

@Provider
public class WebAppExceptionMapper implements ExceptionMapper<WebApplicationException> {
    @Override
    public Response toResponse(WebApplicationException e) {
        Response orig = e.getResponse();
        Object entity = orig.getEntity();
        if (entity == null) {
            entity = Map.of(
                "error", orig.getStatusInfo().getReasonPhrase(),
                "message", e.getMessage()
            );
        }
        return Response.fromResponse(orig)
                .entity(entity)
                .build();
    }
}