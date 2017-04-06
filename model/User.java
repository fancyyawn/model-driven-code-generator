/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package io.terminus.parana.user.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;


@EqualsAndHashCode(of={"name","email","mobile"})
@ToString(of={"id","name","email","mobile","type","status","roles"})
public class User implements Serializable {

    private static final long serialVersionUID = -2961193418926377287L;

    /**
     * 用户id
     */
    @Getter
    @Setter
    private Long id;

    /**
     * 用户名
     */
    @Getter
    @Setter
    private String name;

    /**
     * 邮件
     */
    @Getter
    @Setter
    private String email;

    /**
     * 手机号码
     */
    @Getter
    @Setter
    private String mobile;


    /**
     * 加盐加密的密码
     */
    @Getter
    @Setter
    private String password;

    /**
     * 用户类型 1:超级管理员, 2:普通用户, 3:后台运营, 4:站点拥有者
     */
    @Getter
    @Setter
    private Integer type;

    /**
     * 用户状态 0:未激活, 1:正常, -1:锁定, -2:冻结, -3: 删除
     */
    @Getter
    @Setter
    private Integer status;

    /**
     * 用户所有的角色列表, 不存数据库
     */
    @Getter
    private List<String> roles;

    /**
     * 用户所有的角色列表, json存储, 存数据库
     */
    @Getter
    @JsonIgnore
    private String rolesJson;

    /**
     * 放店铺扩展信息,不存数据库
     */
    @Getter
    private Map<String, String> extra;

    /**
     * 放用户扩展信息, json存储, 存数据库
     */
    @Getter
    @JsonIgnore
    private String extraJson;

    /**
     * 用户本身的tag信息, 由运营操作, 对用户不可见, 不存数据库
     */
    @Getter
    private Map<String, String> tags;

    /**
     * 用户本身的tag信息, 由运营操作, 对用户不可见,存数据库
     */
    @Getter
    @JsonIgnore
    private String tagsJson;

    /**
     * 创建时间
     */
    @Getter
    @Setter
//    @JsonIgnore
    private Date createdAt;

    /**
     * 更新时间
     */
    @Getter
    @Setter
//    @JsonIgnore
    private Date updatedAt;
}
