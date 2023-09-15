package com.accesscontrol.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

public class CommonException extends RuntimeException {

    @Getter
    private HttpStatus status;
}
