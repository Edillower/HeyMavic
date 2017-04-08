package com.edillower.heymavic;

import android.app.Service;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.edillower.heymavic.common.DJISimulatorApplication;

import dji.common.product.Model;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;

/**
 * This class is designed for showing the fpv video feed from the camera or Lightbridge 2.
 */
public class BaseFpvView extends RelativeLayout implements TextureView.SurfaceTextureListener {

    private TextureView mVideoSurface = null;
    private VideoFeeder.VideoDataCallback receivedVideoDataCallback = null;
    private DJICodecManager mCodecManager = null;

    public BaseFpvView(Context context) {
        super(context);

        initUI();
    }

    private void initUI() {
        LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Service.LAYOUT_INFLATER_SERVICE);

        View content = layoutInflater.inflate(R.layout.layout_fpvscreen, null, false);
        RelativeLayout.LayoutParams rlParam = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        addView(content, rlParam);

        Log.v("TAG","Start to test");

        mVideoSurface = (TextureView) findViewById(R.id.texture_video_previewer_surface);

        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);

            // This callback is for

            receivedVideoDataCallback = new VideoFeeder.VideoDataCallback() {
                @Override
                public void onReceive(byte[] bytes, int size) {
                    if (null != mCodecManager) {
                        mCodecManager.sendDataToDecoder(bytes, size);
                    }
                }
            };
        }

        initSDKCallback();

    }

    private void initSDKCallback() {
        try {
            BaseProduct mProduct = DJISimulatorApplication.getProductInstance();

            if (VideoFeeder.getInstance().getVideoFeeds() != null
                    && VideoFeeder.getInstance().getVideoFeeds().size() > 0) {
                VideoFeeder.getInstance().getVideoFeeds().get(0).setCallback(receivedVideoDataCallback);
            }

        } catch (Exception exception) {

        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(getContext(), surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }
        mCodecManager = new DJICodecManager(getContext(), surface, width, height);
        initUI();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }


}
