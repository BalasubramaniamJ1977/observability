package com.payments.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.io.IOException;

/**
 * Plug-and-play payment traceability filter.
 *
 * On every inbound POST with a JSON body the filter:
 *   1. Buffers the request body so it can be read again by the handler.
 *   2. Extracts the "uetr" field.
 *   3. Sets MDC "payment.uetr"  → trace_id appears alongside uetr in every log line.
 *   4. Sets span attribute "payment.uetr" → TraceQL  { span.payment.uetr =~ "^…$" }
 *      works in Tempo so the Grafana UETR search panel returns results.
 *
 * Registered as an outermost filter via CommonAutoConfiguration.
 * No code changes needed in any service.
 */
@Slf4j
public class UETRTraceFilter implements Filter {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String UETR_FIELD  = "uetr";
    private static final String MDC_KEY     = "payment.uetr";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest http) || !isJsonPost(http)) {
            chain.doFilter(request, response);
            return;
        }

        CachedBodyHttpServletRequest cached = new CachedBodyHttpServletRequest(http);
        String uetr = extractUetr(cached.getCachedBody());

        if (uetr != null) {
            MDC.put(MDC_KEY, uetr);
            Span span = Span.current();
            if (span.isRecording()) {
                span.setAttribute(AttributeKey.stringKey("payment.uetr"), uetr);
            }
        }

        try {
            chain.doFilter(cached, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private boolean isJsonPost(HttpServletRequest r) {
        return "POST".equalsIgnoreCase(r.getMethod())
            && r.getContentType() != null
            && r.getContentType().contains("application/json");
    }

    private String extractUetr(byte[] body) {
        if (body == null || body.length == 0) return null;
        try {
            JsonNode node = MAPPER.readTree(body).get(UETR_FIELD);
            return (node != null && node.isTextual()) ? node.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
