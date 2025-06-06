# MinecraftAPI Plugin

**MinecraftAPI** is a lightweight plugin for Minecraft servers (Bukkit/Spigot/Paper) that allows remote control via a simple HTTP interface. It supports executing commands, retrieving online players, and monitoring server performance metrics like TPS and memory usage.

## ✨ Features

- Execute server commands via HTTP `POST` requests
- Retrieve the list of online players and count
- Query server status: TPS, memory usage, and CPU usage
- Option to restrict access to localhost only (`127.0.0.1`)
- Uses `application/x-www-form-urlencoded` format (no JSON required)

## 📦 Installation

1. Place the compiled `.jar` file into your server's `plugins/` directory.
2. Restart the server to generate the configuration file.
3. Edit the `config.yml` file:
   ```yaml
   host: 127.0.0.1
   port: 8080
   server_key: your_secret_key
   allow_remote: false
