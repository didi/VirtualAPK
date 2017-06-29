package android.content.pm;

import android.util.ArraySet;

/**
 * @author johnsonlee
 */
public class PackageUserState {

    public boolean stopped;
    public boolean notLaunched;
    public boolean installed;
    public boolean hidden; // Is the app restricted by owner / admin
    public int enabled;
    public boolean blockUninstall;

    public String lastDisableAppCaller;

    public ArraySet<String> disabledComponents;
    public ArraySet<String> enabledComponents;

    public int domainVerificationStatus;
    public int appLinkGeneration;

    public PackageUserState() {
        throw new RuntimeException("Stub!");
    }

    public PackageUserState(final PackageUserState o) {
        throw new RuntimeException("Stub!");
    }

}
