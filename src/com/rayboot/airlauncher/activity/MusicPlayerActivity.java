package com.rayboot.airlauncher.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.flaviofaria.kenburnsview.KenBurnsView;
import com.rayboot.airlauncher.R;
import com.rayboot.airlauncher.adapter.MusicDetailAdapter;
import com.rayboot.airlauncher.base.BaseActionBarActivity;
import com.rayboot.airlauncher.model.FileObj;
import com.rayboot.airlauncher.model.MusicDetailObj;
import com.rayboot.airlauncher.model.MusicObj;
import com.rayboot.airlauncher.musicservice.MusicService;
import com.rayboot.airlauncher.musicservice.PlayList;
import com.rayboot.airlauncher.musicservice.PlayMode;
import com.rayboot.airlauncher.util.PicUtil;
import com.rayboot.airlauncher.util.StackBlurManager;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author rayboot
 * @from 14-6-24 9:08
 * @TODO
 */
public class MusicPlayerActivity extends BaseActionBarActivity
{
    @InjectView(R.id.ivMusicLogo) ImageView mIvMusicLogo;
    @InjectView(R.id.tvMusicTitle) TextView mTvMusicTitle;
    @InjectView(R.id.tvMusicOwner) TextView mTvMusicOwner;
    @InjectView(R.id.lvMusic) ListView mLvMusic;
    @InjectView(R.id.btnPre) Button mBtnPre;
    @InjectView(R.id.btnPlayPause) Button mBtnPlayPause;
    @InjectView(R.id.btnNext) Button mBtnNext;
    @InjectView(R.id.btnPlayMode) Button mBtnPlayMode;
    @InjectView(R.id.seekBar) SeekBar mSeekBar;

    MusicObj curMusicObj;
    MusicService mService = null;
    List<MusicDetailObj> musicDetailObjs = new ArrayList<MusicDetailObj>(10);
    MusicDetailAdapter<MusicDetailObj> adapter;
    @InjectView(R.id.ivMainBg) KenBurnsView mIvMainBg;
    private StackBlurManager _stackBlurManager;

    @Override public void onCreate(Bundle savedInstanceState)
    {
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);
        ButterKnife.inject(this);
        getSupportActionBar().setBackgroundDrawable(
                getResources().getDrawable(R.drawable.nv_bg));

        curMusicObj = (MusicObj) getIntent().getSerializableExtra("music_obj");
        _stackBlurManager = new StackBlurManager(
                BitmapFactory.decodeFile(curMusicObj.imgPath));

        for (String path : curMusicObj.filePath.split(FileObj.SPLIT_STRING))
        {
            musicDetailObjs.add(new MusicDetailObj(path));
        }

        adapter = new MusicDetailAdapter<MusicDetailObj>(this, musicDetailObjs);
        mLvMusic.setAdapter(adapter);
        mLvMusic.setOnItemClickListener(onItemClickListener);

        PicUtil.getPicasso()
                .load(new File(curMusicObj.imgPath))
                .placeholder(R.drawable.ic_launcher)
                .into(mIvMusicLogo);
        mTvMusicTitle.setText(curMusicObj.title);
        mTvMusicOwner.setText(musicDetailObjs.get(0).owner);

        mSeekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener()
                {
                    @Override public void onProgressChanged(SeekBar seekBar,
                            int progress, boolean fromUser)
                    {
                        if (fromUser && mService != null)
                        {
                            mService.setPosition(progress * 1000);
                        }
                    }

                    @Override public void onStartTrackingTouch(SeekBar seekBar)
                    {

                    }

                    @Override public void onStopTrackingTouch(SeekBar seekBar)
                    {

                    }
                });
        onBlur();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        updateHandler.postDelayed(new Updater(), 100);
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MusicService.LocalBinder binder =
                    (MusicService.LocalBinder) service;
            mService = binder.getService();
            mService.setPlayList(new PlayList(musicDetailObjs));
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0)
        {
            mService = null;
        }
    };

    private AdapterView.OnItemClickListener onItemClickListener =
            new AdapterView.OnItemClickListener()
            {
                @Override public void onItemClick(AdapterView<?> parent,
                        View view, int position, long id)
                {
                    if (mService != null)
                    {
                        mService.processPlayNowRequest(
                                (MusicDetailObj) adapter.getItem(position));
                    }
                }
            };

    public void onControlClick(View view)
    {
        if (mService == null)
        {
            return;
        }

        switch (view.getId())
        {
        case R.id.btnNext:
            mService.processPlayNextRequest();
            break;
        case R.id.btnPre:
            mService.processPlayPreRequest();
            break;
        case R.id.btnPlayPause:
            mService.processPlayPauseRequest();
            break;
        }
    }

    public void onModeClick(View view)
    {
        ((Button) view).setText(PlayMode.getModeName(PlayMode.nextMode()));
    }

    public void onVoiceClick(View view)
    {

    }

    private Handler updateHandler = new Handler();

    private class Updater implements Runnable
    {
        public void run()
        {
            if (mService == null)
            {
                updateHandler.postDelayed(this, 100);
                return;
            }
            MusicDetailObj currentObj = mService.getPlayList().getCurrent();
            if (currentObj != null)
            {
                int position = mService.getPosition();
                int duration = currentObj.time;

                mSeekBar.setMax(duration);
                mSeekBar.setProgress(position / 1000);

                switch (mService.getState())
                {
                case Stopped:
                case Paused:
                    mBtnPlayPause.setBackgroundResource(
                            android.R.drawable.ic_media_play);
                    break;
                case Preparing:
                case Playing:
                    mBtnPlayPause.setBackgroundResource(
                            android.R.drawable.ic_media_pause);
                    break;
                }
            }
            else
            {
                mSeekBar.setMax(0);
                mSeekBar.setProgress(0);
                mBtnPlayPause.setBackgroundResource(
                        android.R.drawable.ic_media_pause);
                mBtnPlayPause.setBackgroundResource(
                        android.R.drawable.ic_media_play);
            }

            updateHandler.postDelayed(this, 100);
        }
    }

    public void changeFile(MusicDetailObj file)
    {
        //ImageView art = (ImageView) findViewById(R.id.playingIcon);
        //TextView info = (TextView) findViewById(R.id.playingInfo);
        //ImageView play = (ImageView) findViewById(R.id.playPauseIcon);
        //
        //if (file != null) {
        //    try {
        //        InputStream in = getContentResolver().openInputStream(file.getImageUri());
        //        Bitmap bitmap = BitmapFactory.decodeStream(in);
        //        art.setImageBitmap(bitmap);
        //    }
        //    catch (FileNotFoundException e) {
        //        art.setImageResource(R.drawable.ic_tab_artists_white);
        //    }
        //
        //    info.setText(file.toString());
        //    play.setImageResource(R.drawable.play);
        //}
        //else {
        //    art.setImageBitmap(null);
        //    info.setText("");
        //    play.setImageBitmap(null);
        //}
        //
        //playListAdapter.notifyDataSetChanged();
    }

    private void onBlur()
    {
        int radius = 10;
        mIvMainBg.setImageBitmap(_stackBlurManager.process(radius));
    }
}