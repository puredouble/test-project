package com.dailydeal.common.domain.system.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.dailydeal.common.domain.system.entity.Code;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.dailydeal.common.domain.system.entity.QCode.code;

@RequiredArgsConstructor
public class CodeRepositoryImpl implements CodeRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Code> getCodeList(String codeName, Long parentId) {
        return queryFactory
                .selectFrom(code)
                .where(
                        code.codeName.eq(codeName),
                        parentId != null ? code.parent.codeId.eq(parentId) : code.parent.isNull()
                )
                .orderBy(code.codeName.asc())
                .fetch();
    }

}
