package me.yinight;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.stream.Collectors;

public class HttpServerThread extends Thread {
    @Override
    public void run() {
        try {
            String host = MinecraftAPI.getInstance().getConfig().getString("host", "127.0.0.1");
            int port = MinecraftAPI.getInstance().getConfig().getInt("port", 8080);

            HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
            server.createContext("/command", this::handleRequest);
            server.setExecutor(null);
            server.start();

            MinecraftAPI.getInstance().getLogger().info("HTTP服务启动成功啦 " + host + ":" + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
    if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
        sendResponse(exchange, 405, "Method Not Allowed");
        return;
    }

    String remoteIP = exchange.getRemoteAddress().getAddress().getHostAddress();
    boolean allowRemote = MinecraftAPI.getInstance().getConfig().getBoolean("allow_remote", false);
    if (!allowRemote && !remoteIP.equals("127.0.0.1")) {
        sendResponse(exchange, 403, "Remote access not allowed");
        return;
    }

    StringBuilder body = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), "UTF-8"))) {
        String line;
        while ((line = reader.readLine()) != null) {
            body.append(line);
        }
    }

    if (MinecraftAPI.debugMode) {
        MinecraftAPI.getInstance().getLogger().info("[DEBUG] 请求体: " + body.toString());
    }

    // URL 参数解析
    String[] params = body.toString().split("&");
    String type = "", message = "", key = "";
    for (String param : params) {
        String[] pair = param.split("=", 2);
        if (pair.length == 2) {
            String k = java.net.URLDecoder.decode(pair[0], "UTF-8");
            String v = java.net.URLDecoder.decode(pair[1], "UTF-8");
            switch (k) {
                case "type": type = v; break;
                case "message": message = v; break;
                case "key": key = v; break;
            }
        }
    }

    // 验证 key
    String configKey = MinecraftAPI.getInstance().getConfig().getString("server_key");
    if (!key.equals(configKey)) {
        sendResponse(exchange, 403, "Invalid key");
        return;
    }

    switch (type.toLowerCase()) {
        case "command": {
            if (message.trim().isEmpty()) {
                sendResponse(exchange, 400, "Empty command");
                return;
            }
        final String CommandToRun = message;
            Bukkit.getScheduler().runTask(MinecraftAPI.getInstance(), () -> {
                boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), CommandToRun);
                if (MinecraftAPI.debugMode) {
                    MinecraftAPI.getInstance().getLogger().info("[DEBUG] 命令执行成功? " + success);
                }
            });

            sendResponse(exchange, 200, "Command executed");
            break;
        }
        case "getplayer": {
            List<Player> players = (List<Player>) Bukkit.getOnlinePlayers();
            String names = players.stream().map(Player::getName).collect(Collectors.joining(","));
            String response = "online-player=" + names + "\ncount=" + players.size();
            sendResponse(exchange, 200, response);
            break;
        }
        case "getstatus": {
            double tps = Bukkit.getTPS()[0];
            long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long maxMemory = Runtime.getRuntime().maxMemory();
            double cpuLoad = getSystemCpuLoad();

            String response = String.format("tps=%.2f\nmemory=%s / %s\ncpu=%.2f%%",
                    tps,
                    formatMemory(usedMemory),
                    formatMemory(maxMemory),
                    cpuLoad * 100);
            sendResponse(exchange, 200, response);
            break;
        }
        default:
            sendResponse(exchange, 400, "Invalid type");
            break;
    }
}


    private void sendResponse(HttpExchange exchange, int code, String message) throws IOException {
        byte[] bytes = message.getBytes("UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private double getSystemCpuLoad() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        try {
            return (double) osBean.getClass().getMethod("getSystemCpuLoad").invoke(osBean);
        } catch (Exception e) {
            return -1;
        }
    }

    private String formatMemory(long bytes) {
        return String.format("%.2f MB", bytes / 1024.0 / 1024.0);
    }
}
