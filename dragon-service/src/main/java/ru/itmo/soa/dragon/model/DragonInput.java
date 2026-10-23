package ru.itmo.soa.dragon.model;

import jakarta.xml.bind.annotation.*;

@XmlRootElement(name = "dragon")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"name", "coordinates", "age", "wingspan", "speaking", "color", "killer"})
public class DragonInput {
    public String name;
    public Coordinates coordinates;
    public Integer age;
    public Long wingspan;
    public Boolean speaking;
    public Color color;
    public Person killer;
}
