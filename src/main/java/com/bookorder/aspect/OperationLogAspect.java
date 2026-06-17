package com.bookorder.aspect;

import com.bookorder.annotation.OpLog;
import com.bookorder.security.SysUserDetails;
import com.bookorder.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class OperationLogAspect {

    @Autowired
    private OperationLogService logService;

    @Around("@annotation(com.bookorder.annotation.OpLog)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        Object result = point.proceed();

        try {
            MethodSignature signature = (MethodSignature) point.getSignature();
            OpLog opLog = signature.getMethod().getAnnotation(OpLog.class);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Long userId = null;
            String username = "anonymous";

            if (auth != null && auth.getPrincipal() instanceof SysUserDetails userDetails) {
                userId = userDetails.getId();
                username = userDetails.getUsername();
            }

            // 获取 IP
            String ip = "";
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                ip = request.getRemoteAddr();
                if (request.getHeader("X-Forwarded-For") != null) {
                    ip = request.getHeader("X-Forwarded-For").split(",")[0].trim();
                }
            }

            // 构建 target 和 detail
            Object[] args = point.getArgs();
            String target = buildTarget(signature, args);
            String detail = opLog.operation() + " " + opLog.module() + " " + target;

            logService.log(userId, username, opLog.module(), opLog.operation(), target, detail, ip);
        } catch (Exception e) {
            // 日志记录失败不影响主业务
        }

        return result;
    }

    private String buildTarget(MethodSignature signature, Object[] args) {
        StringBuilder sb = new StringBuilder();
        String[] paramNames = signature.getParameterNames();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                if (!paramNames[i].equals("userDetails") && args[i] != null) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(paramNames[i]).append("=").append(args[i]);
                }
            }
        }
        return sb.toString();
    }
}
