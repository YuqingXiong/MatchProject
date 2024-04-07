package com.rainsun.yuqing.model.domain.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserRegisterRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 7894804720314029964L;
    private String userAccount;
    private String userPassword;
    private String checkPassword;
    private String planetCode;
}
