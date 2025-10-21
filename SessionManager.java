import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SessionManager {
    private static final Map<String, String> sessions = new HashMap<>();
    // Create new session
    public static String createSession(String username) {
        // Invalidate any exsisting session for this user
        sessions.entrySet().removeIf(entry -> entry.getValue().equals(username));
        // Create new session
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, username);
        System.out.println("Session created:");
        System.out.println(sessions);
        return sessionId;
    }
    // Get username using session id
    public static String getUsername(String SessionId){
        return sessions.get(SessionId);
    }
    // Invalidate session
    public static void invalidateSession(String sessionId){
        sessions.remove(sessionId);
    }
}
