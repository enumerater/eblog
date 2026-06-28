package com.enumerate.gateway.handler;

import com.enumerate.common.core.result.Result;
import com.enumerate.common.core.result.ResultCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Gateway 全局异常/降级处理器
 *
 * 当后端服务熔断/不可用时, 返回统一的降级 JSON, 而不是默认的错误页面
 *
 * 覆盖场景:
 *  - 服务熔断 (Sentinel BlockException)
 *  - 路由超时 (TimeoutException)
 *  - 服务不可用 (ConnectException)
 *  - 404 路由不存在
 */
@Slf4j
@Order(-1)
@Configuration
@RequiredArgsConstructor
public class FallbackHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        HttpStatus status;
        ResultCode resultCode;

        // ── 按异常类型分类 ──
        if (ex instanceof NotFoundException) {
            status = HttpStatus.NOT_FOUND;
            resultCode = ResultCode.NOT_FOUND;
        } else if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            resultCode = ResultCode.SERVICE_UNAVAILABLE;
        } else if (ex instanceof com.alibaba.csp.sentinel.slots.block.BlockException) {
            // Sentinel 限流熔断
            status = HttpStatus.TOO_MANY_REQUESTS;
            resultCode = ResultCode.RATE_LIMITED;
        } else if (ex instanceof java.util.concurrent.TimeoutException) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            resultCode = ResultCode.GATEWAY_TIMEOUT;
        } else {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            resultCode = ResultCode.SERVICE_UNAVAILABLE;
        }

        String path = exchange.getRequest().getURI().getPath();
        log.warn("网关降级: status={}, path={}, error={}",
                status, path, ex.getClass().getSimpleName());

        response.setStatusCode(status);
        return writeJson(response, Result.fail(resultCode.getCode(), resultCode.getMessage()));
    }

    private Mono<Void> writeJson(ServerHttpResponse response, Result<?> result) {
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(result);
        } catch (JsonProcessingException e) {
            bytes = "{\"code\":503,\"message\":\"服务暂不可用\"}".getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
