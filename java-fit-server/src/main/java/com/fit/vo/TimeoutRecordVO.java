package com.fit.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TimeoutRecordVO {

    /** 超时记录ID */
    private String recordId;

    /** 动作名称 */
    private String actionName;

    /** 训练开始时间 */
    private LocalDateTime startTime;

    /** 人性化时间标签，如"昨天 18:30" */
    private String startTimeLabel;
}
