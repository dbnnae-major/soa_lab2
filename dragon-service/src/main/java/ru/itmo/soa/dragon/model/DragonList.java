package ru.itmo.soa.dragon.model;

import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "dragons")
@XmlAccessorType(XmlAccessType.FIELD)
public class DragonList {
    @XmlElement(name = "dragon")
    public List<Dragon> items = new ArrayList<>();
}
