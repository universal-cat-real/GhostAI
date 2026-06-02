package com.ghostcraft.memory;

import com.ghostcraft.core.command.Command;
import com.ghostcraft.core.conversation.ConversationManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MemoryCommand implements Command {

    @Autowired
    private ConversationManager cm;

    @Override
    public String name() {
        return "memory";
    }

    @Override
    public String description() {
        return "记忆管理：/memory status 查看状态, /memory clear 清除记忆";
    }

    @Override
    public String execute(String args) {
        if (args == null || args.isEmpty()) {
            return "用法：/memory status 或 /memory clear";
        }

        if (args.equals("status")) {
            return cm.getMemoryStats();
        }

        if (args.equals("clear")) {
            // 由于不清楚当前 sessionId，这里由终端持有并传参
            // 实际通过终端命令转发
            return "请在终端用 /clear 清除当前会话记忆";
        }
        if (args != null && args.startsWith("search ")) {
            return cm.crossSessionQuery(args.substring(7));
        }
        if ("sessions".equals(args)) {
            return cm.listStoredSessions();
        }
        return "用法：/memory status | /memory clear | /memory search <关键词> | /memory sessions";
    }
}
