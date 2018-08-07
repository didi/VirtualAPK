package android.databinding;

import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;

import com.didi.virtualapk.PluginManager;
import com.didi.virtualapk.internal.Constants;
import com.didi.virtualapk.internal.LoadedPlugin;

import java.util.LinkedList;

/**
 * Replace {@link DataBindingUtil#sMapper}.
 * Created by qiaopu on 2018/4/11.
 */
public class DataBinderMapperProxy extends DataBinderMapper implements PluginManager.Callback {
    public static final String TAG = Constants.TAG_PREFIX + "DataBinderMapperProxy";
    
    private final LinkedList<DataBinderMapper> mMappers;
    private DataBinderMapper[] mCache;
    
    public DataBinderMapperProxy(@NonNull Object source) {
        mMappers = new LinkedList<>();
    
        addMapper((DataBinderMapper) source);
    }
    
    @Override
    public ViewDataBinding getDataBinder(DataBindingComponent bindingComponent, View view, int layoutId) {
        ViewDataBinding viewDataBinding;
    
        for (DataBinderMapper mapper : getCache()) {
            viewDataBinding = mapper.getDataBinder(bindingComponent, view, layoutId);
            if (viewDataBinding != null) {
//                Log.d(TAG, "Found by mapper: " + mapper);
                return viewDataBinding;
            }
        }
        
        return null;
    }
    
    @Override
    ViewDataBinding getDataBinder(DataBindingComponent bindingComponent, View[] view, int layoutId) {
        ViewDataBinding viewDataBinding;
    
        for (DataBinderMapper mapper : getCache()) {
            viewDataBinding = mapper.getDataBinder(bindingComponent, view, layoutId);
            if (viewDataBinding != null) {
//                Log.d(TAG, "Found by mapper: " + mapper);
                return viewDataBinding;
            }
        }
    
        return null;
    }
    
    @Override
    public int getLayoutId(String tag) {
        int layoutId;
    
        for (DataBinderMapper mapper : getCache()) {
            layoutId = mapper.getLayoutId(tag);
            if (layoutId != 0) {
//                Log.d(TAG, "Found by mapper: " + mapper);
                return layoutId;
            }
        }
    
        return 0;
    }
    
    @Override
    public String convertBrIdToString(int id) {
        String brId;
    
        for (DataBinderMapper mapper : getCache()) {
            brId = mapper.convertBrIdToString(id);
            if (brId != null) {
//                Log.d(TAG, "Found by mapper: " + mapper);
                return brId;
            }
        }
    
        return null;
    }
    
    @Override
    public void onAddedLoadedPlugin(LoadedPlugin plugin) {
        try {
            String clsName = "android.databinding.DataBinderMapper_" + plugin.getPackageName().replace('.', '_');
            Log.d(TAG, "Try to find the class: " + clsName);
            Class cls = Class.forName(clsName, true, plugin.getClassLoader());
            Object obj = cls.newInstance();
    
            addMapper((DataBinderMapper) obj);
            
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }
    
    private void addMapper(DataBinderMapper mapper) {
        int size = 0;
        synchronized (mMappers) {
            mMappers.add(mapper);
            mCache = null;
            size = mMappers.size();
        }
    
        Log.d(TAG, "Added mapper: " + mapper + ", size: " + size);
    }
    
    private DataBinderMapper[] getCache() {
        synchronized (mMappers) {
            if (mCache == null) {
                mCache = mMappers.toArray(new DataBinderMapper[mMappers.size()]);
            }
            return mCache;
        }
    }
}
