package com.exceptioncoder.llm.domain.devplan.exception;

/**
 * 所有画像生成器均失败时抛出。
 *
 * @author zhangkai
 * @since 2026-04-11
 */
public class ProfileGenerationException extends RuntimeException {

    public ProfileGenerationException(String message) {
        super(message);
    }

    public ProfileGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
