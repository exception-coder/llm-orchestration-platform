package com.exceptioncoder.llm.infrastructure.prompt;

import com.exceptioncoder.llm.domain.service.PromptTemplate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 数据库 Prompt 模板实现
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabasePromptTemplate implements PromptTemplate {
    
    private String templateName;
    private String templateContent;
    private String category;
    private String description;
    private String variableExamples;
    
    @Override
    public String render(Map<String, Object> variables) {
        String result = templateContent;
        
        // 替换变量占位符 {variableName}
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }
        
        return result;
    }
    
    @Override
    public String getTemplateName() {
        return templateName;
    }
}

