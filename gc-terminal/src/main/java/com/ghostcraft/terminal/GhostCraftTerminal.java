package com.ghostcraft.terminal;

import com.ghostcraft.core.command.Command;
import com.ghostcraft.core.command.CommandRegistry;
import com.ghostcraft.core.conversation.ConversationManager;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Scanner;

@Component
public class GhostCraftTerminal {

    private static final Logger log = LoggerFactory.getLogger(GhostCraftTerminal.class);

    @Autowired
    private ConversationManager cm;

    @Autowired
    private CommandRegistry registry;

    @Autowired
    private MemoryCommand memoryCommand;

    @FunctionalInterface
    private interface CommandHandler {
        String handle(String args);
    }

    @PostConstruct
    public void init() {
        cm.createSession("default");
        loadSkills();
        loadHooks();
        registerCommands();
    }

    private void loadSkills() {
        log.info("正在加载技能包...");
        try {
            cm.getSkillRegistry().register(new com.ghostcraft.skillbasic.SystemSkill());
        } catch (Exception e) {
            log.warn("技能包加载失败: {}", e.getMessage());
        }
        log.info("技能包加载完成，共 {} 个", cm.getSkillRegistry().count());
    }

    private void loadHooks() {
        log.info("正在加载钩子...");
        cm.getHookRegistry().register(new LoggingHook());
        log.info("钩子加载完成，共 {} 个", cm.getHookRegistry().count());
    }

    private void registerCommands() {
        simple("help", "显示此帮助", args -> registry.helpText());
        simple("exit", "退出程序", args -> { System.out.println("再见！"); System.exit(0); return ""; });
        simple("clear", "清除当前会话记忆", args -> { cm.clearMemory(getSid()); return "已清除记忆"; });
        simple("count", "显示会话总数", args -> "当前共 " + cm.sessionCount() + " 个会话");
        simple("skills", "列出已加载的技能包", args -> formatSkills());
        registry.register(memoryCommand);

        registry.register(new Command() {
            public String name() { return "new"; }
            public String description() { return "创建新会话"; }
            public String usage() { return "/new <会话名称>"; }
            public String execute(String args) {
                if (args.isEmpty()) return "用法：/new <会话名称>";
                cm.createSession(args);
                return "已创建并切换到会话: " + cm.getActiveSessionId();
            }
        });

        registry.register(new Command() {
            public String name() { return "session"; }
            public String description() { return "切换会话 /session <会话ID>"; }
            public String usage() { return "/session <会话ID>"; }
            public String execute(String args) {
                if (args.isEmpty()) return "当前会话: " + cm.getActiveSessionId();
                cm.setActiveSessionId(args);
                return "已切换到会话: " + args;
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
                String activeId = cm.getActiveSessionId();
                for (var s : sessions) {
                    String marker = s.getId().equals(activeId) ? " [当前]" : "";
                    sb.append("  [").append(s.getId()).append("] ")
                            .append(s.getName()).append(marker).append("\n");
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
            sb.append("  ").append(skill.name()).append(" -- ").append(skill.description()).append("\n");
        }
        return sb.toString();
    }

    private String getSid() { return cm.getActiveSessionId(); }

    public void start() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("?? GhostCraft -- AI Agent 搭建平台");
        System.out.println("输入 /help 查看命令列表");
        System.out.println("─────────────────────────────");
        while (true) {
            System.out.print("\n你：");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;
            if (input.startsWith("/")) {
                System.out.println(registry.execute(input));
            } else {
                log.info("用户输入: {} (会话: {})", input, getSid());
                System.out.print("Agent：");
                String answer = cm.chat(getSid(), input);
                System.out.println(answer);
            }
        }
    }
}
