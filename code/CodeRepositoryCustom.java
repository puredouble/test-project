package com.dailydeal.common.domain.system.repository;

import com.dailydeal.common.domain.system.entity.Code;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodeRepositoryCustom {

    List<Code> getCodeList(String codeName, Long parentId);

}
