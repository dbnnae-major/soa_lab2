package ru.itmo.soa.killer.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import ru.itmo.soa.killer.api.ApiException;
import ru.itmo.soa.killer.model.KillerTeam;

@ApplicationScoped
@Transactional
public class KillerTeamRepository {
    @PersistenceContext(unitName = "killer")
    private EntityManager entityManager;

    public KillerTeam create(long id, String name, int size, long startCaveId) {
        if (entityManager.find(KillerTeam.class, id) != null) {
            throw new ApiException(409, "Команда с указанным team-id уже существует.");
        }

        KillerTeam team = new KillerTeam();
        team.id = id;
        team.name = name;
        team.size = size;
        team.startCaveId = startCaveId;
        entityManager.persist(team);
        entityManager.flush();
        return team;
    }
}
