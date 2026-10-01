package com.cocos.challenge.infrastructure.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.lang.NonNull;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcTraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String MDC_TRACE_ID_KEY = "traceId";

    private static final int TRACE_ID_LENGTH = 16;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MdcTraceIdFilter.class);

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, TRACE_ID_LENGTH);

        MDC.put(MDC_TRACE_ID_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        String path = request.getRequestURI();
        boolean isActuator = path != null && path.startsWith("/actuator");
        long startTime = System.currentTimeMillis();

        if (!isActuator) {
            String query = request.getQueryString() != null ? "?" + request.getQueryString() : "";
            log.info("[HTTP] [REQUEST] method={} path={}{}", request.getMethod(), path, query);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            if (!isActuator) {
                long duration = System.currentTimeMillis() - startTime;
                log.info("[HTTP] [RESPONSE] method={} path={} status={} durationMs={}ms",
                        request.getMethod(), path, response.getStatus(), duration);
            }
            MDC.remove(MDC_TRACE_ID_KEY);
        }
    }
}
