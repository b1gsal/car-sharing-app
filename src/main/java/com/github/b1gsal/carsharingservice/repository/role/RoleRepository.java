package com.github.b1gsal.carsharingservice.repository.role;

import com.github.b1gsal.carsharingservice.model.Role;
import com.github.b1gsal.carsharingservice.model.RoleName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
