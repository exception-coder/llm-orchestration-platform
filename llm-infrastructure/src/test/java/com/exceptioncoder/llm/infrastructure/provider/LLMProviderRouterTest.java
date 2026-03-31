package com.exceptioncoder.llm.infrastructure.provider;

import com.exceptioncoder.llm.domain.model.ModelType;
import com.exceptioncoder.llm.domain.service.LLMProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LLMProviderRouterTest {

    private LLMProvider aliProvider;
    private LLMProvider ollamaProvider;
    private LLMProviderRouter router;

    @BeforeEach
    void setUp() {
        aliProvider = mock(LLMProvider.class);
        when(aliProvider.getProviderName()).thenReturn("alibaba");
        when(aliProvider.supports(anyString())).thenAnswer(inv -> {
            String m = inv.getArgument(0);
            return m.contains("qwen") || m.contains("deepseek");
        });

        ollamaProvider = mock(LLMProvider.class);
        when(ollamaProvider.getProviderName()).thenReturn("ollama");
        when(ollamaProvider.supports(anyString())).thenAnswer(inv -> {
            String m = inv.getArgument(0);
            return m.contains("llama");
        });

        router = new LLMProviderRouter(List.of(aliProvider, ollamaProvider));
    }

    @Test
    void routeByModelType_shouldReturnMatchingProvider() {
        LLMProvider result = router.route(ModelType.ALI);
        assertThat(result).isSameAs(aliProvider);
    }

    @Test
    void routeByModelType_ollama_shouldReturnOllamaProvider() {
        LLMProvider result = router.route(ModelType.OLLAMA);
        assertThat(result).isSameAs(ollamaProvider);
    }

    @Test
    void routeByModel_shouldReturnMatchingProvider() {
        LLMProvider result = router.route("qwen-plus");
        assertThat(result).isSameAs(aliProvider);
    }

    @Test
    void routeByModel_noMatch_shouldThrowException() {
        assertThatThrownBy(() -> router.route("unknown-model"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unknown-model");
    }

    @Test
    void routeByModelType_noMatch_shouldThrowException() {
        LLMProviderRouter emptyRouter = new LLMProviderRouter(List.of());
        assertThatThrownBy(() -> emptyRouter.route(ModelType.ALI))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
