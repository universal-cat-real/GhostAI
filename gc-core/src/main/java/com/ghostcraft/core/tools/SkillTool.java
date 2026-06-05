package com.ghostcraft.core.tools;

import com.ghostcraft.core.skill.FileSkillLoader;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
public class SkillTool implements GhostTool{

    @Autowired
    private FileSkillLoader fileSkillLoader;

    @Tool("用户获取Agent安装的skill列表和简要描述，返回给用户安装的skill列表以及简要描述")
    public Map<String, String> listSkillAndDescription(){
        Map<String, String> result = new LinkedHashMap<>();
        Set<String> skillsList = fileSkillLoader.listSkills();
        for (String skill : skillsList) {
            Path skillFile = Paths.get(
                    System.getProperty("user.home"), ".ghostcraft", "skills", skill, "SKILL.md");
            try {
                if (Files.exists(skillFile)) {
                    String content = Files.readString(skillFile);
                    result.put(skill, content);
                } else {
                    result.put(skill, "SKILL.md 文件不存在");
                }
            } catch (Exception e) {
                result.put(skill, "读取失败: " + e.getMessage());
            }
        }
        return result;
    }
    
    @Tool("用户获取Agent安装的skill列表")
    public String getSkillList(){
        Set<String> skillList = fileSkillLoader.listSkills();
        if(skillList.isEmpty()){
            return "没有安装任何skills";
        }else {
            return skillList.toString();
        }
    }
}
