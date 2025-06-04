package me.yinight;

import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;
public class MinecraftAPI extends JavaPlugin {
    private static MinecraftAPI instance;

    public static boolean debugMode = false;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        getCommand("minecraftapi").setExecutor(this::onCommand);
        getLogger().info("§a[YiNightProject]喵呜！MinecraftAPI 插件加载成功！");
        new HttpServerThread().start();
    }
    @Override
    public void onDisable() {
        getLogger().info("§c[YiNightProject]喵呜！MinecraftAPI 已经卸载辣！");
    }
    public static MinecraftAPI getInstance() {
        return instance;
    }
    

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("debug")) {
            debugMode = !debugMode;
            sender.sendMessage("§a[MinecraftAPI]哇！你是伟大的开发者吗 Debug模式已经帮你 " + (debugMode ? "§a开启" : "§c关闭"));
            getLogger().info("[DEBUG]嘿嘿 已经帮你 " + (debugMode ? "开启" : "关闭") + "（由 " + sender.getName() + " 切换）");
            return true;
        }
        if (label.equalsIgnoreCase("minecraftapi")) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            this.reloadConfig();
            sender.sendMessage("§a[MinecraftAPI] 喵呜~配置重载成功了啦");
            return true;
        }

        sender.sendMessage("§e使用/minecraftapi debug启动调试模式 使用/minecraftapi relaod 重载你的配置");
        return true;
    }
  
    return false;
}

}
