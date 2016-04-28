package com.davidbarron.dlbwatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.Gravity;
import android.view.SurfaceHolder;

import java.lang.ref.WeakReference;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


public class DLBWatchFace extends CanvasWatchFaceService {


    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<DLBWatchFace.Engine> mWeakReference;

        public EngineHandler(DLBWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            DLBWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;

        boolean mAmbient;
        Time mTime;
        Bitmap mHourBitmap, mHourScaledBitmap;
        Bitmap mMinuteBitmap, mMinuteScaledBitmap;
        Bitmap mSecondBitmap, mSecondScaledBitmap;
        Bitmap mBackgroundBitmap, mBackgroundScaledBitmap;
        Paint mTextPaint;
        Resources resources;
        String[] days = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
        String[] months = {"JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC"};
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        boolean mLowBitAmbient;

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            float centerX = bounds.width() / 2f;
            float centerY = bounds.height() / 2f;

            final float secondsRotation = mTime.second * 6f;
            final float minutesRotation = mTime.minute * 6f;
            final float hourHandOffset = mTime.minute / 2f;
            final float hoursRotation = (mTime.hour * 30) + hourHandOffset;

            if (!isInAmbientMode()) {
                canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);
                canvas.drawText(days[mTime.weekDay],centerX - (bounds.width()/4), centerY , mTextPaint);
                canvas.drawText(months[mTime.month] + " " + String.valueOf(mTime.monthDay), centerX + (bounds.width()/5), centerY, mTextPaint);
            }
            else {
                canvas.drawColor(Color.BLACK);
            }
            canvas.save();
            canvas.rotate(hoursRotation, centerX, centerY);
            canvas.drawBitmap(mHourScaledBitmap, 0, 0, null);

            canvas.rotate(minutesRotation - hoursRotation, centerX, centerY);
            canvas.drawBitmap(mMinuteScaledBitmap, 0, 0, null);

            if (!isInAmbientMode()) {
                canvas.rotate(secondsRotation - minutesRotation, centerX, centerY);
                canvas.drawBitmap(mSecondScaledBitmap, 0, 0, null);
            }
            canvas.restore();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                }
                invalidate();
            }
            updateTimer();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(DLBWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setStatusBarGravity(Gravity.TOP | Gravity.CENTER)
                    .setHotwordIndicatorGravity(Gravity.TOP | Gravity.CENTER)
                    .build());

            resources = DLBWatchFace.this.getResources();

            mTextPaint = new Paint();
            mTextPaint.setColor(resources.getColor(R.color.analog_hands));
            mTextPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            mTextPaint.setAntiAlias(true);
            mTextPaint.setTextSize(resources.getDimension(R.dimen.text_size));
            mTextPaint.setTextAlign(Paint.Align.CENTER);
            mTime = new Time();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            Drawable drawable = resources.getDrawable(R.drawable.hourhand);
            mHourBitmap = ((BitmapDrawable) drawable).getBitmap();

            drawable = resources.getDrawable(R.drawable.minutehand);
            mMinuteBitmap = ((BitmapDrawable) drawable).getBitmap();

            drawable = resources.getDrawable(R.drawable.secondhand);
            mSecondBitmap = ((BitmapDrawable) drawable).getBitmap();

            drawable = resources.getDrawable(R.drawable.square_bg);
            mBackgroundBitmap = ((BitmapDrawable) drawable).getBitmap();
            if (mBackgroundScaledBitmap == null || mBackgroundScaledBitmap.getWidth() != width || mBackgroundScaledBitmap.getHeight() != height) {
                mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,width, height, true);
            }
            if (mHourScaledBitmap == null || mHourScaledBitmap.getWidth() != width || mHourScaledBitmap.getHeight() != height) {
                mHourScaledBitmap = Bitmap.createScaledBitmap(mHourBitmap,width, height, true);
            }
            if (mMinuteScaledBitmap == null || mMinuteScaledBitmap.getWidth() != width || mMinuteScaledBitmap.getHeight() != height) {
                mMinuteScaledBitmap = Bitmap.createScaledBitmap(mMinuteBitmap, width, height, true );
            }
            if (mSecondScaledBitmap == null || mSecondScaledBitmap.getWidth() != width || mSecondScaledBitmap.getHeight() != height) {
                mSecondScaledBitmap = Bitmap.createScaledBitmap(mSecondBitmap, width, height, true);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            }
            else {
                unregisterReceiver();
            }
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            DLBWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            DLBWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}
