package com.edillower.heymavic;

import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.TextureView;

public class FPVFullscreenActivity extends AppCompatActivity {
    private TextureView fpvTexture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        setContentView(R.layout.activity_fpvfullscreen);
        fpvTexture=new TextureView(this);
        fpvTexture.setSurfaceTextureListener(new BaseFpvView(this));
        setContentView(fpvTexture);
    }
}
