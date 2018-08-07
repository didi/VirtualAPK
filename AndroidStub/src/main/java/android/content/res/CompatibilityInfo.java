package android.content.res;

import android.content.pm.ApplicationInfo;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by qiaopu on 2018/5/3.
 */
public class CompatibilityInfo implements Parcelable {
    
    public CompatibilityInfo(ApplicationInfo appInfo, int screenLayout, int sw,
                             boolean forceCompat) {
        throw new RuntimeException("Stub!");
    }
    
    @Override
    public int describeContents() {
        throw new RuntimeException("Stub!");
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        throw new RuntimeException("Stub!");
    }
    
    public static final Parcelable.Creator<CompatibilityInfo> CREATOR
        = new Parcelable.Creator<CompatibilityInfo>() {
        @Override
        public CompatibilityInfo createFromParcel(Parcel source) {
            throw new RuntimeException("Stub!");
        }
        
        @Override
        public CompatibilityInfo[] newArray(int size) {
            throw new RuntimeException("Stub!");
        }
    };
    
}

