package com.ghostcraft.core.command;

import org.springframework.stereotype.Component;
import java.util.*;

/**
 * 命令注册器
 *
 * 作用：统一管理所有命令，支持别名。
 */
@Component
public class CommandRegistry {

    private final Map<String, Command> commands = new LinkedHashMap<>();

    /**
     * 注册一个命令（含别名）
     */
    public void register(Command command) {
        commands.put(command.name(), command);
        for (String alias : command.aliases()) {
            commands.put(alias, command);
        }
    }

    /**
     * 执行命令
     */
    public String execute(String input) {
        if (input == null || !input.startsWith("/")) {
            return "不是命令格式，命令以 / 开头";
        }

        int firstSpace = input.indexOf(' ');
        String name;
        String args;
        if (firstSpace == -1) {
            name = input.substring(1);
            args = "";
        } else {
            name = input.substring(1, firstSpace);
            args = input.substring(firstSpace + 1).trim();
        }

        Command command = commands.get(name);
        if (command == null) {
            return "未知命令: /" + name + "，输入 /help 查看可用命令";
        }

        return command.execute(args);
    }

    /**
     * 获取所有命令的帮助文本
     */
    public String helpText() {
        StringBuilder sb = new StringBuilder("可用命令：\n");
        for (Command cmd : commands.values()) {
            sb.append("  ").append(cmd.usage());
            if (!cmd.aliases().isEmpty()) {
                sb.append(" (");
                for (String alias : cmd.aliases()) {
                    sb.append("/").append(alias).append(" ");
                }
                sb.append(")");
            }
            sb.append("  — ").append(cmd.description()).append("\n");
        }
        return sb.toString();
    }
}
