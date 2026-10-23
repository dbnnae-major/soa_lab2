
package ru.itmo.soa.dragon.api;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import ru.itmo.soa.dragon.model.ProblemDetails;

import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<Exception> {
    @Context
    UriInfo uri;

    @Override
    public Response toResponse(Exception exception) {
        int status = 500;
        String detail = null;
        Response original = null;

        if (exception instanceof ApiException api) {
            status = api.getStatus();
            detail = api.getMessage();
        } else if (exception instanceof WebApplicationException web) {
            original = web.getResponse();
            status = original.getStatus();
        } else if (databaseUnavailable(exception)) {
            status = 503;
        }

        if (detail == null) {
            detail = switch (status) {
                case 400 -> "Некорректный HTTP-запрос.";
                case 404 -> "Запрошенный API-ресурс не существует.";
                case 405 -> "HTTP-метод не разрешён для запрошенного ресурса.";
                case 406 -> "Успешный ответ предоставляется только в application/xml.";
                case 413 -> "Размер XML-тела запроса превышает установленный сервером предел.";
                case 415 -> "Content-Type должен быть application/xml.";
                case 422 -> "Данные Dragon нарушают ограничения модели.";
                case 503 -> "Хранилище временно недоступно.";
                default -> "Внутренняя ошибка обработки запроса.";
            };
        }

        ProblemDetails error = new ProblemDetails();
        error.status = status;
        error.title = switch (status) {
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 406 -> "Not Acceptable";
            case 413 -> "Content Too Large";
            case 415 -> "Unsupported Media Type";
            case 422 -> "Unprocessable Content";
            case 503 -> "Service Unavailable";
            default -> "Internal Server Error";
        };
        error.detail = detail;
        error.instance = uri.getRequestUri().getRawPath();
        String query = uri.getRequestUri().getRawQuery();
        if (query != null) error.instance += "?" + query;

        Response.ResponseBuilder response = Response.status(status)
                .type("application/problem+xml")
                .entity(error);

        if (status == 405 && original != null) response.allow(original.getAllowedMethods());
        if (status == 503) response.header("Retry-After", "30");

        return response.build();
    }

    private boolean databaseUnavailable(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLTransientConnectionException) return true;
            if (cause instanceof SQLException sql && sql.getSQLState() != null
                    && sql.getSQLState().startsWith("08")) return true;
        }
        return false;
    }
}
