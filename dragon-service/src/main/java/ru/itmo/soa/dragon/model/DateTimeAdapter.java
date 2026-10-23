package ru.itmo.soa.dragon.model;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

public class DateTimeAdapter extends XmlAdapter<String, OffsetDateTime> {
    @Override
    public OffsetDateTime unmarshal(String value) {
        if (value == null) return null;
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException e) {
            return LocalDateTime.parse(value).atOffset(ZoneOffset.UTC);
        }
    }

    @Override
    public String marshal(OffsetDateTime value) {
        return value == null ? null : value.toString();
    }
}
