package com.ghostcraft.core.hook;

/**
 * 生命周期钩子接口
 *
 * 作用：在 Agent 处理消息的不同阶段插入自定义逻辑。
 * 每个阶段对应一个方法，默认什么都不做。
 */
public interface Hook {

    /** 用户发送消息后、交给 LLM 之前触发 */
    default void beforeChat(String sessionId, String userMessage) {}

    /** LLM 返回回答后、返回给用户之前触发 */
    default void afterChat(String sessionId, String userMessage, String aiMessage) {}

    /** LLM 请求调用工具时触发，可以返回 false 阻止工具执行 */
    default boolean onToolCall(String sessionId, String toolName, String arguments) {
        return true;
    }

    /** 出错时触发 */
    default void onError(String sessionId, String userMessage, Throwable error) {}

    /** 钩子名称 */
    default String name() {
        return getClass().getSimpleName();
    }
}
