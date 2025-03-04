/*
 * Copyright (C) 2016 The Android Open Source Project
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
package android.app;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.annotation.TestApi;
import android.app.NotificationManager.Importance;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.text.TextUtils;
import android.util.proto.ProtoOutputStream;

import com.android.internal.util.Preconditions;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Objects;

/**
 * A representation of settings that apply to a collection of similarly themed notifications.
 */
public final class NotificationChannel implements Parcelable {

    /**
     * The id of the default channel for an app. This id is reserved by the system. All
     * notifications posted from apps targeting {@link android.os.Build.VERSION_CODES#N_MR1} or
     * earlier without a notification channel specified are posted to this channel.
     */
    public static final String DEFAULT_CHANNEL_ID = "miscellaneous";

    /**
     * The formatter used by the system to create an id for notification
     * channels when it automatically creates conversation channels on behalf of an app. The format
     * string takes two arguments, in this order: the
     * {@link #getId()} of the original notification channel, and the
     * {@link ShortcutInfo#getId() id} of the conversation.
     * @hide
     */
    public static final String CONVERSATION_CHANNEL_ID_FORMAT = "%1$s : %2$s";

    /**
     * TODO: STOPSHIP  remove
     * Conversation id to use for apps that aren't providing them yet.
     * @hide
     */
    public static final String PLACEHOLDER_CONVERSATION_ID = ":placeholder_id";

    /**
     * The maximum length for text fields in a NotificationChannel. Fields will be truncated at this
     * limit.
     * @hide
     */
    public static final int MAX_TEXT_LENGTH = 1000;
    /**
     * @hide
     */
    public static final int MAX_VIBRATION_LENGTH = 1000;

    private static final String TAG_CHANNEL = "channel";
    private static final String ATT_NAME = "name";
    private static final String ATT_DESC = "desc";
    private static final String ATT_ID = "id";
    private static final String ATT_DELETED = "deleted";
    private static final String ATT_PRIORITY = "priority";
    private static final String ATT_VISIBILITY = "visibility";
    private static final String ATT_IMPORTANCE = "importance";
    private static final String ATT_LIGHTS = "lights";
    private static final String ATT_LIGHT_COLOR = "light_color";
    private static final String ATT_ON_TIME = "light_on_time";
    private static final String ATT_OFF_TIME = "light_off_time";
    private static final String ATT_VIBRATION = "vibration";
    private static final String ATT_VIBRATION_ENABLED = "vibration_enabled";
    private static final String ATT_SOUND = "sound";
    private static final String ATT_USAGE = "usage";
    private static final String ATT_FLAGS = "flags";
    private static final String ATT_CONTENT_TYPE = "content_type";
    private static final String ATT_SHOW_BADGE = "show_badge";
    private static final String ATT_USER_LOCKED = "locked";
    private static final String ATT_FG_SERVICE_SHOWN = "fgservice";
    private static final String ATT_GROUP = "group";
    private static final String ATT_BLOCKABLE_SYSTEM = "blockable_system";
    private static final String ATT_ALLOW_BUBBLE = "allow_bubbles";
    private static final String ATT_ORIG_IMP = "orig_imp";
    private static final String ATT_PARENT_CHANNEL = "parent";
    private static final String ATT_CONVERSATION_ID = "conv_id";
    private static final String ATT_IMP_CONVERSATION = "imp_conv";
    private static final String ATT_DEMOTE = "dem";
    private static final String DELIMITER = ",";

    /**
     * @hide
     */
    public static final int USER_LOCKED_PRIORITY = 0x00000001;
    /**
     * @hide
     */
    public static final int USER_LOCKED_VISIBILITY = 0x00000002;
    /**
     * @hide
     */
    public static final int USER_LOCKED_IMPORTANCE = 0x00000004;
    /**
     * @hide
     */
    public static final int USER_LOCKED_LIGHTS = 0x00000008;
    /**
     * @hide
     */
    public static final int USER_LOCKED_VIBRATION = 0x00000010;
    /**
     * @hide
     */
    @SystemApi
    @TestApi
    public static final int USER_LOCKED_SOUND = 0x00000020;

    /**
     * @hide
     */
    public static final int USER_LOCKED_SHOW_BADGE = 0x00000080;

    /**
     * @hide
     */
    public static final int USER_LOCKED_ALLOW_BUBBLE = 0x00000100;

    /**
     * @hide
     */
    public static final int[] LOCKABLE_FIELDS = new int[] {
            USER_LOCKED_PRIORITY,
            USER_LOCKED_VISIBILITY,
            USER_LOCKED_IMPORTANCE,
            USER_LOCKED_LIGHTS,
            USER_LOCKED_VIBRATION,
            USER_LOCKED_SOUND,
            USER_LOCKED_SHOW_BADGE,
            USER_LOCKED_ALLOW_BUBBLE
    };

    /**
     * @hide
     */
    public static final int DEFAULT_ALLOW_BUBBLE = -1;
    /**
     * @hide
     */
    public static final int ALLOW_BUBBLE_ON = 1;
    /**
     * @hide
     */
    public static final int ALLOW_BUBBLE_OFF = 0;

    private static final int DEFAULT_LIGHT_COLOR = 0;
    private static final int DEFAULT_ON_TIME = 0;
    private static final int DEFAULT_OFF_TIME = 0;
    private static final int DEFAULT_VISIBILITY =
            NotificationManager.VISIBILITY_NO_OVERRIDE;
    private static final int DEFAULT_IMPORTANCE =
            NotificationManager.IMPORTANCE_UNSPECIFIED;
    private static final boolean DEFAULT_DELETED = false;
    private static final boolean DEFAULT_SHOW_BADGE = true;

    @UnsupportedAppUsage
    private String mId;
    private String mName;
    private String mDesc;
    private int mImportance = DEFAULT_IMPORTANCE;
    private int mOriginalImportance = DEFAULT_IMPORTANCE;
    private boolean mBypassDnd;
    private int mLockscreenVisibility = DEFAULT_VISIBILITY;
    private Uri mSound = Settings.System.DEFAULT_NOTIFICATION_URI;
    private boolean mLights;
    private int mLightColor = DEFAULT_LIGHT_COLOR;
    private int mLightOnTime = DEFAULT_ON_TIME;
    private int mLightOffTime = DEFAULT_OFF_TIME;
    private long[] mVibration;
    // Bitwise representation of fields that have been changed by the user, preventing the app from
    // making changes to these fields.
    private int mUserLockedFields;
    private boolean mFgServiceShown;
    private boolean mVibrationEnabled;
    private boolean mShowBadge = DEFAULT_SHOW_BADGE;
    private boolean mDeleted = DEFAULT_DELETED;
    private String mGroup;
    private AudioAttributes mAudioAttributes = Notification.AUDIO_ATTRIBUTES_DEFAULT;
    // If this is a blockable system notification channel.
    private boolean mBlockableSystem = false;
    private int mAllowBubbles = DEFAULT_ALLOW_BUBBLE;
    private boolean mImportanceLockedByOEM;
    private boolean mImportanceLockedDefaultApp;
    private String mParentId = null;
    private String mConversationId = null;
    private boolean mDemoted = false;
    private boolean mImportantConvo = false;

    /**
     * Creates a notification channel.
     *
     * @param id The id of the channel. Must be unique per package. The value may be truncated if
     *           it is too long.
     * @param name The user visible name of the channel. You can rename this channel when the system
     *             locale changes by listening for the {@link Intent#ACTION_LOCALE_CHANGED}
     *             broadcast. The recommended maximum length is 40 characters; the value may be
     *             truncated if it is too long.
     * @param importance The importance of the channel. This controls how interruptive notifications
     *                   posted to this channel are.
     */
    public NotificationChannel(String id, CharSequence name, @Importance int importance) {
        this.mId = getTrimmedString(id);
        this.mName = name != null ? getTrimmedString(name.toString()) : null;
        this.mImportance = importance;
    }

    /**
     * @hide
     */
    protected NotificationChannel(Parcel in) {
        if (in.readByte() != 0) {
            mId = getTrimmedString(in.readString());
        } else {
            mId = null;
        }
        if (in.readByte() != 0) {
            mName = getTrimmedString(in.readString());
        } else {
            mName = null;
        }
        if (in.readByte() != 0) {
            mDesc = getTrimmedString(in.readString());
        } else {
            mDesc = null;
        }
        mImportance = in.readInt();
        mBypassDnd = in.readByte() != 0;
        mLockscreenVisibility = in.readInt();
        if (in.readByte() != 0) {
            mSound = Uri.CREATOR.createFromParcel(in);
            mSound = Uri.parse(getTrimmedString(mSound.toString()));
        } else {
            mSound = null;
        }
        mLights = in.readByte() != 0;
        mVibration = in.createLongArray();
        if (mVibration != null && mVibration.length > MAX_VIBRATION_LENGTH) {
            mVibration = Arrays.copyOf(mVibration, MAX_VIBRATION_LENGTH);
        }
        mUserLockedFields = in.readInt();
        mFgServiceShown = in.readByte() != 0;
        mVibrationEnabled = in.readByte() != 0;
        mShowBadge = in.readByte() != 0;
        mDeleted = in.readByte() != 0;
        if (in.readByte() != 0) {
            mGroup = getTrimmedString(in.readString());
        } else {
            mGroup = null;
        }
        mAudioAttributes = in.readInt() > 0 ? AudioAttributes.CREATOR.createFromParcel(in) : null;
        mLightColor = in.readInt();
        mLightOnTime = in.readInt();
        mLightOffTime = in.readInt();
        mBlockableSystem = in.readBoolean();
        mAllowBubbles = in.readInt();
        mImportanceLockedByOEM = in.readBoolean();
        mOriginalImportance = in.readInt();
        mParentId = getTrimmedString(in.readString());
        mConversationId = getTrimmedString(in.readString());
        mDemoted = in.readBoolean();
        mImportantConvo = in.readBoolean();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (mId != null) {
            dest.writeByte((byte) 1);
            dest.writeString(mId);
        } else {
            dest.writeByte((byte) 0);
        }
        if (mName != null) {
            dest.writeByte((byte) 1);
            dest.writeString(mName);
        } else {
            dest.writeByte((byte) 0);
        }
        if (mDesc != null) {
            dest.writeByte((byte) 1);
            dest.writeString(mDesc);
        } else {
            dest.writeByte((byte) 0);
        }
        dest.writeInt(mImportance);
        dest.writeByte(mBypassDnd ? (byte) 1 : (byte) 0);
        dest.writeInt(mLockscreenVisibility);
        if (mSound != null) {
            dest.writeByte((byte) 1);
            mSound.writeToParcel(dest, 0);
        } else {
            dest.writeByte((byte) 0);
        }
        dest.writeByte(mLights ? (byte) 1 : (byte) 0);
        dest.writeLongArray(mVibration);
        dest.writeInt(mUserLockedFields);
        dest.writeByte(mFgServiceShown ? (byte) 1 : (byte) 0);
        dest.writeByte(mVibrationEnabled ? (byte) 1 : (byte) 0);
        dest.writeByte(mShowBadge ? (byte) 1 : (byte) 0);
        dest.writeByte(mDeleted ? (byte) 1 : (byte) 0);
        if (mGroup != null) {
            dest.writeByte((byte) 1);
            dest.writeString(mGroup);
        } else {
            dest.writeByte((byte) 0);
        }
        if (mAudioAttributes != null) {
            dest.writeInt(1);
            mAudioAttributes.writeToParcel(dest, 0);
        } else {
            dest.writeInt(0);
        }
        dest.writeInt(mLightColor);
        dest.writeInt(mLightOnTime);
        dest.writeInt(mLightOffTime);
        dest.writeBoolean(mBlockableSystem);
        dest.writeInt(mAllowBubbles);
        dest.writeBoolean(mImportanceLockedByOEM);
        dest.writeInt(mOriginalImportance);
        dest.writeString(mParentId);
        dest.writeString(mConversationId);
        dest.writeBoolean(mDemoted);
        dest.writeBoolean(mImportantConvo);
    }

    /**
     * @hide
     */
    @TestApi
    public void lockFields(int field) {
        mUserLockedFields |= field;
    }

    /**
     * @hide
     */
    public void unlockFields(int field) {
        mUserLockedFields &= ~field;
    }

    /**
     * @hide
     */
    @TestApi
    public void setFgServiceShown(boolean shown) {
        mFgServiceShown = shown;
    }

    /**
     * @hide
     */
    @TestApi
    public void setDeleted(boolean deleted) {
        mDeleted = deleted;
    }

    /**
     * @hide
     */
    @TestApi
    public void setImportantConversation(boolean importantConvo) {
        mImportantConvo = importantConvo;
    }

    /**
     * Allows users to block notifications sent through this channel, if this channel belongs to
     * a package that is signed with the system signature.
     *
     * If the channel does not belong to a package that is signed with the system signature, this
     * method does nothing, since such channels are blockable by default and cannot be set to be
     * unblockable.
     * @param blockable if {@code true}, allows users to block notifications on this channel.
     * @hide
     */
    @SystemApi
    @TestApi
    public void setBlockable(boolean blockable) {
        mBlockableSystem = blockable;
    }
    // Modifiable by apps post channel creation

    /**
     * Sets the user visible name of this channel.
     *
     * <p>The recommended maximum length is 40 characters; the value may be truncated if it is too
     * long.
     */
    public void setName(CharSequence name) {
        mName = name != null ? getTrimmedString(name.toString()) : null;
    }

    /**
     * Sets the user visible description of this channel.
     *
     * <p>The recommended maximum length is 300 characters; the value may be truncated if it is too
     * long.
     */
    public void setDescription(String description) {
        mDesc = getTrimmedString(description);
    }

    private String getTrimmedString(String input) {
        if (input != null && input.length() > MAX_TEXT_LENGTH) {
            return input.substring(0, MAX_TEXT_LENGTH);
        }
        return input;
    }

    /**
     * @hide
     */
    public void setId(String id) {
        mId = id;
    }

    // Modifiable by apps on channel creation.

    /**
     * Sets what group this channel belongs to.
     *
     * Group information is only used for presentation, not for behavior.
     *
     * Only modifiable before the channel is submitted to
     * {@link NotificationManager#createNotificationChannel(NotificationChannel)}, unless the
     * channel is not currently part of a group.
     *
     * @param groupId the id of a group created by
     * {@link NotificationManager#createNotificationChannelGroup(NotificationChannelGroup)}.
     */
    public void setGroup(String groupId) {
        this.mGroup = groupId;
    }

    /**
     * Sets whether notifications posted to this channel can appear as application icon badges
     * in a Launcher.
     *
     * Only modifiable before the channel is submitted to
     * {@link NotificationManager#createNotificationChannel(NotificationChannel)}.
     *
     * @param showBadge true if badges should be allowed to be shown.
     */
    public void setShowBadge(boolean showBadge) {
        this.mShowBadge = showBadge;
    }

    /**
     * Sets the sound that should be played for notifications posted to this channel and its
     * audio attributes. Notification channels with an {@link #getImportance() importance} of at
     * least {@link NotificationManager#IMPORTANCE_DEFAULT} should have a sound.
     *
     * Only modifiable before the channel is submitted to
     * {@link NotificationManager#createNotificationChannel(NotificationChannel)}.
     */
    public void setSound(Uri sound, AudioAttributes audioAttributes) {
        this.mSound = sound;
        this.mAudioAttributes = audioAttributes;
    }

    /**
     * Sets whether notifications posted to this channel should display notification lights,
     * on devices that support that feature.
     *
     * Only modifiable before the channel is submitted to
     * {@link NotificationManager#createNotificationChannel(NotificationChannel)}.
     */
    public void enableLights(boolean lights) {
        this.mLights = lights;
    }

    /**
     * Sets the notification light color for notifications posted to this channel, if lights are
     * {@link #enableLights(boolean) enabled} on this channel and the device supports that feature.
     *
     * Only modifiable before the channel is submitted to
     * {@link NotificationManager#createNotificationChannel(NotificationChannel)}.
     */
    public void setLightColor(int argb) {
        this.mLightColor = argb;
    }

    /**
     * Sets the notification light ON time for notifications posted to this channel, if lights are
     * {@link #enableLights(boolean) enabled} on this channel and the device supports that feature.
     *
     * Only modifiable before the channel is submitted to
     * {@link NotificationManager#notify(String, int, Notification)}.
     * @hide
     */
    public void setLightOnTime(int time) {
        this.mLightOnTime = time;
    }

    /**
     * Sets the notification light OFF time for notifications posted to this channel, if lights are
     * {@link #enableLights(boolean) enabled} on this channel and the device supports that feature.
     *
     * Only modifiable before the channel is submitted to
     * {@link NotificationManager#notify(String, int, Notification)}.
     * @hide
     */
    public void setLightOffTime(int time) {
        this.mLightOffTime = time;
    }

    /**
     * Sets whether notification posted to this channel should vibrate. The vibration pattern can
     * be set with {@link #setVibrationPattern(long[])}.
     *
     * Only modifiable before the channel is submitted to
     * {@link NotificationManager#createNotificationChannel(NotificationChannel)}.
     */
    public void enableVibration(boolean vibration) {
        this.mVibrationEnabled = vibration;
    }

    /**
     * Sets the vibration pattern for notifications posted to this channel. If the provided
     * pattern is valid (non-null, non-empty), will {@link #enableVibration(boolean)} enable
     * vibration} as well. Otherwise, vibration will be disabled.
     *
     * Only modifiable before the channel is submitted to
     * {@link NotificationManager#createNotificationChannel(NotificationChannel)}.
     */
    public void setVibrationPattern(long[] vibrationPattern) {
        this.mVibrationEnabled = vibrationPattern != null && vibrationPattern.length > 0;
        this.mVibration = vibrationPattern;
    }

    /**
     * Sets the level of interruption of this notification channel.
     *
     * Only modifiable before the channel is submitted to
     * {@link NotificationManager#createNotificationChannel(NotificationChannel)}.
     *
     * @param importance the amount the user should be interrupted by
     *            notifications from this channel.
     */
    public void setImportance(@Importance int importance) {
        this.mImportance = importance;
    }

    // Modifiable by a notification ranker.

    /**
     * Sets whether or not notifications posted to this channel can interrupt the user in
     * {@link android.app.NotificationManager.Policy#INTERRUPTION_FILTER_PRIORITY} mode.
     *
     * Only modifiable by the system and notification ranker.
     */
    public void setBypassDnd(boolean bypassDnd) {
        this.mBypassDnd = bypassDnd;
    }

    /**
     * Sets whether notifications posted to this channel appear on the lockscreen or not, and if so,
     * whether they appear in a redacted form. See e.g. {@link Notification#VISIBILITY_SECRET}.
     *
     * Only modifiable by the system and notification ranker.
     */
    public void setLockscreenVisibility(int lockscreenVisibility) {
        this.mLockscreenVisibility = lockscreenVisibility;
    }

    /**
     * As of Android 11 this value is no longer respected.
     * @see #canBubble()
     * @see Notification#getBubbleMetadata()
     */
    public void setAllowBubbles(boolean allowBubbles) {
        mAllowBubbles = allowBubbles ? ALLOW_BUBBLE_ON : ALLOW_BUBBLE_OFF;
    }

    /**
     * @hide
     */
    public void setAllowBubbles(int allowed) {
        mAllowBubbles = allowed;
    }

    /**
     * Sets this channel as being converastion-centric. Different settings and functionality may be
     * exposed for conversation-centric channels.
     *
     * @param parentChannelId The {@link #getId()} id} of the generic channel that notifications of
     *                        this type would be posted to in absence of a specific conversation id.
     *                        For example, if this channel represents 'Messages from Person A', the
     *                        parent channel would be 'Messages.'
     * @param conversationId The {@link ShortcutInfo#getId()} of the shortcut representing this
     *                       channel's conversation.
     */
    public void setConversationId(@NonNull String parentChannelId,
            @NonNull String conversationId) {
        mParentId = parentChannelId;
        mConversationId = conversationId;
    }

    /**
     * Returns the id of this channel.
     */
    public String getId() {
        return mId;
    }

    /**
     * Returns the user visible name of this channel.
     */
    public CharSequence getName() {
        return mName;
    }

    /**
     * Returns the user visible description of this channel.
     */
    public String getDescription() {
        return mDesc;
    }

    /**
     * Returns the user specified importance e.g. {@link NotificationManager#IMPORTANCE_LOW} for
     * notifications posted to this channel. Note: This value might be >
     * {@link NotificationManager#IMPORTANCE_NONE}, but notifications posted to this channel will
     * not be shown to the user if the parent {@link NotificationChannelGroup} or app is blocked.
     * See {@link NotificationChannelGroup#isBlocked()} and
     * {@link NotificationManager#areNotificationsEnabled()}.
     */
    public int getImportance() {
        return mImportance;
    }

    /**
     * Whether or not notifications posted to this channel can bypass the Do Not Disturb
     * {@link NotificationManager#INTERRUPTION_FILTER_PRIORITY} mode.
     */
    public boolean canBypassDnd() {
        return mBypassDnd;
    }

    /**
     * Whether or not notifications in this conversation are considered important.
     *
     * <p>Important conversations may get special visual treatment, and might be able to bypass DND.
     *
     * <p>This is only valid for channels that represent conversations, that is, those with a valid
     * {@link #getConversationId() conversation id}.
     */
    public boolean isImportantConversation() {
        return mImportantConvo;
    }

    /**
     * Returns the notification sound for this channel.
     */
    public Uri getSound() {
        return mSound;
    }

    /**
     * Returns the audio attributes for sound played by notifications posted to this channel.
     */
    public AudioAttributes getAudioAttributes() {
        return mAudioAttributes;
    }

    /**
     * Returns whether notifications posted to this channel trigger notification lights.
     */
    public boolean shouldShowLights() {
        return mLights;
    }

    /**
     * Returns the notification light color for notifications posted to this channel. Irrelevant
     * unless {@link #shouldShowLights()}.
     */
    public int getLightColor() {
        return mLightColor;
    }

    /**
     * Returns the notification light ON time for notifications posted to this channel. Irrelevant
     * unless {@link #shouldShowLights()}.
     * @hide
     */
    public int getLightOnTime() {
        return mLightOnTime;
    }

    /**
     * Returns the notification light OFF time for notifications posted to this channel. Irrelevant
     * unless {@link #shouldShowLights()}.
     * @hide
     */
    public int getLightOffTime() {
        return mLightOffTime;
    }

    /**
     * Returns whether notifications posted to this channel always vibrate.
     */
    public boolean shouldVibrate() {
        return mVibrationEnabled;
    }

    /**
     * Returns the vibration pattern for notifications posted to this channel. Will be ignored if
     * vibration is not enabled ({@link #shouldVibrate()}.
     */
    public long[] getVibrationPattern() {
        return mVibration;
    }

    /**
     * Returns whether or not notifications posted to this channel are shown on the lockscreen in
     * full or redacted form.
     */
    public int getLockscreenVisibility() {
        return mLockscreenVisibility;
    }

    /**
     * Returns whether notifications posted to this channel can appear as badges in a Launcher
     * application.
     *
     * Note that badging may be disabled for other reasons.
     */
    public boolean canShowBadge() {
        return mShowBadge;
    }

    /**
     * Returns what group this channel belongs to.
     *
     * This is used only for visually grouping channels in the UI.
     */
    public String getGroup() {
        return mGroup;
    }

    /**
     * Returns whether notifications posted to this channel are allowed to display outside of the
     * notification shade, in a floating window on top of other apps.
     *
     * @see Notification#getBubbleMetadata()
     */
    public boolean canBubble() {
        return mAllowBubbles == ALLOW_BUBBLE_ON;
    }

    /**
     * @hide
     */
    public int getAllowBubbles() {
        return mAllowBubbles;
    }

    /**
     * Returns the {@link #getId() id} of the parent notification channel to this channel, if it's
     * a conversation related channel. See {@link #setConversationId(String, String)}.
     */
    public @Nullable String getParentChannelId() {
        return mParentId;
    }

    /**
     * Returns the {@link ShortcutInfo#getId() id} of the conversation backing this channel, if it's
     * associated with a conversation. See {@link #setConversationId(String, String)}.
     */
    public @Nullable String getConversationId() {
        return mConversationId;
    }

    /**
     * @hide
     */
    @SystemApi
    public boolean isDeleted() {
        return mDeleted;
    }

    /**
     * @hide
     */
    @SystemApi
    public int getUserLockedFields() {
        return mUserLockedFields;
    }

    /**
     * @hide
     */
    public boolean isFgServiceShown() {
        return mFgServiceShown;
    }

    /**
     * @hide
     */
    @TestApi
    public boolean isBlockable() {
        return mBlockableSystem;
    }

    /**
     * @hide
     */
    @TestApi
    public void setImportanceLockedByOEM(boolean locked) {
        mImportanceLockedByOEM = locked;
    }

    /**
     * @hide
     */
    @TestApi
    public void setImportanceLockedByCriticalDeviceFunction(boolean locked) {
        mImportanceLockedDefaultApp = locked;
    }

    /**
     * @hide
     */
    @TestApi
    public boolean isImportanceLockedByOEM() {
        return mImportanceLockedByOEM;
    }

    /**
     * @hide
     */
    @TestApi
    public boolean isImportanceLockedByCriticalDeviceFunction() {
        return mImportanceLockedDefaultApp;
    }

    /**
     * @hide
     */
    @TestApi
    public int getOriginalImportance() {
        return mOriginalImportance;
    }

    /**
     * @hide
     */
    @TestApi
    public void setOriginalImportance(int importance) {
        mOriginalImportance = importance;
    }

    /**
     * @hide
     */
    public void setDemoted(boolean demoted) {
        mDemoted = demoted;
    }

    /**
     * @hide
     */
    public boolean isDemoted() {
        return mDemoted;
    }

    /**
     * Returns whether the user has chosen the importance of this channel, either to affirm the
     * initial selection from the app, or changed it to be higher or lower.
     * @see #getImportance()
     */
    public boolean hasUserSetImportance() {
        return (mUserLockedFields & USER_LOCKED_IMPORTANCE) != 0;
    }

    /**
     * Returns whether the user has chosen the sound of this channel.
     * @see #getSound()
     */
    public boolean hasUserSetSound() {
        return (mUserLockedFields & USER_LOCKED_SOUND) != 0;
    }

    /**
     * @hide
     */
    public void populateFromXmlForRestore(XmlPullParser parser, Context context) {
        populateFromXml(parser, true, context);
    }

    /**
     * @hide
     */
    @SystemApi
    public void populateFromXml(XmlPullParser parser) {
        populateFromXml(parser, false, null);
    }

    /**
     * If {@param forRestore} is true, {@param Context} MUST be non-null.
     */
    private void populateFromXml(XmlPullParser parser, boolean forRestore,
            @Nullable Context context) {
        Preconditions.checkArgument(!forRestore || context != null,
                "forRestore is true but got null context");

        // Name, id, and importance are set in the constructor.
        setDescription(parser.getAttributeValue(null, ATT_DESC));
        setBypassDnd(Notification.PRIORITY_DEFAULT
                != safeInt(parser, ATT_PRIORITY, Notification.PRIORITY_DEFAULT));
        setLockscreenVisibility(safeInt(parser, ATT_VISIBILITY, DEFAULT_VISIBILITY));

        Uri sound = safeUri(parser, ATT_SOUND);
        setSound(forRestore ? restoreSoundUri(context, sound) : sound, safeAudioAttributes(parser));

        enableLights(safeBool(parser, ATT_LIGHTS, false));
        setLightColor(safeInt(parser, ATT_LIGHT_COLOR, DEFAULT_LIGHT_COLOR));
        setLightOnTime(safeInt(parser, ATT_ON_TIME, DEFAULT_ON_TIME));
        setLightOffTime(safeInt(parser, ATT_OFF_TIME, DEFAULT_OFF_TIME));
        setVibrationPattern(safeLongArray(parser, ATT_VIBRATION, null));
        enableVibration(safeBool(parser, ATT_VIBRATION_ENABLED, false));
        setShowBadge(safeBool(parser, ATT_SHOW_BADGE, false));
        setDeleted(safeBool(parser, ATT_DELETED, false));
        setGroup(parser.getAttributeValue(null, ATT_GROUP));
        lockFields(safeInt(parser, ATT_USER_LOCKED, 0));
        setFgServiceShown(safeBool(parser, ATT_FG_SERVICE_SHOWN, false));
        setBlockable(safeBool(parser, ATT_BLOCKABLE_SYSTEM, false));
        setAllowBubbles(safeInt(parser, ATT_ALLOW_BUBBLE, DEFAULT_ALLOW_BUBBLE));
        setOriginalImportance(safeInt(parser, ATT_ORIG_IMP, DEFAULT_IMPORTANCE));
        setConversationId(parser.getAttributeValue(null, ATT_PARENT_CHANNEL),
                parser.getAttributeValue(null, ATT_CONVERSATION_ID));
        setDemoted(safeBool(parser, ATT_DEMOTE, false));
        setImportantConversation(safeBool(parser, ATT_IMP_CONVERSATION, false));
    }

    @Nullable
    private Uri restoreSoundUri(Context context, @Nullable Uri uri) {
        if (uri == null || Uri.EMPTY.equals(uri)) {
            return null;
        }
        ContentResolver contentResolver = context.getContentResolver();
        // There are backups out there with uncanonical uris (because we fixed this after
        // shipping). If uncanonical uris are given to MediaProvider.uncanonicalize it won't
        // verify the uri against device storage and we'll possibly end up with a broken uri.
        // We then canonicalize the uri to uncanonicalize it back, which means we properly check
        // the uri and in the case of not having the resource we end up with the default - better
        // than broken. As a side effect we'll canonicalize already canonicalized uris, this is fine
        // according to the docs because canonicalize method has to handle canonical uris as well.
        Uri canonicalizedUri = contentResolver.canonicalize(uri);
        if (canonicalizedUri == null) {
            // We got a null because the uri in the backup does not exist here, so we return default
            return Settings.System.DEFAULT_NOTIFICATION_URI;
        }
        return contentResolver.uncanonicalize(canonicalizedUri);
    }

    /**
     * @hide
     */
    @SystemApi
    public void writeXml(XmlSerializer out) throws IOException {
        writeXml(out, false, null);
    }

    /**
     * @hide
     */
    public void writeXmlForBackup(XmlSerializer out, Context context) throws IOException {
        writeXml(out, true, context);
    }

    private Uri getSoundForBackup(Context context) {
        Uri sound = getSound();
        if (sound == null || Uri.EMPTY.equals(sound)) {
            return null;
        }
        Uri canonicalSound = context.getContentResolver().canonicalize(sound);
        if (canonicalSound == null) {
            // The content provider does not support canonical uris so we backup the default
            return Settings.System.DEFAULT_NOTIFICATION_URI;
        }
        return canonicalSound;
    }

    /**
     * If {@param forBackup} is true, {@param Context} MUST be non-null.
     */
    private void writeXml(XmlSerializer out, boolean forBackup, @Nullable Context context)
            throws IOException {
        Preconditions.checkArgument(!forBackup || context != null,
                "forBackup is true but got null context");
        out.startTag(null, TAG_CHANNEL);
        out.attribute(null, ATT_ID, getId());
        if (getName() != null) {
            out.attribute(null, ATT_NAME, getName().toString());
        }
        if (getDescription() != null) {
            out.attribute(null, ATT_DESC, getDescription());
        }
        if (getImportance() != DEFAULT_IMPORTANCE) {
            out.attribute(
                    null, ATT_IMPORTANCE, Integer.toString(getImportance()));
        }
        if (canBypassDnd()) {
            out.attribute(
                    null, ATT_PRIORITY, Integer.toString(Notification.PRIORITY_MAX));
        }
        if (getLockscreenVisibility() != DEFAULT_VISIBILITY) {
            out.attribute(null, ATT_VISIBILITY,
                    Integer.toString(getLockscreenVisibility()));
        }
        Uri sound = forBackup ? getSoundForBackup(context) : getSound();
        if (sound != null) {
            out.attribute(null, ATT_SOUND, sound.toString());
        }
        if (getAudioAttributes() != null) {
            out.attribute(null, ATT_USAGE, Integer.toString(getAudioAttributes().getUsage()));
            out.attribute(null, ATT_CONTENT_TYPE,
                    Integer.toString(getAudioAttributes().getContentType()));
            out.attribute(null, ATT_FLAGS, Integer.toString(getAudioAttributes().getFlags()));
        }
        if (shouldShowLights()) {
            out.attribute(null, ATT_LIGHTS, Boolean.toString(shouldShowLights()));
        }
        if (getLightColor() != DEFAULT_LIGHT_COLOR) {
            out.attribute(null, ATT_LIGHT_COLOR, Integer.toString(getLightColor()));
        }
        if (getLightOnTime() != DEFAULT_ON_TIME) {
            out.attribute(null, ATT_ON_TIME, Integer.toString(getLightOnTime()));
        }
        if (getLightOffTime() != DEFAULT_OFF_TIME) {
            out.attribute(null, ATT_OFF_TIME, Integer.toString(getLightOffTime()));
        }
        if (shouldVibrate()) {
            out.attribute(null, ATT_VIBRATION_ENABLED, Boolean.toString(shouldVibrate()));
        }
        if (getVibrationPattern() != null) {
            out.attribute(null, ATT_VIBRATION, longArrayToString(getVibrationPattern()));
        }
        if (getUserLockedFields() != 0) {
            out.attribute(null, ATT_USER_LOCKED, Integer.toString(getUserLockedFields()));
        }
        if (isFgServiceShown()) {
            out.attribute(null, ATT_FG_SERVICE_SHOWN, Boolean.toString(isFgServiceShown()));
        }
        if (canShowBadge()) {
            out.attribute(null, ATT_SHOW_BADGE, Boolean.toString(canShowBadge()));
        }
        if (isDeleted()) {
            out.attribute(null, ATT_DELETED, Boolean.toString(isDeleted()));
        }
        if (getGroup() != null) {
            out.attribute(null, ATT_GROUP, getGroup());
        }
        if (isBlockable()) {
            out.attribute(null, ATT_BLOCKABLE_SYSTEM, Boolean.toString(isBlockable()));
        }
        if (getAllowBubbles() != DEFAULT_ALLOW_BUBBLE) {
            out.attribute(null, ATT_ALLOW_BUBBLE, Integer.toString(getAllowBubbles()));
        }
        if (getOriginalImportance() != DEFAULT_IMPORTANCE) {
            out.attribute(null, ATT_ORIG_IMP, Integer.toString(getOriginalImportance()));
        }
        if (getParentChannelId() != null) {
            out.attribute(null, ATT_PARENT_CHANNEL, getParentChannelId());
        }
        if (getConversationId() != null) {
            out.attribute(null, ATT_CONVERSATION_ID, getConversationId());
        }
        if (isDemoted()) {
            out.attribute(null, ATT_DEMOTE, Boolean.toString(isDemoted()));
        }
        if (isImportantConversation()) {
            out.attribute(null, ATT_IMP_CONVERSATION, Boolean.toString(isImportantConversation()));
        }

        // mImportanceLockedDefaultApp and mImportanceLockedByOEM have a different source of
        // truth and so aren't written to this xml file

        out.endTag(null, TAG_CHANNEL);
    }

    /**
     * @hide
     */
    @SystemApi
    public JSONObject toJson() throws JSONException {
        JSONObject record = new JSONObject();
        record.put(ATT_ID, getId());
        record.put(ATT_NAME, getName());
        record.put(ATT_DESC, getDescription());
        if (getImportance() != DEFAULT_IMPORTANCE) {
            record.put(ATT_IMPORTANCE,
                    NotificationListenerService.Ranking.importanceToString(getImportance()));
        }
        if (canBypassDnd()) {
            record.put(ATT_PRIORITY, Notification.PRIORITY_MAX);
        }
        if (getLockscreenVisibility() != DEFAULT_VISIBILITY) {
            record.put(ATT_VISIBILITY, Notification.visibilityToString(getLockscreenVisibility()));
        }
        if (getSound() != null) {
            record.put(ATT_SOUND, getSound().toString());
        }
        if (getAudioAttributes() != null) {
            record.put(ATT_USAGE, Integer.toString(getAudioAttributes().getUsage()));
            record.put(ATT_CONTENT_TYPE,
                    Integer.toString(getAudioAttributes().getContentType()));
            record.put(ATT_FLAGS, Integer.toString(getAudioAttributes().getFlags()));
        }
        record.put(ATT_LIGHTS, Boolean.toString(shouldShowLights()));
        record.put(ATT_LIGHT_COLOR, Integer.toString(getLightColor()));
        record.put(ATT_ON_TIME, Integer.toString(getLightOnTime()));
        record.put(ATT_OFF_TIME, Integer.toString(getLightOffTime()));
        record.put(ATT_VIBRATION_ENABLED, Boolean.toString(shouldVibrate()));
        record.put(ATT_USER_LOCKED, Integer.toString(getUserLockedFields()));
        record.put(ATT_FG_SERVICE_SHOWN, Boolean.toString(isFgServiceShown()));
        record.put(ATT_VIBRATION, longArrayToString(getVibrationPattern()));
        record.put(ATT_SHOW_BADGE, Boolean.toString(canShowBadge()));
        record.put(ATT_DELETED, Boolean.toString(isDeleted()));
        record.put(ATT_GROUP, getGroup());
        record.put(ATT_BLOCKABLE_SYSTEM, isBlockable());
        record.put(ATT_ALLOW_BUBBLE, getAllowBubbles());
        // TODO: original importance
        return record;
    }

    private static AudioAttributes safeAudioAttributes(XmlPullParser parser) {
        int usage = safeInt(parser, ATT_USAGE, AudioAttributes.USAGE_NOTIFICATION);
        int contentType = safeInt(parser, ATT_CONTENT_TYPE,
                AudioAttributes.CONTENT_TYPE_SONIFICATION);
        int flags = safeInt(parser, ATT_FLAGS, 0);
        return new AudioAttributes.Builder()
                .setUsage(usage)
                .setContentType(contentType)
                .setFlags(flags)
                .build();
    }

    private static Uri safeUri(XmlPullParser parser, String att) {
        final String val = parser.getAttributeValue(null, att);
        return val == null ? null : Uri.parse(val);
    }

    private static int safeInt(XmlPullParser parser, String att, int defValue) {
        final String val = parser.getAttributeValue(null, att);
        return tryParseInt(val, defValue);
    }

    private static int tryParseInt(String value, int defValue) {
        if (TextUtils.isEmpty(value)) return defValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defValue;
        }
    }

    private static boolean safeBool(XmlPullParser parser, String att, boolean defValue) {
        final String value = parser.getAttributeValue(null, att);
        if (TextUtils.isEmpty(value)) return defValue;
        return Boolean.parseBoolean(value);
    }

    private static long[] safeLongArray(XmlPullParser parser, String att, long[] defValue) {
        final String attributeValue = parser.getAttributeValue(null, att);
        if (TextUtils.isEmpty(attributeValue)) return defValue;
        String[] values = attributeValue.split(DELIMITER);
        long[] longValues = new long[values.length];
        for (int i = 0; i < values.length; i++) {
            try {
                longValues[i] = Long.parseLong(values[i]);
            } catch (NumberFormatException e) {
                longValues[i] = 0;
            }
        }
        return longValues;
    }

    private static String longArrayToString(long[] values) {
        StringBuffer sb = new StringBuffer();
        if (values != null && values.length > 0) {
            for (int i = 0; i < values.length - 1; i++) {
                sb.append(values[i]).append(DELIMITER);
            }
            sb.append(values[values.length - 1]);
        }
        return sb.toString();
    }

    public static final @android.annotation.NonNull Creator<NotificationChannel> CREATOR =
            new Creator<NotificationChannel>() {
        @Override
        public NotificationChannel createFromParcel(Parcel in) {
            return new NotificationChannel(in);
        }

        @Override
        public NotificationChannel[] newArray(int size) {
            return new NotificationChannel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotificationChannel that = (NotificationChannel) o;
        return getImportance() == that.getImportance()
                && mBypassDnd == that.mBypassDnd
                && getLockscreenVisibility() == that.getLockscreenVisibility()
                && mLights == that.mLights
                && getLightColor() == that.getLightColor()
                && getLightOnTime() == that.getLightOnTime()
                && getLightOffTime()== that.getLightOffTime()
                && getUserLockedFields() == that.getUserLockedFields()
                && isFgServiceShown() == that.isFgServiceShown()
                && mVibrationEnabled == that.mVibrationEnabled
                && mShowBadge == that.mShowBadge
                && isDeleted() == that.isDeleted()
                && isBlockable() == that.isBlockable()
                && mAllowBubbles == that.mAllowBubbles
                && Objects.equals(getId(), that.getId())
                && Objects.equals(getName(), that.getName())
                && Objects.equals(mDesc, that.mDesc)
                && Objects.equals(getSound(), that.getSound())
                && Arrays.equals(mVibration, that.mVibration)
                && Objects.equals(getGroup(), that.getGroup())
                && Objects.equals(getAudioAttributes(), that.getAudioAttributes())
                && mImportanceLockedByOEM == that.mImportanceLockedByOEM
                && mImportanceLockedDefaultApp == that.mImportanceLockedDefaultApp
                && mOriginalImportance == that.mOriginalImportance
                && Objects.equals(getParentChannelId(), that.getParentChannelId())
                && Objects.equals(getConversationId(), that.getConversationId())
                && isDemoted() == that.isDemoted()
                && isImportantConversation() == that.isImportantConversation();
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getId(), getName(), mDesc, getImportance(), mBypassDnd,
                getLockscreenVisibility(), getSound(), mLights, getLightColor(), getLightOnTime(),getLightOffTime(),
                getUserLockedFields(),
                isFgServiceShown(), mVibrationEnabled, mShowBadge, isDeleted(), getGroup(),
                getAudioAttributes(), isBlockable(), mAllowBubbles,
                mImportanceLockedByOEM, mImportanceLockedDefaultApp, mOriginalImportance,
                mParentId, mConversationId, mDemoted, mImportantConvo);
        result = 31 * result + Arrays.hashCode(mVibration);
        return result;
    }

    /** @hide */
    public void dump(PrintWriter pw, String prefix, boolean redacted) {
        String redactedName = redacted ? TextUtils.trimToLengthWithEllipsis(mName, 3) : mName;
        String output = "NotificationChannel{"
                + "mId='" + mId + '\''
                + ", mName=" + redactedName
                + getFieldsString()
                + '}';
        pw.println(prefix + output);
    }

    @Override
    public String toString() {
        return "NotificationChannel{"
                + "mId='" + mId + '\''
                + ", mName=" + mName
                + getFieldsString()
                + '}';
    }

    private String getFieldsString() {
        return  ", mDescription=" + (!TextUtils.isEmpty(mDesc) ? "hasDescription " : "")
                + ", mImportance=" + mImportance
                + ", mBypassDnd=" + mBypassDnd
                + ", mLockscreenVisibility=" + mLockscreenVisibility
                + ", mSound=" + mSound
                + ", mLights=" + mLights
                + ", mLightColor=" + mLightColor
                + ", mLightOnTime=" + mLightOnTime
                + ", mLightOffTime=" + mLightOffTime
                + ", mVibration=" + Arrays.toString(mVibration)
                + ", mUserLockedFields=" + Integer.toHexString(mUserLockedFields)
                + ", mFgServiceShown=" + mFgServiceShown
                + ", mVibrationEnabled=" + mVibrationEnabled
                + ", mShowBadge=" + mShowBadge
                + ", mDeleted=" + mDeleted
                + ", mGroup='" + mGroup + '\''
                + ", mAudioAttributes=" + mAudioAttributes
                + ", mBlockableSystem=" + mBlockableSystem
                + ", mAllowBubbles=" + mAllowBubbles
                + ", mImportanceLockedByOEM=" + mImportanceLockedByOEM
                + ", mImportanceLockedDefaultApp=" + mImportanceLockedDefaultApp
                + ", mOriginalImp=" + mOriginalImportance
                + ", mParent=" + mParentId
                + ", mConversationId=" + mConversationId
                + ", mDemoted=" + mDemoted
                + ", mImportantConvo=" + mImportantConvo;
    }

    /** @hide */
    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        final long token = proto.start(fieldId);

        proto.write(NotificationChannelProto.ID, mId);
        proto.write(NotificationChannelProto.NAME, mName);
        proto.write(NotificationChannelProto.DESCRIPTION, mDesc);
        proto.write(NotificationChannelProto.IMPORTANCE, mImportance);
        proto.write(NotificationChannelProto.CAN_BYPASS_DND, mBypassDnd);
        proto.write(NotificationChannelProto.LOCKSCREEN_VISIBILITY, mLockscreenVisibility);
        if (mSound != null) {
            proto.write(NotificationChannelProto.SOUND, mSound.toString());
        }
        proto.write(NotificationChannelProto.USE_LIGHTS, mLights);
        proto.write(NotificationChannelProto.LIGHT_COLOR, mLightColor);
        if (mVibration != null) {
            for (long v : mVibration) {
                proto.write(NotificationChannelProto.VIBRATION, v);
            }
        }
        proto.write(NotificationChannelProto.USER_LOCKED_FIELDS, mUserLockedFields);
        proto.write(NotificationChannelProto.FG_SERVICE_SHOWN, mFgServiceShown);
        proto.write(NotificationChannelProto.IS_VIBRATION_ENABLED, mVibrationEnabled);
        proto.write(NotificationChannelProto.SHOW_BADGE, mShowBadge);
        proto.write(NotificationChannelProto.IS_DELETED, mDeleted);
        proto.write(NotificationChannelProto.GROUP, mGroup);
        if (mAudioAttributes != null) {
            mAudioAttributes.dumpDebug(proto, NotificationChannelProto.AUDIO_ATTRIBUTES);
        }
        proto.write(NotificationChannelProto.IS_BLOCKABLE_SYSTEM, mBlockableSystem);
        proto.write(NotificationChannelProto.ALLOW_APP_OVERLAY, mAllowBubbles);

        proto.end(token);
    }
}
