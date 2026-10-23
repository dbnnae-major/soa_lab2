package ru.itmo.soa.dragon.model;

import jakarta.xml.bind.annotation.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Embeddable
@XmlRootElement(name = "coordinates")
@XmlAccessorType(XmlAccessType.FIELD)
public class Coordinates {
    @NotNull
    @Min(-842)
    @Column(name = "coordinates_x", nullable = false)
    public Long x;
    @NotNull
    @Column(name = "coordinates_y", nullable = false)
    public Float y;
}
