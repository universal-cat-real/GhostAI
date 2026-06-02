package com.ghostcraft.memory;

import com.ghostcraft.core.command.Command;

public class MemoryCommand implements Command {

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

        return "未知参数: " + args + "，用法：/memory status 或 /memory clear";
    }
}
