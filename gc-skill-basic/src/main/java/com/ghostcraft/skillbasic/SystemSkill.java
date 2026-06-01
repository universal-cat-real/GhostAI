package com.ghostcraft.skillbasic;

import com.ghostcraft.core.skill.Skill;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

/**
 * 系统信息技能包
 *
 * 作用：提供系统相关的工具，如查看时间、系统属性等。
 * 演示一个技能包怎么写，后续可以加更多工具。
 */
public class SystemSkill implements Skill {

    @Override
    public String name() {
        return "system";
    }

    @Override
    public String description() {
        return "系统工具：时间、日期、系统属性";
    }

    @Override
    public Object toolInstance() {
        return this;
    }

    @Tool("获取当前日期和时间，可以传入时区，如 Asia/Shanghai 或 UTC")
    public String getCurrentTime(@P("时区 ID，如 Asia/Shanghai 或 UTC，不传默认系统时区") String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return java.time.LocalDateTime.now().toString();
        }
        return java.time.ZonedDateTime.now(java.time.ZoneId.of(timezone)).toString();
    }

    @Tool("获取系统属性，如 os.name、java.version、user.name")
    public String getSystemProperty(@P("属性名，如 os.name") String key) {
        return System.getProperty(key, "未找到: " + key);
    }
}

