package com.dailydeal.common.domain.system.repository;

import com.dailydeal.common.domain.system.entity.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServerRepository extends JpaRepository<Server, Long>, ServerRepositoryCustom {
}
