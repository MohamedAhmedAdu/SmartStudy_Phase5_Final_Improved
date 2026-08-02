package com.smartstudy.service;
import com.smartstudy.config.AppConfig;
import com.smartstudy.model.SessionPrincipal;
import java.time.Duration;
import java.time.Instant;
public final class SessionManager {
    private static final SessionManager INSTANCE = new SessionManager();
    private SessionPrincipal principal; private Instant lastActivity;
    private SessionManager() {}
    public static SessionManager get() { return INSTANCE; }
    public synchronized void login(SessionPrincipal p) { principal = p; touch(); }
    public synchronized void logout() { principal = null; lastActivity = null; }
    public synchronized void touch() { if (principal != null) lastActivity = Instant.now(); }
    public synchronized SessionPrincipal requireActive() {
        if (principal == null) throw new IllegalStateException("No active session.");
        int minutes = AppConfig.getInt("session.timeout.minutes", 15);
        if (lastActivity != null && Duration.between(lastActivity, Instant.now()).toMinutes() >= minutes) { logout(); throw new IllegalStateException("Session expired after inactivity."); }
        touch(); return principal;
    }
    public synchronized void updateName(String name) {
        if (principal != null) {
            principal = new SessionPrincipal(principal.id(), name, principal.email(), principal.role());
            touch();
        }
    }
    public synchronized SessionPrincipal current() { return principal; }
}
