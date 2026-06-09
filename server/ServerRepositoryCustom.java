package com.dailydeal.common.domain.system.repository;

import com.dailydeal.common.domain.system.entity.Server;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServerRepositoryCustom {

    Server getServer(String uid);

    Server getMasterServer();

    List<Server> getServerList();

}
