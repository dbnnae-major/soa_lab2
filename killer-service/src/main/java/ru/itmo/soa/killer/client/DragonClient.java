package ru.itmo.soa.killer.client;

import jakarta.enterprise.context.ApplicationScoped;
import ru.itmo.soa.killer.api.ApiException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

@ApplicationScoped
public class DragonClient {
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public void kill(long dragonId) {
        String baseUrl = System.getProperty("dragon.api.url");
        if (baseUrl == null || !baseUrl.startsWith("https://")) {
            throw new ApiException(500, "Не настроен HTTPS-адрес Dragon Collection Service.");
        }
        while (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        String url = baseUrl + "/dragons/" + dragonId;
        HttpResponse<String> result = request(url, "GET");

        if (result.statusCode() == 404) {
            throw new ApiException(404, "Запрошенный API-ресурс не существует.");
        }
        if (result.statusCode() != 200) {
            throw unexpectedStatus(result.statusCode());
        }
        String contentType = result.headers().firstValue("Content-Type").orElse("");
        if (!contentType.split(";", 2)[0].trim().equalsIgnoreCase("application/xml")) {
            throw new ApiException(502, "Dragon Collection Service вернул ответ не в формате application/xml.");
        }
        if (hasKiller(result.body(), dragonId)) {
            throw new ApiException(409, "Операция не может быть выполнена: у указанного Dragon уже задан killer.");
        }

        result = request(url, "DELETE");
        if (result.statusCode() == 204) return;
        if (result.statusCode() == 404) {
            throw new ApiException(404, "Запрошенный API-ресурс не существует.");
        }
        throw unexpectedStatus(result.statusCode());
    }

    private HttpResponse<String> request(String url, String method) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/xml")
                    .method(method, HttpRequest.BodyPublishers.noBody())
                    .build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw new ApiException(504, "Dragon Collection Service не ответил в течение настроенного времени ожидания.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(503, "Соединение с Dragon Collection Service было прервано.");
        } catch (java.io.IOException e) {
            throw new ApiException(503, "Соединение с Dragon Collection Service установить не удалось.");
        }
    }

    private ApiException unexpectedStatus(int status) {
        if (status == 503) {
            return new ApiException(503, "Dragon Collection Service временно недоступен.");
        }
        return new ApiException(502, "Dragon Collection Service вернул статус, не предусмотренный контрактом межсервисного вызова.");
    }

    static boolean hasKiller(String xml, long dragonId) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            Element root = factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)))
                    .getDocumentElement();
            if (!"dragon".equals(root.getTagName()) || root.getNamespaceURI() != null) {
                throw new IllegalArgumentException();
            }
            Element id = null;
            for (int i = 0; i < root.getChildNodes().getLength(); i++) {
                if (root.getChildNodes().item(i) instanceof Element child && "id".equals(child.getTagName())) {
                    if (id != null) throw new IllegalArgumentException();
                    id = child;
                }
            }
            if (id == null || Long.parseLong(id.getTextContent().trim()) != dragonId) {
                throw new IllegalArgumentException();
            }
            for (int i = 0; i < root.getChildNodes().getLength(); i++) {
                if (root.getChildNodes().item(i) instanceof Element child && "killer".equals(child.getTagName())) {
                    String nil = child.getAttributeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "nil");
                    return !"true".equals(nil) && !"1".equals(nil);
                }
            }
            return false;
        } catch (Exception e) {
            throw new ApiException(502, "Dragon Collection Service вернул XML, который Killer Service не смог разобрать.");
        }
    }
}
