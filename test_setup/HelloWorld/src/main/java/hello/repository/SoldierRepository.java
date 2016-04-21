package hello.repository;

import hello.model.Soldier;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by bjagwani on 4/17/16.
 */

@Repository
public interface SoldierRepository extends CrudRepository<Soldier, Long>{
}
