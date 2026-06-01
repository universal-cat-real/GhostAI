# GhostCraft

AI Agent 搭建平台 — 基于 LangChain4j。命令行多轮对话，支持技能包、MCP、子任务、多 Agent 协作。

## 项目结构

```
gc-core/         核心框架 — 会话管理、消息总线、模型定义
gc-terminal/     命令行终端
gc-memory/       记忆管理 — token 监控、压缩、持久化
gc-skill/        技能包加载框架
gc-skill-basic/  内置基础技能包
gc-hook/         生命周期钩子
gc-security/     权限防御
gc-mcp/          MCP 协议
gc-subagent/     子任务分发
gc-team/         团队协作
```

## 构建

```bash
mvn compile -pl gc-terminal -am
```

## 运行

```bash
mvn exec:java -pl gc-terminal -am "-Dexec.mainClass=com.ghostcraft.terminal.GhostCraftTerminal"
```
