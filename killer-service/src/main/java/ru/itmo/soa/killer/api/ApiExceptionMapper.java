package ru.itmo.soa.killer.api;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import ru.itmo.soa.killer.model.ProblemDetails;

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
        } else {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof SQLTransientConnectionException) {
                    status = 503;
                    break;
                }
                if (cause instanceof SQLException sql) {
                    String state = sql.getSQLState();
                    if ("23505".equals(state)) {
                        status = 409;
                        detail = "Команда с указанным team-id уже существует.";
                        break;
                    }
                    if (state != null && state.startsWith("08")) {
                        status = 503;
                        break;
                    }
                }
            }
        }

        if (detail == null) {
            detail = switch (status) {
                case 400 -> "Некорректный HTTP-запрос.";
                case 404 -> "Запрошенный API-ресурс не существует.";
                case 405 -> "HTTP-метод не разрешён для запрошенного ресурса.";
                case 406 -> "Успешный ответ предоставляется только в application/xml.";
                case 409 -> "Команда с указанным team-id уже существует.";
                case 422 -> "Параметр dragon-id должен быть больше 0.";
                case 502 -> "Dragon Collection Service вернул некорректный ответ.";
                case 503 -> "Хранилище временно недоступно.";
                case 504 -> "Dragon Collection Service не ответил в течение настроенного времени ожидания.";
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
            case 409 -> "Conflict";
            case 422 -> "Unprocessable Content";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            case 504 -> "Gateway Timeout";
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
}
