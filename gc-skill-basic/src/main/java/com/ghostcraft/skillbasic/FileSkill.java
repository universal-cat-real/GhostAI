package com.ghostcraft.skillbasic;

import com.ghostcraft.core.skill.Skill;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文件操作技能包
 */
@Component
public class FileSkill implements Skill {

    @Override
    public String name() { return "file"; }

    @Override
    public String description() { return "文件操作：列出目录、读取文件内容"; }

    @Override
    public Object toolInstance() { return this; }

    @Tool("列出指定目录下的文件和子目录")
    public String listDirectory(@P("目录路径，如 D:/java_study") String path) {
        try (Stream<Path> paths = Files.list(Paths.get(path))) {
            return paths.map(p -> {
                String type = Files.isDirectory(p) ? "[目录]" : "[文件]";
                try {
                    long size = Files.size(p);
                    return type + " " + p.getFileName() + " (" + size + " 字节)";
                } catch (IOException e) {
                    return type + " " + p.getFileName();
                }
            }).collect(Collectors.joining("\n"));
        } catch (IOException e) {
            return "读取目录失败: " + e.getMessage();
        }
    }

    @Tool("读取指定文件的内容（文本文件），返回前 2000 字")
    public String readFile(@P("文件路径，如 D:/java_study/README.md") String path) {
        try {
            String content = Files.readString(Paths.get(path));
            if (content.length() > 2000) {
                content = content.substring(0, 2000) + "\n\n... (截断，仅显示前 2000 字)";
            }
            return content;
        } catch (IOException e) {
            return "读取文件失败: " + e.getMessage();
        }
    }
}