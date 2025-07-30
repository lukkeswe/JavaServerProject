import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SessionManager {
    private static final Map<String, String> sessions = new HashMap<>();

    public static String createSession(String username) {
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, username);
        System.out.println("Session created:");
        System.out.println(sessions);
        return sessionId;
    }

    public static String getUsername(String SessionId){
        return sessions.get(SessionId);
    }

    public static void invalidateSession(String sessionId){
        sessions.remove(sessionId);
    }
}
