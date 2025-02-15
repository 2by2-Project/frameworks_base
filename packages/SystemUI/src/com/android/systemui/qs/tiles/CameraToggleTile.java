/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.qs.tiles;

import static android.hardware.SensorPrivacyManager.Sensors.CAMERA;
import static android.os.UserManager.DISALLOW_CAMERA_TOGGLE;

import static com.android.systemui.DejankUtils.whitelistIpcs;

import android.hardware.SensorPrivacyManager.Sensors.Sensor;
import android.os.Handler;
import android.os.Looper;
import android.provider.DeviceConfig;
import android.safetycenter.SafetyCenterManager;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.QsEventLogger;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.res.R;
import com.android.systemui.statusbar.policy.IndividualSensorPrivacyController;
import com.android.systemui.statusbar.policy.KeyguardStateController;

import javax.inject.Inject;

public class CameraToggleTile extends SensorPrivacyToggleTile {

    public static final String TILE_SPEC = "cameratoggle";

    @Inject
    protected CameraToggleTile(
            QSHost host,
            QsEventLogger uiEventLogger,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            MetricsLogger metricsLogger,
            FalsingManager falsingManager,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger,
            IndividualSensorPrivacyController sensorPrivacyController,
            KeyguardStateController keyguardStateController,
            SafetyCenterManager safetyCenterManager) {
        super(host, uiEventLogger, backgroundLooper, mainHandler, falsingManager, metricsLogger,
                statusBarStateController, activityStarter, qsLogger, sensorPrivacyController,
                keyguardStateController, safetyCenterManager);
    }

    @Override
    public boolean isAvailable() {
        return mSensorPrivacyController.supportsSensorToggle(CAMERA)
                && whitelistIpcs(() -> DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_PRIVACY,
                "camera_toggle_enabled",
                true));
    }

    @Override
    public @DrawableRes int getIconRes(boolean isBlocked) {
        if (isBlocked) {
            return R.drawable.qs_camera_access_icon_off;
        } else {
            return R.drawable.qs_camera_access_icon_on;
        }
    }

    @Override
    public @NonNull CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_camera_label);
    }

    @Override
    public @Sensor int getSensorId() {
        return CAMERA;
    }

    @Override
    public String getRestriction() {
        return DISALLOW_CAMERA_TOGGLE;
    }
}
