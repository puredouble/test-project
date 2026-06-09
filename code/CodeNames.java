package com.dailydeal.common.domain.system.enumeration;

import com.dailydeal.common.base.enumeration.EnumModel;

/**
 * 코드명 유형
 */
public enum CodeNames implements EnumModel {

    KCCI_MEMBER("대한 상공회의소 회원");

    private final String value;

    CodeNames(String value) {
        this.value = value;
    }

    @Override
    public String getKey() {
        return name();
    }

    @Override
    public String getValue() {
        return value;
    }

}
