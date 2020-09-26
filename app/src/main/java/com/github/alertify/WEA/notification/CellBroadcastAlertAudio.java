package com.github.alertify.WEA.notification;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.github.alertify.R;
import com.github.alertify.WEA.misc.CellBroadcastSettings;

import static com.github.alertify.WEA.CellBroadcastAlertService.AlertType;

public class CellBroadcastAlertAudio extends Service  {
    public static final String TAG = "CellBroadcastAlertAudio";

    /** Action to start playing alert audio/vibration/speech. */
    public static final String ACTION_START_ALERT_AUDIO = "ACTION_START_ALERT_AUDIO";

    /** Extra for alert tone type */
    public static final String ALERT_AUDIO_TONE_TYPE = "com.github.alertify.WEA.ALERT_AUDIO_TONE_TYPE";

    /** Extra for alert vibration pattern (unless main volume is silent). */
    public static final String ALERT_AUDIO_VIBRATION_PATTERN_EXTRA = "com.github.alertify.WEA.ALERT_AUDIO_VIBRATION_PATTERN";

    /** Extra for playing alert sound in full volume regardless Do Not Disturb is on. */
    public static final String ALERT_AUDIO_OVERRIDE_DND_EXTRA = "com.github.alertify.WEA.ALERT_OVERRIDE_DND_EXTRA";

    /** Extra for cutomized alert duration in ms. */
    public static final String ALERT_AUDIO_DURATION = "com.github.alertify.WEA.ALERT_AUDIO_DURATION";

    /** Extra for alert subscription index */
    public static final String ALERT_AUDIO_SUB_INDEX = "com.github.alertify.WEA.ALERT_AUDIO_SUB_INDEX";

    private static final int STATE_IDLE = 0;
    private static final int STATE_ALERTING = 1;


    private static final boolean DBG = true;

    private int mState;
    private AlertType mAlertType;
    private int mSubId;
    private boolean mEnableAudio;
    private boolean mOverrideDnd;
    private boolean mResetAlarmVolumeNeeded;
    private int mUserSetAlarmVolume;
    private int mAlertDuration = -1;

    private MediaPlayer mMediaPlayer;
    private AudioManager mAudioManager;

    // Internal messages
    private static final int ALERT_SOUND_FINISHED = 1000;
    private static final int ALERT_PAUSE_FINISHED = 1001;
    private Handler mHandler;

    public void onCreate() {
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == ALERT_SOUND_FINISHED) {
                    if (DBG) log("ALERT_SOUND_FINISHED");
                    stop();     // stop alert sound
                } else {
                    loge("Handler received unknown message, what=" + msg.what);
                }
            }
        };

    }

    @Override
    public void onDestroy() {
        // stop audio, vibration and TTS
        if (DBG) log("onDestroy");
        stop();
        mAudioManager.abandonAudioFocus(null);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // No intent, tell the system not to restart us.
        if (intent == null) {
            if (DBG) log("Null intent. Stop CellBroadcastAlertAudio service");
            stopSelf();
            return START_NOT_STICKY;
        }

        mSubId = intent.getIntExtra(ALERT_AUDIO_SUB_INDEX,-1);

        // retrieve whether to play alert sound in full volume regardless Do Not Disturb is on.
        mOverrideDnd = intent.getBooleanExtra(ALERT_AUDIO_OVERRIDE_DND_EXTRA, true);
        Resources res = CellBroadcastSettings.getResources(getApplicationContext(), mSubId);
        // retrieve the customized alert duration. -1 means play the alert with the tone's duration.
        mAlertDuration = intent.getIntExtra(ALERT_AUDIO_DURATION, -1);
        // retrieve the alert type
        mAlertType = AlertType.DEFAULT;
        if (intent.getSerializableExtra(ALERT_AUDIO_TONE_TYPE) != null) {
            mAlertType = (AlertType) intent.getSerializableExtra(ALERT_AUDIO_TONE_TYPE);
        }

         mEnableAudio = true;
         playAlertTone(mAlertType);
        return START_STICKY;
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    private void playAlertTone(AlertType alertType ) {
        // stop() checks to see if we are already playing.
        stop();
        loge("WE DIDIT BOIS");

        log("playAlertTone: alertType=" + alertType + ", mEnableVibrate="
                + ", mEnableAudio=" + mEnableAudio + ", mOverrideDnd=" + mOverrideDnd
                + ", mSubId=" + mSubId);
        Resources res = CellBroadcastSettings.getResources(getApplicationContext(), mSubId);

        // Get the alert tone duration. Negative tone duration value means we only play the tone
        // once, not repeat it.
        int customAlertDuration = mAlertDuration;

        // future optimization: reuse media player object
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnErrorListener((mp, what, extra) -> {
            loge("Error occurred while playing audio.");
                mHandler.sendMessage(mHandler.obtainMessage(ALERT_SOUND_FINISHED));
                return true;
                });

        mMediaPlayer.setOnCompletionListener(mp -> {
            if (DBG) log("Audio playback complete.");
                mHandler.sendMessage(mHandler.obtainMessage(ALERT_SOUND_FINISHED));
        });


        try {
            //log("Locale=" + res.getConfiguration().getLocales() + ", alertType=" + alertType);
            // Load the tones based on type
            switch (alertType) {
                case ETWS_EARTHQUAKE:
                    setDataSourceFromResource(res, mMediaPlayer, R.raw.etws_earthquake);
                    break;
                case ETWS_TSUNAMI:
                    setDataSourceFromResource(res, mMediaPlayer, R.raw.etws_tsunami);
                    break;
                case OTHER:
                    setDataSourceFromResource(res, mMediaPlayer, R.raw.etws_other_disaster);
                    break;
                case CMAS_OTHER:
                    setDataSourceFromResource(res, mMediaPlayer, R.raw.other);
                    break;
                case ETWS_DEFAULT:
                    setDataSourceFromResource(res, mMediaPlayer, R.raw.etws_default);
                    break;
                case INFO:
                    setDataSourceFromResource(res, mMediaPlayer, R.raw.info);
                    break;
                case TEST:
                case DEFAULT:
                default:
                    setDataSourceFromResource(res, mMediaPlayer, R.raw.default_tone);
                    //setDataSourceFromResource(res, mMediaPlayer, R.raw.default_tone);
            }

                // Request audio focus (though we're going to play even if we don't get it)
                mAudioManager.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                mMediaPlayer.setAudioAttributes(getAlertAudioAttributes(mAlertType));
                setAlertVolume(mAlertType);

                // If we are using the custom alert duration, set looping to true so we can repeat
                // the alert. The tone playing will stop when ALERT_SOUND_FINISHED arrives.
                // Otherwise we just play the alert tone once.
                mMediaPlayer.setLooping(customAlertDuration >= 0);
                mMediaPlayer.prepare();
                mMediaPlayer.start();

            } catch (Exception ex) {
                loge("Failed to play alert sound: " + ex);
                // Immediately move into the next state ALERT_SOUND_FINISHED.
                mHandler.sendMessage(mHandler.obtainMessage(ALERT_SOUND_FINISHED));
            }

        mState = STATE_ALERTING;
    }

    private static void setDataSourceFromResource(Resources resources, MediaPlayer player, int res) throws java.io.IOException {
        AssetFileDescriptor afd = resources.openRawResourceFd(res);
        if (afd != null) {
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
        }
    }


    /**
     * Stops alert audio and speech.
     */
    public void stop() {
        if (DBG) log("stop()");

        mHandler.removeMessages(ALERT_SOUND_FINISHED);
        mHandler.removeMessages(ALERT_PAUSE_FINISHED);

        resetAlarmStreamVolume(mAlertType);

        if (mState == STATE_ALERTING) {
            // Stop audio playing
            if (mMediaPlayer != null) {
                try {
                    mMediaPlayer.stop();
                    mMediaPlayer.release();
                } catch (IllegalStateException e) {
                    // catch "Unable to retrieve AudioTrack pointer for stop()" exception
                    loge("exception trying to stop media player");
                }
                mMediaPlayer = null;
            }
        }
        mState = STATE_IDLE;
    }

    /**
     * Get audio attribute for the alarm.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private AudioAttributes getAlertAudioAttributes(AlertType alertType) {
        AudioAttributes.Builder builder = new AudioAttributes.Builder();

        builder.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION);
        builder.setUsage((alertType == AlertType.INFO ? AudioAttributes.USAGE_NOTIFICATION : AudioAttributes.USAGE_ALARM));
        return builder.build();
    }

    /**
     * Set volume for alerts.
     */
    private void setAlertVolume(AlertType alertType) {
            setAlarmStreamVolumeToFull(alertType);
    }

    /**
     * Set volume of STREAM_ALARM to full.
     */
    private void setAlarmStreamVolumeToFull(AlertType alertType) {
        log("setting alarm volume to full for cell broadcast alerts.");
        int streamType = (alertType == AlertType.INFO) ? AudioManager.STREAM_NOTIFICATION : AudioManager.STREAM_ALARM;
        mUserSetAlarmVolume = mAudioManager.getStreamVolume(streamType);
        mResetAlarmVolumeNeeded = true;
        mAudioManager.setStreamVolume(streamType, mAudioManager.getStreamMaxVolume(streamType), 0);
    }

    /**
     * Reset volume of STREAM_ALARM, if needed.
     */
    private void resetAlarmStreamVolume(AlertType alertType) {
        if (mResetAlarmVolumeNeeded) {
            log("resetting alarm volume to back to " + mUserSetAlarmVolume);
            mAudioManager.setStreamVolume(alertType == AlertType.INFO  ? AudioManager.STREAM_NOTIFICATION : AudioManager.STREAM_ALARM,  mUserSetAlarmVolume, 0);
            mResetAlarmVolumeNeeded = false;
        }
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }

    private static void loge(String msg) {
        Log.e(TAG, msg);
    }
}
