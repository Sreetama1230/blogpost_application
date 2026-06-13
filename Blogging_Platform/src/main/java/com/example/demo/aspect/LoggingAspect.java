package com.example.demo.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

	private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

	@Around("execution(* com.example.demo.service.*.*(..))")
	public Object logAround(ProceedingJoinPoint joinpoint) throws Throwable {

		long start = System.currentTimeMillis();
		try {
			Object result = joinpoint.proceed();

			long timeTake = System.currentTimeMillis() - start;

			log.info("{} executed in {} ms", joinpoint.getSignature().toShortString(), timeTake);

			return result;
		} catch (Throwable e) {
			long timeTake = System.currentTimeMillis() - start;
			log.error("{} failed after {} ms", joinpoint.getSignature().toShortString(), timeTake, e);
			throw e;
		}

	}
}
