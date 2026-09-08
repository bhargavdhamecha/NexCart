package com.nexcart.backend.common.logging;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Logs entry and exit for every method on every *ServiceImpl class — broad, automatic, no
 * per-method annotation needed. Two things are deliberately kept out of the log line rather
 * than printed in full:
 *  - Any parameter whose value would be long (see MAX_VALUE_LENGTH) — replaced with a short
 *    placeholder instead of dumping a large object/string into the log.
 *  - Any parameter marked @LogMask (e.g. a password, a refresh token) — replaced with "***"
 *    regardless of length, since the length rule alone wouldn't catch something short.
 *
 * Logged at DEBUG, not INFO — with every service method wrapped, INFO would double the log
 * volume of every request by default. Enable via logging.level.com.nexcart.backend=DEBUG (or
 * narrower, logging.level.com.nexcart.backend.common.logging=DEBUG for just this aspect) when
 * actually tracing something, rather than this being on unconditionally.
 */
@Aspect
@Component
public class LoggingAspect {

	private static final int MAX_VALUE_LENGTH = 200;

	@Around("execution(* com.nexcart.backend..service..*ServiceImpl.*(..))")
	public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {
		Logger log = LoggerFactory.getLogger(joinPoint.getTarget().getClass());
		if (!log.isDebugEnabled()) {
			return joinPoint.proceed();
		}

		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		String methodName = method.getName();

		log.debug("Entering {}({})", methodName, formatArgs(method, joinPoint.getArgs()));

		long startedAt = System.currentTimeMillis();
		try {
			Object result = joinPoint.proceed();
			long elapsedMs = System.currentTimeMillis() - startedAt;
			log.debug("Exiting {}() -> {} ({}ms)", methodName, formatValue(result), elapsedMs);
			return result;
		}
		catch (Throwable exception) {
			long elapsedMs = System.currentTimeMillis() - startedAt;
			log.debug("Exiting {}() threw {}: {} ({}ms)", methodName,
				exception.getClass().getSimpleName(), exception.getMessage(), elapsedMs);
			throw exception;
		}
	}

	private String formatArgs(Method method, Object[] args) {
		Annotation[][] parameterAnnotations = method.getParameterAnnotations();
		return IntStream.range(0, args.length)
			.mapToObj(i -> isMasked(parameterAnnotations[i]) ? "***" : formatValue(args[i]))
			.collect(Collectors.joining(", "));
	}

	private boolean isMasked(Annotation[] parameterAnnotations) {
		for (Annotation annotation : parameterAnnotations) {
			if (annotation.annotationType() == LogMask.class) {
				return true;
			}
		}
		return false;
	}

	private String formatValue(Object value) {
		if (value == null) {
			return "null";
		}
		String stringValue = String.valueOf(value);
		if (stringValue.length() <= MAX_VALUE_LENGTH) {
			return stringValue;
		}
		return "<" + value.getClass().getSimpleName() + ", length=" + stringValue.length() + ", skipped>";
	}
}
