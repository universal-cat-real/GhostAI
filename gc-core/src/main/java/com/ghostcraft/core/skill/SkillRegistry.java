package com.ghostcraft.core.skill;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 技能包注册器
 *
 * 作用：管理所有已加载的技能包。
 * 提供技能包列表，供 ConversationManager 在创建 Agent 时注册工具。
 */
public class SkillRegistry {

    private final Map<String, Skill> skills = new LinkedHashMap<>();

    /** 注册一个技能包 */
    public void register(Skill skill) {
        skills.put(skill.name(), skill);
        System.out.println("  [技能] 已加载: " + skill.name() + " — " + skill.description());
    }

    /** 获取所有工具实例，用于注册给 AiServices */
    public List<Object> allToolInstances() {
        return skills.values().stream()
                .map(Skill::toolInstance)
                .toList();
    }

    /** 获取指定技能包 */
    public Skill get(String name) {
        return skills.get(name);
    }

    /** 列出所有已加载的技能 */
    public List<Skill> list() {
        return List.copyOf(skills.values());
    }

    /** 技能包数量 */
    public int count() {
        return skills.size();
    }
}