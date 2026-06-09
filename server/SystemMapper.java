package com.dailydeal.common.domain.system.dto.mapper;

import com.dailydeal.common.domain.system.dto.response.AttachResponse;
import com.dailydeal.common.domain.system.dto.response.CodeListResponse;
import com.dailydeal.common.domain.system.dto.response.CodeResponse;
import com.dailydeal.common.domain.system.entity.Attach;
import com.dailydeal.common.domain.system.entity.Code;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SystemMapper {

    SystemMapper SYSTEM_MAPPER = Mappers.getMapper(SystemMapper.class);

    AttachResponse toAttachResponse(Attach attach);

    CodeListResponse toCodeListResponse(Code code);

    CodeResponse toCodeResponse(Code code);

}
