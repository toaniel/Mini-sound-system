package fr.bowserf.testsoundsystem;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Arrays;

import fr.bowserf.soundsystem.SoundSystem;
import fr.bowserf.soundsystem.listener.SSExtractionObserver;
import fr.bowserf.soundsystem.listener.SSPlayingStatusObserver;
import fr.bowserf.testsoundsystem.spectrum.SpectrumGLSurfaceView;
import fr.bowserf.testsoundsystem.utils.AudioFeaturesManager;
import fr.bowserf.testsoundsystem.utils.FindTrackManager;

/**
 * Simple activity launching the sound system.
 */
public class MainActivity extends AppCompatActivity {

    @SuppressWarnings("unused")
    private static final String TAG = "MainActivity";

    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;

    private static final int MUSIC_LENGTH_DIVISION = 40;

    /**
     * UI
     */
    private Button mToggleStop;
    private Button mBtnExtractFile;
    private TextView mTvSoundSystemStatus;
    private TextView mTvPercentageDisplayed;
    private ToggleButton mTogglePlayPause;
    private SpectrumGLSurfaceView mSpectrum;

    /**
     * Sound system
     */
    private SoundSystem mSoundSystem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final AudioFeaturesManager audioFeaturesManager = AudioFeaturesManager.init(this);

        mSoundSystem = SoundSystem.getInstance();
        if(!mSoundSystem.isSoundSystemInit()) {
            mSoundSystem.initSoundSystem(
                    audioFeaturesManager.getSampleRate(),
                    audioFeaturesManager.getFramesPerBuffer());
        }

        initUI();

        attachToListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSpectrum.onResume();
    }

    @Override
    protected void onPause() {
        mSpectrum.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if(!isChangingConfigurations()) {
            detachListeners();
            mSoundSystem.release();
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mSoundSystem.loadFile(FindTrackManager.getTrackPath(MainActivity.this).getPath());
        } else {
            Toast.makeText(this, "No permission, no extraction !", Toast.LENGTH_SHORT).show();
        }
    }

    private void initUI() {
        // extract button
        mBtnExtractFile = (Button) findViewById(R.id.toggle_extract_file);
        mBtnExtractFile.setOnClickListener(mOnClickListener);

        // play pause button
        mTogglePlayPause = (ToggleButton) findViewById(R.id.toggle_play_pause);
        mTogglePlayPause.setOnCheckedChangeListener(mOnCheckedChangeListener);

        // stop button
        mToggleStop = (Button) findViewById(R.id.btn_stop);
        mToggleStop.setOnClickListener(mOnClickListener);

        // tv sound system status
        mTvSoundSystemStatus = (TextView) findViewById(R.id.tv_sound_system_status);

        // tv percentage displayed
        mTvPercentageDisplayed = (TextView) findViewById(R.id.tv_percentage_displayed);

        final Button buttonCodecs = (Button)findViewById(R.id.btn_display_available_codecs);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            buttonCodecs.setOnClickListener(mOnClickListener);
        }else{
            buttonCodecs.setVisibility(View.GONE);
        }

        if(mSoundSystem.isLoaded()){
            mTogglePlayPause.setEnabled(true);
            mToggleStop.setEnabled(true);
            mBtnExtractFile.setEnabled(false);
        }else{
            mBtnExtractFile.setEnabled(true);
            mTogglePlayPause.setEnabled(false);
            mToggleStop.setEnabled(false);
        }

        mTogglePlayPause.setChecked(mSoundSystem.isPlaying());

        mSpectrum = (SpectrumGLSurfaceView)findViewById(R.id.spectrum);
    }

    private void attachToListeners() {
        mSoundSystem.addPlayingStatusObserver(mSSPlayingStatusObserver);
        mSoundSystem.addExtractionObserver(mSSExtractionObserver);
    }

    private void detachListeners() {
        mSoundSystem.removePlayingStatusObserver(mSSPlayingStatusObserver);
        mSoundSystem.removeExtractionObserver(mSSExtractionObserver);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void displayAvailableAudioCodecs(){
        final MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        final MediaCodecInfo[] codecInfos = mediaCodecList.getCodecInfos();
        final StringBuilder stringBuilder = new StringBuilder();
        for (final MediaCodecInfo codecInfo : codecInfos) {
            if(!codecInfo.isEncoder()) {
                boolean isAudioCodec = false;
                for (final String type : codecInfo.getSupportedTypes()) {
                    if (type.contains("audio/")) {
                        isAudioCodec = true;
                    }
                }
                if(isAudioCodec){
                    stringBuilder.append(codecInfo.getName()).append(" : \n");
                    for (final String type : codecInfo.getSupportedTypes()) {
                        if (type.contains("audio/")) {
                            stringBuilder.append(type).append("\n");
                        }
                    }
                }
            }
        }

        final AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("Available codecs :")
                .setMessage(stringBuilder.toString())
                .setPositiveButton(android.R.string.ok, null)
                .create();
        alertDialog.show();
    }

    private SSExtractionObserver mSSExtractionObserver = new SSExtractionObserver() {
        @Override
        public void onExtractionStarted() {
            mBtnExtractFile.setEnabled(false);
            mTvSoundSystemStatus.setText("Extraction started");
        }

        @Override
        public void onExtractionCompleted() {
            mTogglePlayPause.setEnabled(true);
            mToggleStop.setEnabled(true);
            mTvSoundSystemStatus.setText("Extraction ended");

            mTvPercentageDisplayed.setVisibility(View.VISIBLE);
            mTvPercentageDisplayed.setText("Music displayed : 1 / " + MUSIC_LENGTH_DIVISION + " of the total length");

            final DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);

            // we only want to display 1/40 of all data
            final short[] extractedData = mSoundSystem.getExtractedDataMono();
            final short[] reducedData = Arrays.copyOf(extractedData, extractedData.length / MUSIC_LENGTH_DIVISION);

            mSpectrum.drawData(reducedData, metrics.widthPixels);
            mSpectrum.requestRender();
        }
    };

    private SSPlayingStatusObserver mSSPlayingStatusObserver = new SSPlayingStatusObserver() {
        @Override
        public void onPlayingStatusDidChange(final boolean playing) {
            mTvSoundSystemStatus.setText(playing
                    ? "Playing"
                    : "Pause");
        }

        @Override
        public void onEndOfMusic() {
            mTogglePlayPause.setChecked(false);
            mTvSoundSystemStatus.setText("Track finished");
        }

        @Override
        public void onStopTrack() {
            mTogglePlayPause.setChecked(false);
            mTvSoundSystemStatus.setText("Track stopped");
        }
    };

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.toggle_extract_file:
                    loadTrackOrAskPermission();
                    break;
                case R.id.btn_stop:
                    mSoundSystem.stopMusic();
                    break;
                case R.id.btn_display_available_codecs:
                    displayAvailableAudioCodecs();
                    break;
            }
        }
    };

    private void loadTrackOrAskPermission() {
        final int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            String trackPath = FindTrackManager.getTrackPath(MainActivity.this).getPath();
            Log.i(TAG, trackPath);
            mSoundSystem.loadFile(trackPath);
        } else {

            askForReadExternalStoragePermission();
        }
    }

    private void askForReadExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        }
    }

    private CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            switch (buttonView.getId()) {
                case R.id.toggle_play_pause:
                    mSoundSystem.playMusic(isChecked);
                    break;
            }
        }
    };

}
