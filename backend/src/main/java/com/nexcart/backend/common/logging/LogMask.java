package com.nexcart.backend.common.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service-method parameter as never safe to log — LoggingAspect prints "***" for it
 * regardless of length, instead of its actual value. Needed because the length-based skip rule
 * (see LoggingAspect) doesn't help for something like a password: it's short, so it wouldn't be
 * skipped on size alone, it would just get logged in plaintext. Applied to whichever parameter
 * position genuinely carries a secret — e.g. AuthServiceImpl.login's LoginRequest (carries a
 * password), or refresh()'s raw refreshToken string — not blanket-applied to every parameter.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogMask {
}
