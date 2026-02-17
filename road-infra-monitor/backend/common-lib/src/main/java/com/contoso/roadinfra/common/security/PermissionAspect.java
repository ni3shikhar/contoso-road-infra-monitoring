package com.contoso.roadinfra.common.security;

import com.contoso.roadinfra.common.constants.Permission;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Aspect that processes @RequiresPermission annotations to enforce
 * permission-based access control.
 */
@Aspect
@Component
public class PermissionAspect {

    @Around("@annotation(com.contoso.roadinfra.common.security.RequiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresPermission annotation = method.getAnnotation(RequiresPermission.class);

        if (annotation == null) {
            return joinPoint.proceed();
        }

        Permission[] requiredPermissions = annotation.value();
        boolean requireAll = annotation.requireAll();
        String message = annotation.message();

        boolean hasAccess;
        if (requireAll) {
            hasAccess = SecurityUtils.hasAllPermissions(requiredPermissions);
        } else {
            hasAccess = SecurityUtils.hasAnyPermission(requiredPermissions);
        }

        if (!hasAccess) {
            throw new AccessDeniedException(message);
        }

        return joinPoint.proceed();
    }
}
