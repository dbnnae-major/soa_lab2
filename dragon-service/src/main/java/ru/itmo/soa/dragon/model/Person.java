package ru.itmo.soa.dragon.model;

import jakarta.xml.bind.annotation.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.OffsetDateTime;

@Embeddable
@XmlRootElement(name = "person")
@XmlAccessorType(XmlAccessType.FIELD)
public class Person {
    @NotNull
    @Size(min = 1)
    @Column(name = "killer_name", columnDefinition = "text")
    public String name;
    @XmlJavaTypeAdapter(DateTimeAdapter.class)
    @Column(name = "killer_birthday")
    public OffsetDateTime birthday;
    @Enumerated(EnumType.STRING)
    @Column(name = "killer_nationality", columnDefinition = "text")
    public Country nationality;
}
