# [Steam Helper](https://github.com/cssxsh/steam-helper)

> 基于 [Mirai Console](https://github.com/mamoe/mirai-console) 的 [Steam](https://store.steampowered.com/) 桥接插件

[![Release](https://img.shields.io/github/v/release/cssxsh/steam-helper)](https://github.com/cssxsh/steam-helper/releases)
[![Downloads](https://img.shields.io/github/downloads/cssxsh/steam-helper/total)](https://shields.io/category/downloads)
[![MiraiForum](https://img.shields.io/badge/post-on%20MiraiForum-yellow)](https://mirai.mamoe.net/topic/287)

**使用前应该查阅的相关文档或项目**

* [User Manual](https://github.com/mamoe/mirai/blob/dev/docs/UserManual.md)
* [Permission Command](https://github.com/mamoe/mirai/blob/dev/mirai-console/docs/BuiltInCommands.md#permissioncommand)
* [Chat Command](https://github.com/project-mirai/chat-command)

认证账号后，插件会将 Steam 好友的消息会通过 Bot 推送到 QQ.

## 指令

注意: 使用前请确保可以 [在聊天环境执行指令](https://github.com/project-mirai/chat-command)
带括号的`/`前缀是可选的  
`<...>`中的是指令名，由空格隔开表示或，选择其中任一名称都可执行  
`[...]`表示参数，当`[...]`后面带`?`时表示参数可选  
`{...}`表示连续的多个参数

本插件指令权限ID 格式为 `xyz.cssxsh.mirai.plugin.steam-helper:command.*`, `*` 是指令的第一指令名  
例如 `/steam-auth` 的权限ID为 `xyz.cssxsh.mirai.plugin.steam-helper:command.steam-auth`

`target` 取值是 SteamID, 对于用户格式是 `[U:1:好友码]`, 例如 `[U:1:123456]`  
使用 `/steam-friend list` 可以看到好友的 SteamID  

### SteamAuthCommand

| 指令              | 描述          |
|:----------------|:------------|
| `/<steam-auth>` | 绑定 Steam 账号 |

### SteamFriendCommand

| 指令                                                | 描述     |
|:--------------------------------------------------|:-------|
| `/<steam-friend> <list>`                          | 列出好友列表 |
| `/<steam-friend> <ignore> [target] [ignore]?`     | 忽略好友   |
| `/<steam-friend> <nickname> [target] [nickname]?` | 设置好友昵称 |
| `/<steam-friend> <add> [target]?`                 | 添加好友   |
| `/<steam-friend> <plus> [name]?`                  | 添加好友   |
| `/<steam-friend> <remove> [target]?`              | 移除好友   |

`ignore` 取值是 `true`, `false` 

### SteamSendCommand

| 指令                       | 描述   |
|:-------------------------|:-----|
| `/<steam-send> <target>` | 发送消息 |

## 安装

### MCL 指令安装

* 预览版  
    `./mcl --update-package xyz.cssxsh:steam-helper --channel maven-prerelease --type plugin`

* 稳定版  
    `./mcl --update-package xyz.cssxsh:steam-helper --channel maven-stable --type plugin`

### 手动安装

1. 运行 [Mirai Console](https://github.com/mamoe/mirai-console) 生成`plugins`文件夹
2. 从 [Releases](https://github.com/cssxsh/steam-helper/releases) 下载`jar`并将其放入`plugins`文件夹中

## TODO

- [ ] 聊天室
- [ ] 社区提醒推送
- [ ] 云存档下载到QQ文件