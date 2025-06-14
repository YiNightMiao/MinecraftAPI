package me.yinight;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class HttpServerThread extends Thread {
    private volatile boolean running = true;
    private HttpServer server;

    @Override
    public void run() {
        try {
            String host = MinecraftAPI.getInstance().getConfig().getString("host", "127.0.0.1");
            int port = MinecraftAPI.getInstance().getConfig().getInt("port", 8080);

            if (!isPortAvailable(port)) {
                MinecraftAPI.getInstance().getLogger().warning("端口 " + port + " 已被占用，服务未启动。");
                return;
            }

            server = HttpServer.create(new InetSocketAddress(host, port), 0);
            server.createContext("/", this::handleRequest);
            server.setExecutor(null);
            server.start();

            MinecraftAPI.getInstance().getLogger().info("[MinecraftAPI] 服务已启动，监听端口: " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        running = false;
        if (server != null) {
            server.stop(0);
        }
    }

    private void handleCORS(HttpExchange exchange) throws IOException {
        String domain = MinecraftAPI.getInstance().getConfig().getString("Allow-domain", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", domain);
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
        }
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        handleCORS(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            return;
        }

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

        String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));

        if (MinecraftAPI.debugMode) {
            MinecraftAPI.getInstance().getLogger().info("[DEBUG] 请求体: " + body);
        }

        String[] params = body.split("&");
        String type = "", message = "", key = "";
        for (String param : params) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2) {
                String k = URLDecoder.decode(pair[0], "UTF-8");
                String v = URLDecoder.decode(pair[1], "UTF-8");
                switch (k) {
                    case "type": type = v; break;
                    case "message": message = v; break;
                    case "key": key = v; break;
                }
            }
        }

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
                final String commandToRun = message;
                Bukkit.getScheduler().runTask(MinecraftAPI.getInstance(), () -> {
                    boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandToRun);
                    if (MinecraftAPI.debugMode) {
                        MinecraftAPI.getInstance().getLogger().info("[DEBUG] 命令执行成功: " + success);
                    }
                });
                sendResponse(exchange, 200, "命令执行完成。");
                break;
            }
            case "getplayer": {
                List<Player> players = Bukkit.getOnlinePlayers().stream().collect(Collectors.toList());
                String names = players.stream().map(Player::getName).collect(Collectors.joining(","));
                String response = "online-player=" + names + "\nsize=" + players.size();
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
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
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

    private boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
