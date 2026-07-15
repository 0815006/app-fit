package com.fit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_favorite_dish")
public class UserFavoriteDish {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 用户ID（user表主键） */
    private String userId;

    /** 收藏的菜品名称 */
    private String dishName;

    /** 收藏时间 */
    private LocalDateTime createTime;
}
