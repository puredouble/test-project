package com.dailydeal.common.domain.system.dao;

import com.dailydeal.common.base.BaseService;
import com.dailydeal.common.base.exception.type.EntityNotFoundException;
import com.dailydeal.common.domain.system.entity.Server;
import com.dailydeal.common.domain.system.repository.ServerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ServerDao extends BaseService {

    private final ServerRepository repository;

    public Long create(Server entity) {
        return repository.save(entity).getServerId();
    }

    @Transactional(readOnly = true)
    public Boolean exists(Long id) {
        return repository.existsById(id);
    }

    @Transactional(readOnly = true)
    public Server get(Long id) {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException(Server.class.getSimpleName(), id));
    }

    public Server getServer(String uid) {
        return repository.getServer(uid);
    }

    public Server getMasterServer() {
        return repository.getMasterServer();
    }

    public List<Server> getServerList() {
        return repository.getServerList();
    }
}
