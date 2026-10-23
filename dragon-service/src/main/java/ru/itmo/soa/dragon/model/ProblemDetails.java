package ru.itmo.soa.dragon.model;

import jakarta.xml.bind.annotation.*;

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
