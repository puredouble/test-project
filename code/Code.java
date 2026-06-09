package com.dailydeal.common.domain.system.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "SYSTEM_CODE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Code {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codeId;

    @Column(nullable = false)
    private String codeName;

    @Column(nullable = false)
    private String codeValue;

    @Column(length = 1000)
    private String description;

    private String parentCodeName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Code parent;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createDt;

    @UpdateTimestamp
    @Column(insertable = false)
    private LocalDateTime updateDt;

    /**
     * 생성자
     */
    private Code(
            String codeName,
            String codeValue,
            String description,
            Code parent
    ) {
        this.codeName = codeName;
        this.codeValue = codeValue;
        this.description = description;
        this.parent = parent;

        if (parent != null) {
            this.parentCodeName = parentCodeName;
        }
    }

    /**
     * 생성
     */
    public static Code createEntity(
            String codeName,
            String codeValue,
            String description,
            Code parent
    ) {
        return new Code(
                codeName,
                codeValue,
                description,
                parent
        );
    }

}
