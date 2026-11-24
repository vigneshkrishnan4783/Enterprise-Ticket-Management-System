package com.enterprise.ticket.repository;

import com.enterprise.ticket.constant.RoleType;
import com.enterprise.ticket.model.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

  Optional<Role> findByName(RoleType name);
}
