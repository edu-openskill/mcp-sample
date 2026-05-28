package com.example.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * MCP Server를 자식 프로세스로 띄우고 stdin/stdout으로 JSON-RPC를 주고받는다.
 * 학생이 이 클래스의 코드를 보면 "MCP가 결국 stdin/stdout으로 JSON 한 줄씩 주고받는 거구나"를 깨닫는다.
 */
public class McpProcess implements AutoCloseable {

    private final Process process;
    private final BufferedWriter stdin;
    private final BufferedReader stdout;

    public McpProcess(List<String> command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false); // stderr는 별도로 (MCP Server의 로그는 파일로 가있음)
        this.process = pb.start();
        this.stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        this.stdout = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
    }

    /** JSON-RPC 메시지를 한 줄로 보내고 응답 한 줄을 읽어온다. */
    public JsonNode sendAndReceive(String jsonRpcLine) throws Exception {
        System.out.println("→ " + jsonRpcLine);
        stdin.write(jsonRpcLine);
        stdin.newLine();
        stdin.flush();

        String response = stdout.readLine();
        if (response == null) {
            throw new IOException("MCP Server가 응답을 닫았습니다. logs/mcp-server.log를 확인하세요.");
        }
        System.out.println("← " + response);
        return JsonRpc.parse(response);
    }

    /** 알림(notification)은 응답 없음. */
    public void sendNotification(String jsonRpcLine) throws Exception {
        System.out.println("→ " + jsonRpcLine + "  (notification)");
        stdin.write(jsonRpcLine);
        stdin.newLine();
        stdin.flush();
    }

    @Override
    public void close() throws Exception {
        try {
            stdin.close();
        } catch (Exception ignored) {}
        process.waitFor();
    }
}
