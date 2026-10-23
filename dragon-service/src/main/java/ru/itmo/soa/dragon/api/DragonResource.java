package ru.itmo.soa.dragon.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.JAXBElement;
import ru.itmo.soa.dragon.model.Dragon;
import ru.itmo.soa.dragon.model.DragonInput;
import ru.itmo.soa.dragon.model.DragonList;
import ru.itmo.soa.dragon.query.DragonQuery;
import ru.itmo.soa.dragon.repository.DragonRepository;
import ru.itmo.soa.dragon.xml.DragonXmlReader;

import javax.xml.namespace.QName;

@Path("/dragons")
@Produces(MediaType.APPLICATION_XML)
public class DragonResource {
    @Inject
    DragonRepository repository;

    @Inject
    DragonXmlReader xmlReader;

    @Context
    UriInfo uri;

    @GET
    public Response list() {
        DragonQuery query = new DragonQuery(uri.getQueryParameters());
        DragonList result = new DragonList();
        result.items = query.apply(repository.findAll());
        return Response.ok(result).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    public Response create(String xml) {
        DragonInput input = xmlReader.read(xml);
        return Response.status(201).entity(repository.create(input)).build();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") String value) {
        Dragon dragon = repository.findById(readId(value));
        if (dragon == null) throw new ApiException(404, "Запрошенный API-ресурс не существует.");
        return Response.ok(dragon).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_XML)
    public Response update(@PathParam("id") String value, String xml) {
        long id = readId(value);
        DragonInput input = xmlReader.read(xml);
        Dragon dragon = repository.update(id, input);
        if (dragon == null) throw new ApiException(404, "Запрошенный API-ресурс не существует.");
        return Response.ok(dragon).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String value) {
        if (!repository.delete(readId(value))) {
            throw new ApiException(404, "Запрошенный API-ресурс не существует.");
        }
        return Response.noContent().build();
    }

    @GET
    @Path("/name/starts-with")
    public Response byNamePrefix(@QueryParam("prefix") String prefix) {
        requiredText("prefix", prefix);
        DragonList result = new DragonList();
        result.items = repository.findAll().stream()
                .filter(dragon -> dragon.name.startsWith(prefix))
                .toList();
        return Response.ok(result).build();
    }

    @GET
    @Path("/killer/less-than/count")
    public Response countByKillerLessThan(@QueryParam("killerName") String killerName) {
        requiredText("killerName", killerName);
        long count = repository.findAll().stream()
                .filter(dragon -> dragon.killer != null && dragon.killer.name.compareTo(killerName) < 0)
                .count();
        return Response.ok(new JAXBElement<>(new QName("count"), Long.class, count)).build();
    }

    @GET
    @Path("/killer/greater-than")
    public Response byKillerGreaterThan(@QueryParam("killerName") String killerName) {
        requiredText("killerName", killerName);
        DragonList result = new DragonList();
        result.items = repository.findAll().stream()
                .filter(dragon -> dragon.killer != null && dragon.killer.name.compareTo(killerName) > 0)
                .toList();
        return Response.ok(result).build();
    }

    private void requiredText(String name, String value) {
        if (value == null) throw new ApiException(400, "Обязательный query-параметр " + name + " не передан.");
        if (value.isEmpty()) throw new ApiException(422, "Параметр " + name + " не может быть пустой строкой.");
    }

    private long readId(String value) {
        long id;
        try {
            id = Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new ApiException(400, "Параметр id должен быть целым числом.");
        }
        if (id <= 0) throw new ApiException(422, "Параметр id должен быть больше 0.");
        return id;
    }
}
