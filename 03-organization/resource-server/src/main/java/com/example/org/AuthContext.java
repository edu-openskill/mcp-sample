package com.example.org;

/**
 * 현재 HTTP 요청의 인증된 userId를 ThreadLocal로 보관.
 * HandlerInterceptor가 set, Controller/Repository가 read, finally에서 clear.
 *
 * 단일 토큰 매핑 (in-memory, 강의용):
 *   alice-token -> alice
 *   bob-token   -> bob
 */
public final class AuthContext {

    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();

    private AuthContext() {}

    public static void set(String userId) {
        USER_ID.set(userId);
    }

    public static String getOrThrow() {
        String id = USER_ID.get();
        if (id == null) {
            throw new IllegalStateException("No authenticated userId in context");
        }
        return id;
    }

    public static void clear() {
        USER_ID.remove();
    }

    /** 강의용 단순 토큰 매핑. 운영에서는 JWT/OAuth 검증으로 대체. */
    public static String resolveUserId(String bearerToken) {
        if (bearerToken == null) return null;
        return switch (bearerToken) {
            case "alice-token" -> "alice";
            case "bob-token" -> "bob";
            default -> null;
        };
    }
}
