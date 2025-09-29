package com.slam.concertreservation.infrastructure.logging;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;
import org.springframework.web.servlet.HandlerMapping;

@Slf4j
@Component
@Order(1) // 요청이 서비스로 진입하여 처리 되기 전에 가장 먼저 실행되도록 우선순위 지정
public class RequestTraceFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 1. Trace ID 생성 (요청당 고유 식별자)
        String traceId = UUID.randomUUID().toString().substring(0, 8);

        // 2. 요청 시작 시간 (응답시간 측정용)
        long startTime = System.currentTimeMillis();

        // 3. MDC에 컨텍스트 정보 추가 (모든 로그에 자동 포함됨)
        MDC.put("traceId", traceId);
        MDC.put("method", httpRequest.getMethod());
        MDC.put("uri", httpRequest.getRequestURI());
        MDC.put("clientIp", getClientIp(httpRequest));

        // 4. 헤더에서 userId 추출 (있는 경우만)
        String userId = httpRequest.getHeader("X-User-Id");
        if (userId != null) {MDC.put("userId", userId);}

        // 5. 응답 헤더에 traceId 추가 (클라이언트 디버깅용)
        httpResponse.setHeader("X-Trace-Id", traceId);

        try {
            // 6. 요청 시작 로그
            log.info("[{}] REQ START", traceId);

            // 7. 실제 요청 처리
            chain.doFilter(request, response);

        } finally {
            // 8. 처리 시간 계산
            long duration = System.currentTimeMillis() - startTime;
            MDC.put("duration", String.valueOf(duration));
            MDC.put("status", String.valueOf(httpResponse.getStatus()));

            // 요청 완료 로그
            log.info("[{}] REQ END & RES RETURNED : {}, Duration: {} ms",
                    MDC.get("traceId"),
                    httpResponse.getStatus(),
                    duration);

            // 9. MDC 정리 (ThreadLocal 내부에 적재된 내용을 정리하여 Memory Leak 방지)
            MDC.clear();
        }
    }

    /**
     * 실제 클라이언트 IP 추출 (프록시 고려)
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
