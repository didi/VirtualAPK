package android.content.res;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by qiaopu on 2018/5/3.
 */
public final class ResourcesKey {
    @Nullable
    public final String mResDir;
    
    @Nullable
    public final String[] mSplitResDirs;
    
    @Nullable
    public final String[] mOverlayDirs;
    
    @Nullable
    public final String[] mLibDirs;
    
    public final int mDisplayId;
    
    @NonNull
    public final Configuration mOverrideConfiguration;
    
    @NonNull
    public final CompatibilityInfo mCompatInfo;
    
    public ResourcesKey(@Nullable String resDir,
                        @Nullable String[] splitResDirs,
                        @Nullable String[] overlayDirs,
                        @Nullable String[] libDirs,
                        int displayId,
                        @Nullable Configuration overrideConfig,
                        @Nullable CompatibilityInfo compatInfo) {
        throw new RuntimeException("Stub!");
    }
    
}

