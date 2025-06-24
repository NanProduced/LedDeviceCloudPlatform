package org.nan.cloud.common.web;

import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class TraceHandler {

    private final Tracer tracer;

    public String getTraceId() {
        return tracer.currentSpan() != null ? Objects.requireNonNull(tracer.currentSpan()).context().traceId() : null;
    }
}
