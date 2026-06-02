package com.ghostcraft.terminal;

import com.ghostcraft.core.command.Command;
import com.ghostcraft.core.conversation.ConversationManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MemoryCommand implements Command {

    @Autowired
    private ConversationManager cm;

    @Override
    public String name() { return "memory"; }

    @Override
    public String description() { return "记忆管理：/memory status 查看状态, /memory clear 清除记忆"; }

    @Override
    public String execute(String args) {
        if ("status".equals(args)) {
            return cm.getMemoryStats();
        }
        if ("clear".equals(args)) {
            return "请在终端用 /clear 清除当前会话记忆";
        }
        return "用法：/memory status 或 /memory clear";
    }
}
