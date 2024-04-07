package com.rainsun.yuqing.model.domain.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserLoginRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = -4952406713693029852L;

    private String userAccount;
    private String userPassword;
}
