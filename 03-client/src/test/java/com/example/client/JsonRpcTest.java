package com.example.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonRpcTest {

    @Test
    void request_buildsJsonRpcEnvelope() throws Exception {
        String json = JsonRpc.request(1, "tools/list", Map.of());

        JsonNode node = JsonRpc.parse(json);
        assertEquals("2.0", node.get("jsonrpc").asText());
        assertEquals(1, node.get("id").asInt());
        assertEquals("tools/list", node.get("method").asText());
        assertTrue(node.has("params"));
    }

    @Test
    void request_withParams_includesArguments() throws Exception {
        String json = JsonRpc.request(2, "tools/call",
                Map.of("name", "add_todo", "arguments", Map.of("title", "x")));

        JsonNode node = JsonRpc.parse(json);
        assertEquals("tools/call", node.get("method").asText());
        assertEquals("add_todo", node.get("params").get("name").asText());
        assertEquals("x", node.get("params").get("arguments").get("title").asText());
    }

    @Test
    void parseResponse_readsResultField() throws Exception {
        String responseLine = """
                {"jsonrpc":"2.0","id":1,"result":{"tools":[{"name":"add_todo"}]}}
                """;

        JsonNode node = JsonRpc.parse(responseLine);
        assertEquals(1, node.get("id").asInt());
        assertTrue(node.has("result"));
        assertEquals("add_todo", node.get("result").get("tools").get(0).get("name").asText());
    }
}
