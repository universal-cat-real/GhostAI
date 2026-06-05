package com.ghostcraft.core.skill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FileSkillLoader {

    private static final Logger log = LoggerFactory.getLogger(FileSkillLoader.class);
    private static final Path SKILL_DIR = Paths.get(System.getProperty("user.home"), ".ghostcraft", "skills");

    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private volatile long lastScanTime = 0;

    public FileSkillLoader() {
        try {
            Files.createDirectories(SKILL_DIR);
        } catch (IOException e) {
            log.warn("创建技能目录失败: {}", e.getMessage());
        }
    }

    public void scan() {
        cache.clear();
        File[] dirs = SKILL_DIR.toFile().listFiles(File::isDirectory);
        if (dirs == null) return;

        for (File dir : dirs) {
            File skillFile = new File(dir, "SKILL.md");
            if (!skillFile.exists()) continue;
            try {
                String content = Files.readString(skillFile.toPath());
                // 替换{{为{和{之间加零宽空格，避免被PromptTemplate当成模板变量
                content = content.replace("{{", "{\u200B{");
                content = content.replace("}}", "}\u200B}");
                cache.put(dir.getName(), content);
                log.info("已加载技能 [{}]", dir.getName());
            } catch (IOException e) {
                log.warn("读取技能 [{}] 失败: {}", dir.getName(), e.getMessage());
            }
        }
        lastScanTime = System.currentTimeMillis();
        log.info("技能加载完成，共 {} 个", cache.size());
    }

    public String getAllPrompts() {
        File[] dirs = SKILL_DIR.toFile().listFiles(File::isDirectory);
        int currentCount = dirs != null ? dirs.length : 0;
        if (currentCount != cache.size() || System.currentTimeMillis() - lastScanTime > 30000) {
            scan();
        }
        if (cache.isEmpty()) return "";

        StringBuilder sb = new StringBuilder("\n\n## 可用技能\n");
        sb.append("以下技能已加载，根据用户需求选择合适的技能使用：\n\n");
        for (Map.Entry<String, String> entry : cache.entrySet()) {
            sb.append("### ").append(entry.getKey()).append("\n");
            sb.append(entry.getValue()).append("\n\n");
        }
        return sb.toString();
    }

    public void reload() { scan(); }
    public Set<String> listSkills() { return cache.keySet(); }
}
