package com.ghostcraft.terminal;

import com.ghostcraft.core.command.Command;
import com.ghostcraft.core.command.CommandRegistry;
import com.ghostcraft.core.conversation.ConversationManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Scanner;

@Component
public class GhostCraftTerminal {

    @Autowired
    private ConversationManager cm;

    @Autowired
    private CommandRegistry registry;

    private String currentSessionId;

    @FunctionalInterface
    private interface CommandHandler {
        String handle(String args);
    }

    @PostConstruct
    public void init() {
        this.currentSessionId = cm.createSession("default");
        loadSkills();
        loadHooks();
        registerCommands();
    }

    private void loadSkills() {
        System.out.println("正在加载技能包...");
        try {
            cm.getSkillRegistry().register(new com.ghostcraft.skillbasic.SystemSkill());
        } catch (Exception e) {
            System.out.println("  [技能] 加载失败: " + e.getMessage());
        }
        System.out.println("技能包加载完成，共 " + cm.getSkillRegistry().count() + " 个");
    }

    private void loadHooks() {
        System.out.println("正在加载钩子...");
        cm.getHookRegistry().register(new LoggingHook());
        System.out.println("钩子加载完成，共 " + cm.getHookRegistry().count() + " 个");
    }

    private void registerCommands() {
        simple("help", "显示此帮助", args -> registry.helpText());
        simple("exit", "退出程序", args -> { System.out.println("再见！"); System.exit(0); return ""; });
        simple("clear", "清除当前会话记忆", args -> { cm.clearMemory(currentSessionId); return "已清除记忆"; });
        simple("count", "显示会话总数", args -> "当前共 " + cm.sessionCount() + " 个会话");
        simple("skills", "列出已加载的技能包", args -> formatSkills());
        registry.register(new Command() {
            public String name() { return "new"; }
            public String description() { return "创建新会话"; }
            public String usage() { return "/new <会话名称>"; }
            public String execute(String args) {
                if (args.isEmpty()) return "用法：/new <会话名称>";
                String id = cm.createSession(args);
                currentSessionId = id;
                return "已创建会话 [" + args + "]，ID: " + id;
            }
        });

        registry.register(new Command() {
            public String name() { return "sessions"; }
            public List<String> aliases() { return List.of("ls"); }
            public String description() { return "列出所有会话"; }
            public String execute(String args) {
                var sessions = cm.listSessions();
                if (sessions.isEmpty()) return "暂无会话";
                StringBuilder sb = new StringBuilder("会话列表：\n");
                for (var s : sessions) {
                    sb.append("  [").append(s.getId()).append("] ").append(s.getName()).append("\n");
                }
                return sb.toString();
            }
        });
    }

    private void simple(String name, String description, CommandHandler handler) {
        registry.register(new Command() {
            public String name() { return name; }
            public String description() { return description; }
            public String execute(String args) { return handler.handle(args); }
        });
    }

    private String formatSkills() {
        var list = cm.getSkillRegistry().list();
        if (list.isEmpty()) return "未加载任何技能包";
        StringBuilder sb = new StringBuilder("已加载的技能包：\n");
        for (var skill : list) {
            sb.append("  ").append(skill.name()).append(" — ").append(skill.description()).append("\n");
        }
        return sb.toString();
    }

    public void start() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("🐙 GhostCraft — AI Agent 搭建平台");
        System.out.println("输入 /help 查看命令列表");
        System.out.println("─────────────────────────────");
        while (true) {
            System.out.print("\n你：");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;
            if (input.startsWith("/")) {
                System.out.println(registry.execute(input));
            } else {
                System.out.print("Agent：");
                System.out.println(cm.chat(currentSessionId, input));
            }
        }
    }
}
