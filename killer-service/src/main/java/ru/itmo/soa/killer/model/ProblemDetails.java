package ru.itmo.soa.killer.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "problem", namespace = "urn:ietf:rfc:7807")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"type", "title", "status", "detail", "instance"})
public class ProblemDetails {
    @XmlElement(namespace = "urn:ietf:rfc:7807")
    public String type = "about:blank";
    @XmlElement(namespace = "urn:ietf:rfc:7807")
    public String title;
    @XmlElement(namespace = "urn:ietf:rfc:7807")
    public int status;
    @XmlElement(namespace = "urn:ietf:rfc:7807")
    public String detail;
    @XmlElement(namespace = "urn:ietf:rfc:7807")
    public String instance;
}
