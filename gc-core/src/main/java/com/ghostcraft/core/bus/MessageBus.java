package com.ghostcraft.core.bus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 消息总线（发布-订阅模式）
 *
 * 作用：解耦组件之间的通信。
 * 发消息的人不关心谁在监听，监听的人不关心谁发的。
 * 比如在 gc-terminal 里发了一条消息，gc-memory 可以监听并记录 token 用量，
 * gc-hook 可以监听并触发钩子，互相不知道对方存在。
 */
public class MessageBus {

    private final Map<String, List<EventListener>> listeners = new ConcurrentHashMap<>();

    /**
     * 监听器接口：所有监听器实现这个接口
     */
    @FunctionalInterface
    public interface EventListener {
        void onEvent(Event event);
    }

    /**
     * 事件：包含类型和数据
     */
    public static class Event {
        private final String type;
        private final Object data;

        public Event(String type, Object data) {
            this.type = type;
            this.data = data;
        }

        public String type() {
            return type;
        }

        @SuppressWarnings("unchecked")
        public <T> T data() {
            return (T) data;
        }
    }

    /**
     * 订阅某个类型的事件
     *
     * @param type     事件类型，如 "before_chat"、"after_tool_call"
     * @param listener 监听器
     */
    public void subscribe(String type, EventListener listener) {
        listeners.computeIfAbsent(type, k -> new ArrayList<>()).add(listener);
    }

    /**
     * 发布事件：通知所有订阅了这个类型的监听器
     *
     * @param type 事件类型
     * @param data 事件数据
     */
    public void publish(String type, Object data) {
        Event event = new Event(type, data);
        List<EventListener> list = listeners.get(type);
        if (list != null) {
            for (EventListener listener : list) {
                listener.onEvent(event);
            }
        }
    }

    /**
     * 取消订阅
     */
    public void unsubscribe(String type, EventListener listener) {
        List<EventListener> list = listeners.get(type);
        if (list != null) {
            list.remove(listener);
        }
    }
}
