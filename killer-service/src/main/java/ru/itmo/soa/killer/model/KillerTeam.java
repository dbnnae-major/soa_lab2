package ru.itmo.soa.killer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@Entity
@Table(name = "soa_lab2_killer_team")
@XmlRootElement(name = "team")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"id", "name", "size", "startCaveId"})
public class KillerTeam {
    @Id
    public Long id;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public Integer size;

    @Column(name = "start_cave_id", nullable = false)
    public Long startCaveId;
}
