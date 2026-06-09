package com.dailydeal.common.domain.system.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 서버
 */
@Entity
@Table(name = "SYSTEM_SERVER")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Server {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long serverId;

    @Column(nullable = false)
    private String uid;

    @Column(nullable = false)
    private Boolean isMaster;

    @Column(nullable = false)
    private Integer priority;

    @Column(nullable = false)
    private String ipAddress;

    @Column(nullable = false)
    private Boolean isDeleted;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createDt;

    private LocalDateTime updateDt;

    private Server(
            String uid,
            Boolean isMaster,
            Integer priority,
            String ipAddress
    ) {
        this.uid = uid;
        this.isMaster = isMaster;
        this.priority = priority;
        this.ipAddress = ipAddress;

        this.isDeleted = false;
    }

    public static Server createEntity(
            String uid,
            Boolean isMaster,
            Integer priority,
            String ipAddress
    ) {
        return new Server(
                uid,
                isMaster,
                priority,
                ipAddress
        );
    }

    /**
     * 서버 삭제
     */
    public void delete() {
        this.isDeleted = true;
        this.isMaster = false;
    }

    /**
     * 우선순위 점수 업데이트
     */
    public void updatePriorityScore(int score) {
        this.priority = score;
    }

    /**
     * 마스터 서버 지정
     */
    public void setMaster() {
        this.isMaster = true;
    }

    /**
     * 서브 서버 지정
     */
    public void setSub() {
        this.isMaster = false;
    }

    /**
     * 서버 live 확인 시간 업데이트
     */
    public void setUpdateDt() {
        this.updateDt = LocalDateTime.now();
    }

}
