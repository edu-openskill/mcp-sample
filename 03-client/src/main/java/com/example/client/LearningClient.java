package com.example.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class LearningClient {

    public static void main(String[] args) throws Exception {
        // 1) 명령행 인자: MCP Server jar 경로
        String jarPath = args.length > 0
                ? args[0]
                : Path.of("..", "02-mcp-server", "build", "libs", "mcp-server.jar")
                        .toAbsolutePath().toString();

        List<String> command = List.of("java", "-jar", jarPath);

        System.out.println("=== Learning MCP Client ===");
        System.out.println("MCP Server: " + jarPath);
        System.out.println();

        try (McpProcess mcp = new McpProcess(command)) {
            // 2) initialize 핸드셰이크
            JsonNode init = mcp.sendAndReceive(JsonRpc.request(1, "initialize", Map.of(
                    "protocolVersion", "2024-11-05",
                    "capabilities", Map.of(),
                    "clientInfo", Map.of("name", "learning-client", "version", "0.0.1")
            )));
            System.out.println("[OK] initialize → " + init.get("result").get("serverInfo").get("name").asText());
            mcp.sendNotification(JsonRpc.notification("notifications/initialized", Map.of()));
            System.out.println();

            // 3) tools/list — MCP Server에 어떤 tool이 있나?
            JsonNode tools = mcp.sendAndReceive(JsonRpc.request(2, "tools/list", Map.of()));
            System.out.println("[OK] tools/list returned " + tools.get("result").get("tools").size() + " tools");
            for (JsonNode t : tools.get("result").get("tools")) {
                System.out.println("    - " + t.get("name").asText() + " : " + t.get("description").asText());
            }
            System.out.println();

            // 4) tools/call — add_todo
            JsonNode add = mcp.sendAndReceive(JsonRpc.request(3, "tools/call", Map.of(
                    "name", "add_todo",
                    "arguments", Map.of("title", "MCP 강의 듣기", "memo", "오늘")
            )));
            System.out.println("[OK] add_todo response:");
            System.out.println(JsonRpc.pretty(add));
            System.out.println();

            // 5) tools/call — list_todos
            JsonNode list = mcp.sendAndReceive(JsonRpc.request(4, "tools/call", Map.of(
                    "name", "list_todos",
                    "arguments", Map.of()
            )));
            System.out.println("[OK] list_todos response:");
            System.out.println(JsonRpc.pretty(list));
            System.out.println();

            System.out.println("=== End of learning scenario ===");
            System.out.println("이 메시지들이 MCP의 전부입니다.");
            System.out.println("Claude Code도 같은 메시지를 같은 MCP Server에 보냅니다.");
        }
    }
}
