package com.didi.virtualapk.utils;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.core.VariantType;
import com.android.builder.model.BuildType;
import com.android.utils.StringHelper;

/**
 * Created by muyonggang on 2023/7/12
 */
public class VariantConfiguration {


//    // copy from agp 3.0.0
//    @NonNull
//    public static <B extends BuildType> String computeFullName(
//            @NonNull String flavorName,
//            @NonNull B buildType,
//            @NonNull VariantType type,
//            @Nullable VariantType testedType) {
//        StringBuilder sb = new StringBuilder();
//
//        if (!flavorName.isEmpty()) {
//            sb.append(flavorName);
//            sb.append(StringHelper.capitalize(buildType.getName()));
//        } else {
//            sb.append(buildType.getName());
//        }
//
//        if (type == VariantType.FEATURE) {
//            sb.append("Feature");
//        }
//
//        if (type.isForTesting()) {
//            if (testedType == VariantType.FEATURE) {
//                sb.append("Feature");
//            }
//            sb.append(type.getSuffix());
//        }
//        return sb.toString();
//    }
}
