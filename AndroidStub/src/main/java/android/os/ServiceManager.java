package android.os;

import java.util.HashMap;
import java.util.Map;

/**
 * @author johnsonlee
 */
public final class ServiceManager {

    private static HashMap<String, IBinder> sCache = new HashMap<String, IBinder>();

    public static IBinder getService(final String name) {
        throw new RuntimeException("Stub!");
    }

    public static void addService(final String name, final IBinder service) {
        throw new RuntimeException("Stub!");
    }

    public static void addService(final String name, final IBinder service, final boolean allowIsolated) {
        throw new RuntimeException("Stub!");
    }

    public static IBinder checkService(final String name) {
        throw new RuntimeException("Stub!");
    }

    public static String[] listServices() throws RemoteException {
        throw new RuntimeException("Stub!");
    }

    public static void initServiceCache(final Map<String, IBinder> cache) {
        throw new RuntimeException("Stub!");
    }

}
