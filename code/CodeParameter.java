package com.dailydeal.common.domain.system.dto.parameter;

import jakarta.validation.constraints.NotBlank;
import com.dailydeal.common.base.annotation.FieldComment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CodeParameter {

    @NotBlank
    @FieldComment(code = "Code.codeName")
    private String codeName;

    @FieldComment("부모 코드 일련번호")
    private Long parentId;

}
