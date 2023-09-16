package com.accesscontrol.error;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalErrorHandler {
    @ExceptionHandler(CommonException.class)
    public ResponseEntity<CommonExceptionResponse> commonExceptionHandler(CommonException e) {
        CommonExceptionResponse error = new CommonExceptionResponse(e.getStatus().value(), e.getMessage() != null ? e.getMessage() : "나도 몰라요");
        return ResponseEntity.status(e.getStatus()).body(error);
    }
}