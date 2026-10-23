package ru.itmo.soa.dragon.query;

import jakarta.ws.rs.core.MultivaluedMap;
import ru.itmo.soa.dragon.api.ApiException;
import ru.itmo.soa.dragon.model.Color;
import ru.itmo.soa.dragon.model.Country;
import ru.itmo.soa.dragon.model.DateTimeAdapter;
import ru.itmo.soa.dragon.model.Dragon;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class DragonQuery {
    private final List<Predicate<Dragon>> filters = new ArrayList<>();
    private Comparator<Dragon> sortOrder;
    private final long page;
    private final long pageSize;

    public DragonQuery(MultivaluedMap<String, String> parameters) {
        page = readLong("page", parameters.getFirst("page"), 0);
        pageSize = readLong("pageSize", parameters.getFirst("pageSize"), 10);
        if (page < 0) throw new ApiException(422, "Параметр page должен быть не меньше 0.");
        if (pageSize <= 0) throw new ApiException(422, "Параметр pageSize должен быть больше 0.");

        for (String field : List.of("id", "name", "coordinates.x", "coordinates.y", "creationDate", "age",
                "wingspan", "speaking", "color", "killer.name", "killer.birthday", "killer.nationality")) {
            if (parameters.containsKey(field)) addFilter(field, parameters.getFirst(field));
        }

        if (parameters.containsKey("sort")) {
            for (String value : parameters.get("sort")) {
                String[] parts = value.split(",", -1);
                if (parts.length != 2 || (!parts[1].equals("asc") && !parts[1].equals("desc"))) {
                    throw invalidSort();
                }
                Comparator<Dragon> next = comparator(parts[0], parts[1].equals("desc"));
                sortOrder = sortOrder == null ? next : sortOrder.thenComparing(next);
            }
        }
    }

    public List<Dragon> apply(List<Dragon> dragons) {
        List<Dragon> result = new ArrayList<>(dragons);
        for (Predicate<Dragon> filter : filters) {
            result.removeIf(dragon -> !filter.test(dragon));
        }

        if (sortOrder != null) result.sort(sortOrder);

        if (page > result.size() / pageSize) return List.of();
        int start = (int) (page * pageSize);
        int end = (int) Math.min(result.size(), start + pageSize);
        return result.subList(start, end);
    }

    private void addFilter(String field, String value) {
        switch (field) {
            case "id" -> equal(dragon -> dragon.id, number(field, value, 0));
            case "name" -> equal(dragon -> dragon.name, text(field, value));
            case "coordinates.x" -> equal(dragon -> dragon.coordinates.x, number(field, value, -843));
            case "coordinates.y" -> {
                float expected = coordinate(value);
                filters.add(dragon -> dragon.coordinates.y == expected);
            }
            case "creationDate" -> equal(dragon -> dragon.creationDate.toInstant(), date(field, value));
            case "age" -> equal(dragon -> dragon.age, intNumber(field, value));
            case "wingspan" -> equal(dragon -> dragon.wingspan, number(field, value, 0));
            case "speaking" -> {
                if (!"true".equals(value) && !"false".equals(value)) {
                    throw new ApiException(400, "Параметр speaking должен иметь значение true или false.");
                }
                equal(dragon -> dragon.speaking, Boolean.valueOf(value));
            }
            case "color" -> equal(dragon -> dragon.color, enumeration(Color.class, field, value));
            case "killer.name" -> equal(dragon -> dragon.killer == null ? null : dragon.killer.name, text(field, value));
            case "killer.birthday" -> equal(dragon -> dragon.killer == null || dragon.killer.birthday == null
                    ? null : dragon.killer.birthday.toInstant(), date(field, value));
            case "killer.nationality" -> equal(dragon -> dragon.killer == null ? null : dragon.killer.nationality,
                    enumeration(Country.class, field, value));
        }
    }

    private <T> void equal(Function<Dragon, T> field, T value) {
        filters.add(dragon -> value.equals(field.apply(dragon)));
    }

    private long readLong(String name, String value, long defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new ApiException(400, "Параметр " + name + " должен быть целым числом.");
        }
    }

    private long number(String name, String value, long minExclusive) {
        long result = readLong(name, value, 0);
        if (result <= minExclusive) {
            throw new ApiException(422, "Параметр " + name + " должен быть больше " + minExclusive + ".");
        }
        return result;
    }

    private int intNumber(String name, String value) {
        int result;
        try {
            result = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ApiException(400, "Параметр " + name + " выходит за диапазон int32 или не является целым числом.");
        }
        if (result <= 0) throw new ApiException(422, "Параметр " + name + " должен быть больше 0.");
        return result;
    }

    private String text(String name, String value) {
        if (value == null || value.isEmpty()) {
            throw new ApiException(422, "Параметр " + name + " не может быть пустой строкой.");
        }
        return value;
    }

    private float coordinate(String value) {
        try {
            float result = new BigDecimal(value).floatValue();
            if (!Float.isFinite(result)) throw new NumberFormatException();
            return result;
        } catch (NumberFormatException e) {
            throw new ApiException(400, "Параметр coordinates.y должен быть числом формата float.");
        }
    }

    private Instant date(String name, String value) {
        try {
            return new DateTimeAdapter().unmarshal(value).toInstant();
        } catch (DateTimeParseException e) {
            throw new ApiException(400, "Параметр " + name + " должен быть датой в формате date-time.");
        }
    }

    private <T extends Enum<T>> T enumeration(Class<T> type, String name, String value) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            String allowed = String.join(", ", Arrays.stream(type.getEnumConstants()).map(Enum::name).toList());
            throw new ApiException(400, "Параметр " + name + " должен иметь одно из значений " + allowed + ".");
        }
    }

    private Comparator<Dragon> comparator(String field, boolean descending) {
        return switch (field) {
            case "id" -> compare(dragon -> dragon.id, descending);
            case "name" -> compare(dragon -> dragon.name, descending);
            case "coordinates.x" -> compare(dragon -> dragon.coordinates.x, descending);
            case "coordinates.y" -> compare(dragon -> dragon.coordinates.y, descending);
            case "creationDate" -> compare(dragon -> dragon.creationDate.toInstant(), descending);
            case "age" -> compare(dragon -> dragon.age, descending);
            case "wingspan" -> compare(dragon -> dragon.wingspan, descending);
            case "speaking" -> compare(dragon -> dragon.speaking, descending);
            case "color" -> compare(dragon -> dragon.color == null ? null : dragon.color.name(), descending);
            case "killer.name" -> compare(dragon -> dragon.killer == null ? null : dragon.killer.name, descending);
            case "killer.birthday" -> compare(dragon -> dragon.killer == null || dragon.killer.birthday == null
                    ? null : dragon.killer.birthday.toInstant(), descending);
            case "killer.nationality" -> compare(dragon -> dragon.killer == null || dragon.killer.nationality == null
                    ? null : dragon.killer.nationality.name(), descending);
            default -> throw invalidSort();
        };
    }

    private ApiException invalidSort() {
        return new ApiException(400, "Параметр sort должен иметь формат field,asc или field,desc.");
    }

    private static <T extends Comparable<? super T>> Comparator<Dragon> compare(
            Function<Dragon, T> field, boolean descending) {
        Comparator<T> values = Comparator.naturalOrder();
        if (descending) values = values.reversed();
        return Comparator.comparing(field, Comparator.nullsLast(values));
    }
}
