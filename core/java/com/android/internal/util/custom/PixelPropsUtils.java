/*
 * Copyright (C) 2020 The Pixel Experience Project
 *               2021-2025 crDroid Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.util.custom;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.SystemProperties;
import android.util.Log;

import com.android.internal.R;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @hide
 */
public final class PixelPropsUtils {

    private static final String TAG = PixelPropsUtils.class.getSimpleName();
    private static final String DEVICE = "ro.product.device";
    private static final boolean DEBUG = false;

    private static final String SPOOF_PIXEL_PI = "persist.sys.pixelprops.pi";
    private static final String SPOOF_PIXEL_GPHOTOS = "persist.sys.pixelprops.gphotos";

    private static final Map<String, Object> propsToChangeGeneric;
    private static final Map<String, Object> propsToChangePixelXL;

    private static volatile boolean sIsFinsky = false;
    private static volatile String[] sCertifiedProps;

    static {
        propsToChangeGeneric = new HashMap<>();
        propsToChangeGeneric.put("TYPE", "user");
        propsToChangeGeneric.put("TAGS", "release-keys");
        propsToChangePixelXL = new HashMap<>();
        propsToChangePixelXL.put("BRAND", "google");
        propsToChangePixelXL.put("MANUFACTURER", "Google");
        propsToChangePixelXL.put("DEVICE", "marlin");
        propsToChangePixelXL.put("PRODUCT", "marlin");
        propsToChangePixelXL.put("HARDWARE", "marlin");
        propsToChangePixelXL.put("MODEL", "Pixel XL");
        propsToChangePixelXL.put("ID", "QP1A.191005.007.A3");
        propsToChangePixelXL.put("FINGERPRINT", "google/marlin/marlin:10/QP1A.191005.007.A3/5972272:user/release-keys");
    }

    public static void setProps(Context context) {
        final String packageName = context.getPackageName();
        if (packageName == null || packageName.isEmpty()) {
            return;
        }

        propsToChangeGeneric.forEach((k, v) -> setPropValue(k, v));

        Map<String, Object> propsToChange = new HashMap<>();

        if (packageName.equals("com.google.android.apps.photos")) {
            if (SystemProperties.getBoolean(SPOOF_PIXEL_GPHOTOS, true)) {
                propsToChange.putAll(propsToChangePixelXL);
            }
        } else if (packageName.equals("com.android.vending")) {
            sIsFinsky = true;
            return;
        } else if (packageName.equals("com.google.android.gms")) {
            final String processName = Application.getProcessName().toLowerCase();
            if (processName.contains("unstable")) {
                spoofBuildGms(context);
                return;
            }
            return;
        }

        if (DEBUG) Log.d(TAG, "Defining props for: " + packageName);
        for (Map.Entry<String, Object> prop : propsToChange.entrySet()) {
            String key = prop.getKey();
            Object value = prop.getValue();
            if (DEBUG) Log.d(TAG, "Defining " + key + " prop for: " + packageName);
            setPropValue(key, value);
        }
    }

    private static void setPropValue(String key, Object value) {
        setPropValue(key, value.toString());
    }

    private static void setPropValue(String key, String value) {
        try {
            if (DEBUG) Log.d(TAG, "Defining prop " + key + " to " + value);
            Class<?> clazz = Build.class;
            if (key.startsWith("VERSION.")) {
                clazz = Build.VERSION.class;
                key = key.substring(8);
            }
            Field field = clazz.getDeclaredField(key);
            field.setAccessible(true);
            // Determine the field type and parse the value accordingly.
            if (field.getType().equals(Integer.TYPE)) {
                field.set(null, Integer.parseInt(value));
            } else if (field.getType().equals(Long.TYPE)) {
                field.set(null, Long.parseLong(value));
            } else {
                field.set(null, value);
            }
            field.setAccessible(false);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set prop " + key, e);
        }
    }

    private static void spoofBuildGms(Context context) {
        if (!SystemProperties.getBoolean(SPOOF_PIXEL_PI, true))
            return;
        // Alter build parameters to avoid hardware attestation enforcement
        sCertifiedProps = context.getResources().getStringArray(R.array.config_certifiedBuildProperties);
        for (String entry : sCertifiedProps) {
            // Each entry must be of the format FIELD:value
            final String[] fieldAndProp = entry.split(":", 2);
            if (fieldAndProp.length != 2) {
                Log.e(TAG, "Invalid entry in certified props: " + entry);
                continue;
            }
            setPropValue(fieldAndProp[0], fieldAndProp[1]);
        }
    }

    private static boolean isCallerSafetyNet() {
        return Arrays.stream(Thread.currentThread().getStackTrace())
                        .anyMatch(elem -> elem.getClassName().toLowerCase()
                            .contains("droidguard"));
    }

    public static void onEngineGetCertificateChain() {
        if (!SystemProperties.getBoolean(SPOOF_PIXEL_PI, true))
            return;
        // Check stack for SafetyNet or Play Integrity
        if (isCallerSafetyNet() || sIsFinsky) {
            Log.i(TAG, "Blocked key attestation");
            throw new UnsupportedOperationException();
        }
    }
}
