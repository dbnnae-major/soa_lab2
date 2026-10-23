package ru.itmo.soa.dragon.xml;

import jakarta.enterprise.context.ApplicationScoped;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import ru.itmo.soa.dragon.api.ApiException;
import ru.itmo.soa.dragon.model.Color;
import ru.itmo.soa.dragon.model.Coordinates;
import ru.itmo.soa.dragon.model.Country;
import ru.itmo.soa.dragon.model.DateTimeAdapter;
import ru.itmo.soa.dragon.model.DragonInput;
import ru.itmo.soa.dragon.model.Person;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.format.DateTimeParseException;

@ApplicationScoped
public class DragonXmlReader {
    public DragonInput read(String xml) {
        if (xml == null || xml.isBlank()) {
            throw new ApiException(400, "Требуется XML-тело запроса.");
        }

        Element root = parse(xml);
        if (!root.getTagName().equals("dragon") || root.getNamespaceURI() != null) {
            throw new ApiException(400, "Корневым элементом XML должен быть <dragon>.");
        }
        String nil = root.getAttributeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "nil");
        if (nil.equals("true") || nil.equals("1")) {
            throw new ApiException(422, "Объект Dragon не может быть null.");
        }

        DragonInput input = new DragonInput();
        input.name = text(root, "name");
        input.coordinates = coordinates(child(root, "coordinates"));
        input.age = age(text(root, "age"));
        input.wingspan = longValue(text(root, "wingspan"), "wingspan");
        input.speaking = speaking(text(root, "speaking"));
        input.color = color(text(root, "color"));
        input.killer = killer(child(root, "killer"));
        validate(input);
        return input;
    }

    private Element parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)))
                    .getDocumentElement();
        } catch (SAXException | IOException e) {
            throw new ApiException(400, "XML-документ не удалось разобрать: нарушена структура XML.");
        } catch (ParserConfigurationException e) {
            throw new ApiException(500, "Не удалось настроить обработку XML.");
        }
    }

    private Coordinates coordinates(Element element) {
        if (element == null) return null;
        Coordinates result = new Coordinates();
        result.x = longValue(text(element, "x"), "coordinates.x");
        String y = text(element, "y");
        if (y != null) {
            try {
                result.y = new BigDecimal(y.trim()).floatValue();
                if (!Float.isFinite(result.y)) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                throw new ApiException(422, "Поле coordinates.y должно быть числом формата float.");
            }
        }
        return result;
    }

    private Integer age(String value) {
        Long result = longValue(value, "age");
        if (result == null) return null;
        if (result < Integer.MIN_VALUE || result > Integer.MAX_VALUE) {
            throw new ApiException(422, "Поле age выходит за диапазон int32.");
        }
        return result.intValue();
    }

    private Boolean speaking(String value) {
        if (value == null) return null;
        String text = value.trim();
        if (!text.equals("true") && !text.equals("false")) {
            throw new ApiException(422, "Поле speaking должно иметь значение true или false.");
        }
        return Boolean.valueOf(text);
    }

    private Color color(String value) {
        if (value == null) return null;
        try {
            return Color.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new ApiException(422, "Поле color должно иметь одно из значений GREEN, BLUE, YELLOW, ORANGE.");
        }
    }

    private Person killer(Element element) {
        if (element == null) return null;
        Person result = new Person();
        result.name = text(element, "name");
        String birthday = text(element, "birthday");
        if (birthday != null) {
            try {
                result.birthday = new DateTimeAdapter().unmarshal(birthday.trim());
            } catch (DateTimeParseException e) {
                throw new ApiException(422, "Поле killer.birthday должно быть датой в формате date-time.");
            }
        }
        String nationality = text(element, "nationality");
        if (nationality != null) {
            try {
                result.nationality = Country.valueOf(nationality);
            } catch (IllegalArgumentException e) {
                throw new ApiException(422, "Поле killer.nationality должно иметь одно из значений FRANCE, ITALY, JAPAN.");
            }
        }
        return result;
    }

    private void validate(DragonInput input) {
        if (input.name == null || input.name.isEmpty()) {
            throw new ApiException(422, "Поле name обязательно и не может быть пустым.");
        }
        if (input.coordinates == null || input.coordinates.x == null || input.coordinates.y == null) {
            throw new ApiException(422, "Обязательны coordinates.x и coordinates.y.");
        }
        if (input.coordinates.x <= -843) {
            throw new ApiException(422, "Поле coordinates.x должно быть больше -843.");
        }
        if (input.wingspan == null || input.wingspan <= 0) {
            throw new ApiException(422, "Поле wingspan должно быть больше 0.");
        }
        if (input.speaking == null) {
            throw new ApiException(422, "Поле speaking обязательно.");
        }
        if (input.age != null && input.age <= 0) {
            throw new ApiException(422, "Поле age должно быть больше 0.");
        }
        if (input.killer != null && (input.killer.name == null || input.killer.name.isEmpty())) {
            throw new ApiException(422, "Поле killer.name обязательно и не может быть пустым.");
        }
    }

    private Element child(Element parent, String name) {
        Element found = null;
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && element.getTagName().equals(name)) {
                if (found != null) {
                    throw new ApiException(422, "Поле " + name + " передано несколько раз.");
                }
                found = element;
            }
        }
        if (found == null) return null;

        String nil = found.getAttributeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "nil");
        if (nil.equals("true") || nil.equals("1")) {
            if (!found.getTextContent().isBlank()) {
                throw new ApiException(422, "Поле " + name + " с xsi:nil не должно содержать значение.");
            }
            if (found.getElementsByTagName("*").getLength() > 0) {
                throw new ApiException(422, "Поле " + name + " с xsi:nil должно быть пустым.");
            }
            return null;
        }
        if (!nil.isEmpty() && !nil.equals("false") && !nil.equals("0")) {
            throw new ApiException(422, "Некорректное значение xsi:nil для поля " + name + ".");
        }
        return found;
    }

    private String text(Element parent, String name) {
        Element element = child(parent, name);
        if (element == null) return null;
        if (element.getElementsByTagName("*").getLength() > 0) {
            throw new ApiException(422, "Поле " + name + " должно содержать скалярное значение.");
        }
        return element.getTextContent();
    }

    private Long longValue(String value, String name) {
        if (value == null) return null;
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new ApiException(422, "Поле " + name + " должно быть целым числом формата int64.");
        }
    }
}
