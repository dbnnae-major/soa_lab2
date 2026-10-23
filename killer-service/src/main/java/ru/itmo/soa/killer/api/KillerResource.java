package ru.itmo.soa.killer.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ru.itmo.soa.killer.client.DragonClient;
import ru.itmo.soa.killer.repository.KillerTeamRepository;

@Path("/killer")
public class KillerResource {
    @Inject
    DragonClient dragonClient;

    @Inject
    KillerTeamRepository repository;

    @POST
    @Path("/dragon/{dragon-id}/kill")
    public Response killDragon(@PathParam("dragon-id") String value) {
        long id = readLong("dragon-id", value);
        if (id <= 0) throw new ApiException(422, "Параметр dragon-id должен быть больше 0.");
        dragonClient.kill(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/teams/create/{team-id}/{team-name}/{team-size}/{start-cave-id}")
    @Produces(MediaType.APPLICATION_XML)
    public Response createTeam(@PathParam("team-id") String id,
                               @PathParam("team-name") String name,
                               @PathParam("team-size") String size,
                               @PathParam("start-cave-id") String caveId) {
        return Response.status(201).entity(repository.create(
                readLong("team-id", id), name,
                readInt("team-size", size),
                readLong("start-cave-id", caveId))).build();
    }

    private long readLong(String name, String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new ApiException(400, "Параметр " + name + " должен иметь целочисленный формат.");
        }
    }

    private int readInt(String name, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ApiException(400, "Параметр " + name + " должен иметь целочисленный формат.");
        }
    }
}
