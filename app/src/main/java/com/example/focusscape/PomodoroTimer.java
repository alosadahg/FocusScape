package com.example.focusscape;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.Locale;

public class PomodoroTimer extends AppCompatActivity {
    private EditText mEditTextInput;
    private TextView mTextViewCountdown;
    private Button mButtonSet;
    private Button mButtonStartPause;
    private Button mButtonReset;

    private CountDownTimer mCountDownTimer;

    private boolean mTimerRunning;

    private long mStartTimeInMillis;
    private long mTimeLeftInMillis;
    private long mEndTime;

    private AlarmManager mAlarmManager;

    private Intent mAlarmIntent;
    private PendingIntent alarmPendingIntent;

    private CircularProgressIndicator timeCircularIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pomodoro_timer);

        mTextViewCountdown = findViewById(R.id.textViewCountdown);
        mButtonStartPause = findViewById(R.id.btnStartPause);
        mButtonReset = findViewById(R.id.btnReset);
        mEditTextInput = findViewById(R.id.editTextMins);
        mButtonSet = findViewById(R.id.btnSetMinutes);

        timeCircularIndicator = findViewById(R.id.cpiTime);
        timeCircularIndicator.setProgress(100);

        mButtonSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String inputMins = String.valueOf(mEditTextInput.getText());
                if(inputMins.length() == 0) {
                    Toast.makeText(view.getContext(),"Field can't be empty",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                long millisInput = Long.parseLong(inputMins) * 60000;
                if(millisInput == 0) {
                    Toast.makeText(view.getContext(),"Please enter a positive number",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                setTime(millisInput);
                mEditTextInput.setText("");
            }
        });

        mButtonStartPause.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                mAlarmIntent = new Intent(view.getContext(), PomodoroAlarmReceiver.class);
                PendingIntent mAlarmPendingIntent = PendingIntent.getBroadcast(view.getContext(),
                        0, mAlarmIntent, PendingIntent.FLAG_IMMUTABLE);

                if(mTimerRunning) {
                    pauseTimer();
                    mAlarmManager.cancel(mAlarmPendingIntent);
                } else {
                    startTimer();
                    mAlarmManager.setExact(AlarmManager.RTC_WAKEUP,
                            System.currentTimeMillis() + mTimeLeftInMillis,mAlarmPendingIntent);
                }
            }
        });

        mButtonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetTimer();
            }
        });

        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "PomodoroAlarm";
            String description = "Channel for alarm";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("focusscapealarm",name,importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void setTime(long milliseconds) {
        mStartTimeInMillis = milliseconds;
        resetTimer();
        closeKeyboard();
    }

    public void startTimer() {
        mEndTime = System.currentTimeMillis() + mTimeLeftInMillis;
        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeftInMillis = millisUntilFinished;
                updateCountDownTextAndIndicator();
            }

            @Override
            public void onFinish() {
                mTimerRunning = false;
                updateTimerInterface();
            }
        }.start();

        mTimerRunning = true;
        updateTimerInterface();
    }

    public void pauseTimer() {
        mCountDownTimer.cancel();
        mTimerRunning = false;
        updateTimerInterface();
    }

    public void resetTimer() {
        mTimeLeftInMillis = mStartTimeInMillis;
        updateCountDownTextAndIndicator();
        updateTimerInterface();
    }

    public void updateCountDownTextAndIndicator() {
        int hours = (int) mTimeLeftInMillis / 1000 / 3600;
        int minutes = (int) mTimeLeftInMillis / 1000 % 3600 / 60;
        int seconds = (int) mTimeLeftInMillis / 1000 % 60;

        String timeLeftFormatted;
        if(hours > 0) {
            timeLeftFormatted = String.format(Locale.getDefault(),
                    "%d:%02d:%02d",hours,minutes,seconds);
        } else {
            timeLeftFormatted = String.format(Locale.getDefault(),
                    "%02d:%02d",minutes,seconds);
        }
        mTextViewCountdown.setText(timeLeftFormatted);

        double progress = (mTimeLeftInMillis/ (double) mStartTimeInMillis) * 100;
        System.out.println(progress);
        timeCircularIndicator.setProgress((int) progress, true);
    }

    private void updateTimerInterface() {
        if(mTimerRunning) {
            mEditTextInput.setVisibility(View.INVISIBLE);
            mButtonSet.setVisibility(View.INVISIBLE);
            mButtonReset.setVisibility(View.INVISIBLE);
            mButtonStartPause.setText("Pause");
        } else {
            mEditTextInput.setVisibility(View.VISIBLE);
            mButtonSet.setVisibility(View.VISIBLE);
            mButtonStartPause.setText("Start");

            if(mTimeLeftInMillis < 1000) {
                mButtonStartPause.setVisibility(View.INVISIBLE);
            } else {
                mButtonStartPause.setVisibility(View.VISIBLE);
            }

            if(mTimeLeftInMillis < mStartTimeInMillis) {
                mButtonReset.setVisibility(View.VISIBLE);
            } else {
                mButtonReset.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        if(view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(),0);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        SharedPreferences prefs = getSharedPreferences("prefs",MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putLong("startTimeInMillis",mStartTimeInMillis);
        editor.putLong("millisLeft",mTimeLeftInMillis);
        editor.putBoolean("timerRunning",mTimerRunning);
        editor.putLong("endTime",mEndTime);

        editor.apply();

        if(mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        SharedPreferences prefs = getSharedPreferences("prefs",MODE_PRIVATE);

        mStartTimeInMillis = prefs.getLong("startTimeInMillis", 600000);
        mTimeLeftInMillis = prefs.getLong("millisLeft", mStartTimeInMillis);
        mTimerRunning = prefs.getBoolean("timerRunning", false);
        updateCountDownTextAndIndicator();
        updateTimerInterface();

        if(mTimerRunning) {
            mEndTime = prefs.getLong("endTime",0);
            mTimeLeftInMillis = mEndTime - System.currentTimeMillis();

            if(mTimeLeftInMillis < 0) {
                mTimeLeftInMillis = 0;
                mTimerRunning = false;
                updateCountDownTextAndIndicator();
                updateTimerInterface();
            } else {
                startTimer();
            }
        }
    }
}