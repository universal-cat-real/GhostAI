package com.ghostcraft.terminal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * GhostCraft 启动入口
 */
@SpringBootApplication(scanBasePackages = "com.ghostcraft")
public class GhostCraftApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(GhostCraftApplication.class, args);
        GhostCraftTerminal terminal = ctx.getBean(GhostCraftTerminal.class);
        terminal.start();
    }
}
