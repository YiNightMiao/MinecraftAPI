package me.yinight;

import java.io.IOException;
import java.net.ServerSocket;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.ChatColor;

public class MinecraftAPI extends JavaPlugin {
    private static MinecraftAPI instance;
    private HttpServerThread httpThread;
    private boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean debugMode = false;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info(ChatColor.GREEN + "[YiNightProject] 喵！MinecraftAPI 插件加载成功！");
        int port = getConfig().getInt("port");
        if (isPortAvailable(port)) {
            httpThread = new HttpServerThread();
            httpThread.start();
        }
        else {
            getLogger().info("[YiNightProject]请检查端口是否占用 你使用的端口:"+port+"强制启动请在插件加载完成后输入/minecraftapi start");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info(ChatColor.RED + "[YiNightProject] 喵！MinecraftAPI 已经卸载辣！");
        int port = getConfig().getInt("port");
        if (isPortAvailable(port)) {
            getLogger().info("[MinecraftAPI]服务未启动 无需停止运行");
        }
        else {
            httpThread.shutdown();
            getLogger().info("服务已停止运行");
        }
    }
        

    @Override
public void reloadConfig() {
    super.reloadConfig(); 
    int port = getConfig().getInt("port");

    if (httpThread != null) {
        httpThread.shutdown();
        httpThread = null;
    }

    if (isPortAvailable(port)) {
        httpThread = new HttpServerThread();
        httpThread.start();
        getLogger().info("[MinecraftAPI] 服务已启动，监听端口: " + port);
    } else {
        getLogger().warning("[MinecraftAPI] 端口 " + port + " 被占用，无法启动服务");
        getLogger().warning("你可以稍后使用 /minecraftapi start 来尝试强制启动");
    }
}

    public static MinecraftAPI getInstance() {
        return instance;
    }

     public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("minecraftapi")) {

            if (args.length == 1 && args[0].equalsIgnoreCase("debug")) {
                debugMode = !debugMode;
                getLogger().info((debugMode ? ChatColor.GREEN : ChatColor.RED) + "[Debug] debug模式已" + (debugMode ? "打开" : "关闭"));
                return true;
            }

            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "[MinecraftAPI] 喵~配置重载成功了啦");
                return true;
            }

            if (args.length == 1 && args[0].equalsIgnoreCase("stop")) {
                int port = getConfig().getInt("port", 8080);
                if (isPortAvailable(port)) {
                    getLogger().info(ChatColor.RED + "[MinecraftAPI] 服务未启动 无法停止");
                    return true;
                    
                } else {
                    httpThread.shutdown();
                    getLogger().info(ChatColor.RED + "[MinecraftAPI] 服务已经停止运行");
                    return true;
                    
                }
                
            }

            if (args.length == 1 && args[0].equalsIgnoreCase("start")) {
                int port = getConfig().getInt("port", 8080);
                if (isPortAvailable(port)) {
                    httpThread = new HttpServerThread();
                    httpThread.start();
                    getLogger().info(ChatColor.GREEN + "[MinecraftAPI] 服务已启动");
                    return true;
                } else {
                    getLogger().info(ChatColor.RED + "[MinecraftAPI] 哎呀 已经在运行啦 不能再次启动哦~");
                    return true;
                }
                
            }

            if (args.length == 1 && args[0].equalsIgnoreCase("check")) {
                int port = getConfig().getInt("port",8080);
                if (isPortAvailable(port)) {
                    getLogger().info(ChatColor.GREEN + "[MinecraftAPI] 服务正在运行。你可以使用 /minecraftapi stop 关闭它");
                } else {
                    getLogger().info(ChatColor.RED + "[MinecraftAPI] 服务没有运行！！请使用 /minecraftapi start 打开它");
                }
                return true;
            }

            sender.sendMessage(ChatColor.YELLOW + "使用 /minecraftapi debug 启动调试模式，/minecraftapi reload 重载配置，/minecraftapi start|stop|check 启动、停止或检查服务状态");
            return true;
        }
        return false;
    }
}
