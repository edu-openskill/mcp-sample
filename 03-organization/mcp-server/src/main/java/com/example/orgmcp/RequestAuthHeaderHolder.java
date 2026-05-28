package com.example.orgmcp;

/**
 * incoming MCP HTTP 요청의 Authorization 헤더를 downstream Resource Server 호출까지 전달하기 위한 holder.
 * AuthForwardInterceptor가 set, TodoRestClient가 read, finally에서 clear.
 */
public final class RequestAuthHeaderHolder {
    private static final ThreadLocal<String> AUTH = new ThreadLocal<>();

    private RequestAuthHeaderHolder() {}

    public static void set(String header) { AUTH.set(header); }
    public static String get() { return AUTH.get(); }
    public static void clear() { AUTH.remove(); }
}
