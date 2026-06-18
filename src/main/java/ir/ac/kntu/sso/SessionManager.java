package ir.ac.kntu.sso;

import java.time.LocalDateTime;

import ir.ac.kntu.persona.Persona;

public class SessionManager {

    private static String currentEmail;
    private static String sessionToken;
    private static LocalDateTime loginTime;

    public static void createSession(Persona persona) {
        if (persona == null) {
            throw new IllegalArgumentException("Cannot create a session for a null user");
        }
        currentEmail = persona.getEmail() != null ? persona.getEmail() : persona.getUsername();
        sessionToken = "SES-" + ((int) (Math.random() * 900_000) + 100_000);
        loginTime = LocalDateTime.now();
        Persona.setCurrentUser(persona);
    }

    public static boolean isActive() {
        return sessionToken != null;
    }

    public static String getSessionToken() {
        return sessionToken;
    }

    public static String getCurrentEmail() {
        return currentEmail;
    }

    public static LocalDateTime getLoginTime() {
        return loginTime;
    }

    public static void destroySession() {
        currentEmail = null;
        sessionToken = null;
        loginTime = null;
        Persona.setCurrentUser(null);
    }
}
