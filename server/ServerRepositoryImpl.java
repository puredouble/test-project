package com.dailydeal.common.domain.system.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.dailydeal.common.domain.system.entity.Server;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.dailydeal.common.domain.system.entity.QServer.server;

@RequiredArgsConstructor
public class ServerRepositoryImpl implements ServerRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Server getServer(String uid) {
        return queryFactory
                .selectFrom(server)
                .where(
                        server.isDeleted.isFalse(),
                        server.uid.eq(uid)
                ).fetchFirst();
    }

    @Override
    public Server getMasterServer() {
        return queryFactory
                .selectFrom(server)
                .where(
                        server.isDeleted.isFalse(),
                        server.isMaster.isTrue()
                )
                .orderBy(server.serverId.desc())
                .fetchFirst();
    }

    @Override
    public List<Server> getServerList() {
        return queryFactory
                .selectFrom(server)
                .where(
                        server.isDeleted.isFalse()
                )
                .orderBy(server.priority.desc(), server.updateDt.asc())
                .fetch();
    }

}
