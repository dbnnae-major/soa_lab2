package ru.itmo.soa.dragon.model;

import jakarta.xml.bind.annotation.*;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.OffsetDateTime;

@Entity
@Table(name = "soa_lab2_dragon")
@XmlRootElement(name = "dragon")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"id", "name", "coordinates", "creationDate", "age", "wingspan", "speaking", "color", "killer"})
public class Dragon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    public Long id;
    @NotNull
    @Size(min = 1)
    @Column(nullable = false, columnDefinition = "text")
    public String name;
    @NotNull
    @Valid
    @Embedded
    public Coordinates coordinates;
    @XmlJavaTypeAdapter(DateTimeAdapter.class)
    @NotNull
    @Column(name = "creation_date", nullable = false, updatable = false)
    public OffsetDateTime creationDate;
    @Positive
    public Integer age;
    @NotNull
    @Positive
    @Column(nullable = false)
    public Long wingspan;
    @NotNull
    @Column(nullable = false)
    public Boolean speaking;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    public Color color;
    @Valid
    @Embedded
    public Person killer;

    @PrePersist
    private void generateCreationDate() {
        creationDate = OffsetDateTime.now(java.time.ZoneOffset.UTC).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }
}
