package com.ghostcraft.core.skill;

/**
 * 技能包接口
 *
 * 作用：每个技能包实现这个接口。
 * 一个技能包可以包含多个 @Tool 方法。
 * 系统启动时扫描所有技能包，把工具注册给 Agent。
 */
public interface Skill {

    /** 技能包名称，如 "system"、"file"、"network" */
    String name();

    /** 技能包描述 */
    String description();

    /** 获取这个技能包含有的工具对象（@Tool 注解所在的实例） */
    Object toolInstance();
}
