package com.p2pledger.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePin {
    String message() default "Invalid transaction PIN";
    int userIdIndex() default 0;
    int pinIndex() default 1;
}
