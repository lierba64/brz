package com.brz.mx5;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.brz.fragment.ProgrammeFragment;
import com.brz.fragment.ProgrammePresenter;
import com.brz.programme.Programme;
import com.brz.programme.ProgrammeContext;
import com.brz.programme.Theme;

import java.util.List;

import io.vov.vitamio.Vitamio;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {

    private ProgrammeContext mProgrammeContext;
    private ProgrammeFragment mFragment;

    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);

        Vitamio.isInitialized(this);

        mContentView = findViewById(R.id.fullscreen_content);
        hide();

        Theme theme = MX5Application.getInstance().getTheme2();
        List<Programme> programmeList = theme.getDefaults();

        mProgrammeContext = MX5Application.getInstance().getProgrammeContext(programmeList.get(0).getName());
        mFragment = new ProgrammeFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.fullscreen_content, mFragment).commit();
    }

    @Override
    protected void onResume() {
        super.onResume();

        new ProgrammePresenter(this, mProgrammeContext, mFragment);
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        mHideHandler.post(mHidePart2Runnable);
    }
}
