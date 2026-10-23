package ru.itmo.soa.dragon.api;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Provider
public class XmlBodySizeFilter implements ContainerRequestFilter {
    private static final int MAX_BYTES = 1024 * 1024;

    @Override
    public void filter(ContainerRequestContext request) {
        if (!request.getMethod().equals("POST") && !request.getMethod().equals("PUT")) return;
        if (request.getMediaType() == null || !request.getMediaType().isCompatible(MediaType.APPLICATION_XML_TYPE)) return;

        try {
            byte[] body = request.getEntityStream().readNBytes(MAX_BYTES + 1);

            if (body.length > MAX_BYTES) {
                throw new ApiException(413, "Размер XML-тела запроса превышает предел " + MAX_BYTES + " байт.");
            }

            request.setEntityStream(new ByteArrayInputStream(body));
        } catch (IOException e) {
            throw new BadRequestException();
        }
    }
}
