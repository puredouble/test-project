package com.dailydeal.common.domain.system.dao;

import com.dailydeal.common.base.BaseService;
import com.dailydeal.common.base.exception.type.EntityNotFoundException;
import com.dailydeal.common.domain.system.entity.Code;
import com.dailydeal.common.domain.system.repository.CodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CodeDao extends BaseService {

    private final CodeRepository repository;

    public Long create(Code entity) {
        return repository.save(entity).getCodeId();
    }

    public List<Long> create(List<Code> entities) {
        return repository.saveAll(entities).stream().map(Code::getCodeId).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Code get(Long id) {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException(Code.class.getSimpleName(), id));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public List<Code> getCodeList(String codeName, Long parentId) {
        return repository.getCodeList(codeName, parentId);
    }
}
