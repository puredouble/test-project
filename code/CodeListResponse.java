package com.dailydeal.common.domain.system.dto.response;

import com.dailydeal.common.base.annotation.FieldComment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CodeListResponse {

    @FieldComment(code = "Code.codeId")
    private Long codeId;

    @FieldComment(code = "Code.codeName")
    private String codeName;

    @FieldComment(code = "Code.codeValue")
    private String codeValue;

    @FieldComment(code = "Code.description")
    private String description;

    @FieldComment(code = "Code.parentCodeName")
    private String parentCodeName;

}
