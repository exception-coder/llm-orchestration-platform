#!/usr/bin/env node

/**
 * Claude Code SDK Bridge — Java ProcessBuilder 与 Claude Code SDK 之间的桥接层。
 *
 * 用法：node claude-code-bridge.mjs '<json-config>'
 *
 * 输入（JSON）：
 *   { "prompt": "...", "cwd": "/path/to/project", "skillName": "generate-project-profile" }
 *
 * 输出（stdout，逐行 JSON）：
 *   {"type": "event", "event": "..."}     — Claude Code 事件流
 *   {"type": "result", "success": true}    — 最终结果
 *   {"type": "error", "message": "..."}    — 错误信息
 *
 * @author zhangkai
 * @since 2026-04-12
 */

import { claude } from '@anthropic-ai/claude-code';

function writeLine(obj) {
    process.stdout.write(JSON.stringify(obj) + '\n');
}

async function main() {
    const raw = process.argv[2];
    if (!raw) {
        writeLine({ type: 'error', message: 'Missing JSON config argument' });
        process.exit(1);
    }

    let config;
    try {
        config = JSON.parse(raw);
    } catch (e) {
        writeLine({ type: 'error', message: `Invalid JSON: ${e.message}` });
        process.exit(1);
    }

    const { prompt, cwd, skillName } = config;
    const finalPrompt = skillName
        ? `请执行 ${skillName} skill，分析项目并生成画像文档。项目路径：${cwd}`
        : prompt;

    try {
        const messages = await claude({
            prompt: finalPrompt,
            options: {
                cwd: cwd,
                dangerouslySkipPermissions: true,
                maxTurns: 50,
            },
            onEvent: (event) => {
                // 将关键事件流式输出给 Java 侧消费
                if (event.type === 'assistant' && event.message) {
                    const content = event.message.content;
                    if (Array.isArray(content)) {
                        for (const block of content) {
                            if (block.type === 'text' && block.text) {
                                writeLine({ type: 'progress', text: block.text });
                            } else if (block.type === 'tool_use') {
                                writeLine({
                                    type: 'tool_use',
                                    tool: block.name,
                                    input: typeof block.input === 'string'
                                        ? block.input.substring(0, 200)
                                        : JSON.stringify(block.input).substring(0, 200),
                                });
                            }
                        }
                    }
                } else if (event.type === 'result') {
                    writeLine({
                        type: 'result',
                        success: !event.isError,
                        costUsd: event.costUsd,
                        durationMs: event.durationMs,
                        numTurns: event.numTurns,
                    });
                }
            },
        });

        // 输出最终 assistant 文本（--print 等效内容）
        const finalText = messages
            .filter(m => m.role === 'assistant')
            .flatMap(m => m.content)
            .filter(b => b.type === 'text')
            .map(b => b.text)
            .join('\n');

        if (finalText) {
            writeLine({ type: 'final_output', text: finalText });
        }

        process.exit(0);
    } catch (err) {
        writeLine({ type: 'error', message: err.message || String(err) });
        process.exit(1);
    }
}

main();
