package ru.itmo.soa.dragon.api;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@PreMatching
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private boolean allowed(ContainerRequestContext request) {
        String origin = request.getHeaderString("Origin");
        return origin != null && origin.equals(System.getProperty("frontend.origin", "https://se.ifmo.ru"));
    }

    @Override
    public void filter(ContainerRequestContext request) {
        if (allowed(request) && "OPTIONS".equals(request.getMethod())
                && request.getHeaderString("Access-Control-Request-Method") != null) {
            request.abortWith(Response.noContent().build());
        }
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        if (!allowed(request)) return;
        response.getHeaders().putSingle("Access-Control-Allow-Origin", request.getHeaderString("Origin"));
        response.getHeaders().putSingle("Vary", "Origin");
        if ("OPTIONS".equals(request.getMethod())) {
            response.getHeaders().putSingle("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.getHeaders().putSingle("Access-Control-Allow-Headers", "Accept, Content-Type");
        }
    }
}
