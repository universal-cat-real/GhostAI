package com.ghostcraft.core.skill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SkillRegistry {

    private static final Logger log = LoggerFactory.getLogger(SkillRegistry.class);
    private final Map<String, Skill> skills = new LinkedHashMap<>();

    public void register(Skill skill) {
        skills.put(skill.name(), skill);
        log.info("已加载技能包: {} — {}", skill.name(), skill.description());
    }

    public List<Object> allToolInstances() {
        return skills.values().stream().map(Skill::toolInstance).toList();
    }

    public Skill get(String name) { return skills.get(name); }
    public List<Skill> list() { return List.copyOf(skills.values()); }
    public int count() { return skills.size(); }
}
