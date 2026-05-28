package com.example.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;

public final class JsonRpc {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonRpc() {}

    public static String request(int id, String method, Map<String, ?> params) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("jsonrpc", "2.0");
        node.put("id", id);
        node.put("method", method);
        node.set("params", MAPPER.valueToTree(params));
        return node.toString();
    }

    public static String notification(String method, Map<String, ?> params) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("jsonrpc", "2.0");
        node.put("method", method);
        node.set("params", MAPPER.valueToTree(params));
        return node.toString();
    }

    public static JsonNode parse(String line) throws Exception {
        return MAPPER.readTree(line);
    }

    public static String pretty(JsonNode node) throws Exception {
        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    }
}
