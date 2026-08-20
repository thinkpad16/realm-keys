package net.realm_keys.util;

public class PortalTeleportContext {
    private static final ThreadLocal<Boolean> IS_PORTAL_TELEPORT = ThreadLocal.withInitial(() -> false);

    public static boolean isPortalTeleport() {
        return IS_PORTAL_TELEPORT.get();
    }

    public static void setPortalTeleport(boolean value) {
        IS_PORTAL_TELEPORT.set(value);
    }
}