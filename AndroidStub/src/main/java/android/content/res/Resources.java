package android.content.res;

import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

/**
 * Created by qiaopu on 2018/5/18.
 */
public class Resources {
    
    public Resources(AssetManager assets, DisplayMetrics metrics, Configuration config) {
        throw new RuntimeException("Stub!");
    }
    
    public final AssetManager getAssets() {
        throw new RuntimeException("Stub!");
    }
    
    public int getColor(int id) throws NotFoundException {
        throw new RuntimeException("Stub!");
    }
    
    public Configuration getConfiguration() {
        throw new RuntimeException("Stub!");
    }
    
    public DisplayMetrics getDisplayMetrics() {
        throw new RuntimeException("Stub!");
    }
    
    public Drawable getDrawable(int id) throws NotFoundException {
        throw new RuntimeException("Stub!");
    }
    
    public String getString(int id) throws NotFoundException {
        throw new RuntimeException("Stub!");
    }
    
    public CharSequence getText(int id) throws NotFoundException {
        throw new RuntimeException("Stub!");
    }
    
    public XmlResourceParser getXml(int id) throws NotFoundException {
        throw new RuntimeException("Stub!");
    }
    
    public ResourcesImpl getImpl() {
        throw new RuntimeException("Stub!");
    }
    
    public final Theme newTheme() {
        throw new RuntimeException("Stub!");
    }
    
    public void updateConfiguration(Configuration config, DisplayMetrics metrics) {
        throw new RuntimeException("Stub!");
    }
    
    public final class Theme {
    
        public void applyStyle(int resId, boolean force) {
            throw new RuntimeException("Stub!");
        }
    
        public TypedArray obtainStyledAttributes(int[] attrs) {
            throw new RuntimeException("Stub!");
        }
    
        public void setTo(Theme other) {
            throw new RuntimeException("Stub!");
        }
        
    }
    
    public static class NotFoundException extends RuntimeException {
    
    }
    
}
