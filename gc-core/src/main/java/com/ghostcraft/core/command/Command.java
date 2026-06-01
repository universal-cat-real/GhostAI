package com.ghostcraft.core.command;

import java.util.List;

/**
 * 命令接口
 *
 * 作用：所有 / 开头的命令都实现这个接口。
 * 支持别名和参数说明。
 */
public interface Command {

    /** 命令主名称，如 "help" */
    String name();

    /** 别名，如 /sessions 的别名可以是 /ls */
    default List<String> aliases() {
        return List.of();
    }

    /** 命令描述 */
    String description();

    /** 参数用法说明，如 "/new <名称>" */
    default String usage() {
        return "/" + name();
    }

    /** 执行命令 */
    String execute(String args);
}
