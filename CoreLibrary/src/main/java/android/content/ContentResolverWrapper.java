package android.content;

import android.annotation.TargetApi;
import android.os.Build;

/**
 * Wrapper of {@link ContentResolver}
 * Created by qiaopu on 2018/5/7.
 */
public abstract class ContentResolverWrapper extends ContentResolver {
    
    ContentResolver mBase;
    
    public ContentResolverWrapper(Context context) {
        super(context);
        mBase = context.getContentResolver();
    }
    
    @Override
    protected IContentProvider acquireProvider(Context context, String auth) {
        return mBase.acquireProvider(context, auth);
    }
    
    @Override
    protected IContentProvider acquireExistingProvider(Context context, String auth) {
        return mBase.acquireExistingProvider(context, auth);
    }
    
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected IContentProvider acquireUnstableProvider(Context context, String auth) {
        return mBase.acquireUnstableProvider(context, auth);
    }
    
    @Override
    public boolean releaseProvider(IContentProvider icp) {
        return mBase.releaseProvider(icp);
    }
    
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean releaseUnstableProvider(IContentProvider icp) {
        return mBase.releaseUnstableProvider(icp);
    }
    
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void unstableProviderDied(IContentProvider icp) {
        mBase.unstableProviderDied(icp);
    }
    
    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    @Override
    public void appNotRespondingViaProvider(IContentProvider icp) {
        // dark greylist in Android P
//        mBase.appNotRespondingViaProvider(icp);
    }

}
