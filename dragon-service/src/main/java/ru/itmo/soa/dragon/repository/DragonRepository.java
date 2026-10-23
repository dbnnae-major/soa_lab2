package ru.itmo.soa.dragon.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import ru.itmo.soa.dragon.model.Dragon;
import ru.itmo.soa.dragon.model.DragonInput;
import java.util.List;

@ApplicationScoped
@Transactional
public class DragonRepository {
    @PersistenceContext(unitName = "dragon")
    private EntityManager entityManager;

    public DragonRepository() {
    }

    public DragonRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<Dragon> findAll() {
        return entityManager.createQuery("SELECT d FROM Dragon d ORDER BY d.id", Dragon.class).getResultList();
    }

    public Dragon findById(long id) {
        return entityManager.find(Dragon.class, id);
    }

    public Dragon create(DragonInput input) {
        Dragon dragon = new Dragon();
        copy(input, dragon);
        entityManager.persist(dragon);
        entityManager.flush();
        return dragon;
    }

    public Dragon update(long id, DragonInput input) {
        Dragon dragon = entityManager.find(Dragon.class, id);
        if (dragon == null) return null;
        copy(input, dragon);
        entityManager.flush();
        return dragon;
    }

    public boolean delete(long id) {
        Dragon dragon = entityManager.find(Dragon.class, id);
        if (dragon == null) return false;
        entityManager.remove(dragon);
        entityManager.flush();
        return true;
    }

    private void copy(DragonInput input, Dragon dragon) {
        dragon.name = input.name;
        dragon.coordinates = input.coordinates;
        dragon.age = input.age;
        dragon.wingspan = input.wingspan;
        dragon.speaking = input.speaking;
        dragon.color = input.color;
        dragon.killer = input.killer;
    }
}
