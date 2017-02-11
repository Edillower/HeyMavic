package com.edillower.heymavic;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.Service;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * FPV main control window
 * @author Eddie Wang
 */
public class FPVFullscreenActivity extends Activity{
    private TextureView fpvTexture;

    private Button mBtnInput;
    private boolean mBtnInput_flag=true;
    private EditText mTxtCmmand;
    private Button mBtmDummy;
    private String mStrIntention="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);

        fpvTexture=new TextureView(this);
        fpvTexture.setSurfaceTextureListener(new BaseFpvView(this));
        setContentView(fpvTexture);

        LayoutInflater layoutInflater = getLayoutInflater();
        View content = layoutInflater.inflate(R.layout.activity_fpvfullscreen, null, false);
        RelativeLayout.LayoutParams rlParam = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        addContentView(content,rlParam);
        initUI();


    }

    private void initUI(){
        mTxtCmmand=(EditText) findViewById(R.id.command_text);
        mBtnInput=(Button) findViewById(R.id.input_btn);
        mBtmDummy=(Button) findViewById(R.id.dummy_btn);
        voiceInputListener();
        inputBtnListener();
    }



    private void voiceInputListener(){
        mBtmDummy.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mTxtCmmand.setBackgroundResource(R.drawable.common_google_signin_btn_text_dark_pressed);
                        System.out.println("I got you too");
                        break;
                    case MotionEvent.ACTION_UP:
                        mTxtCmmand.setBackgroundResource(R.drawable.common_google_signin_btn_text_dark_normal);
                        break;
                }
                return false;
            }
        });
    }

    private void inputBtnListener(){
        mBtnInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBtnInput_flag){
                    mBtnInput.setBackgroundResource(R.drawable.keyboard);
                    mTxtCmmand.setHint("Enter Your Command");
                    mTxtCmmand.setEnabled(true);
                    mBtmDummy.setVisibility(View.GONE);
                    mBtnInput_flag=false;
//                    mTxtCmmand.addTextChangedListener(new TextWatcher() {
//                        @Override
//                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//
//                        }
//
//                        @Override
//                        public void onTextChanged(CharSequence s, int start, int before, int count) {
//
//                        }
//
//                        @Override
//                        public void afterTextChanged(Editable s) {
//
//                        }
//                    });
                }else{
                    mBtnInput.setBackgroundResource(R.drawable.mic);
                    mStrIntention=mTxtCmmand.getText().toString();
                    mTxtCmmand.setText("");
                    mTxtCmmand.setHint("Hold for Voice Input");
                    mTxtCmmand.setEnabled(false);
                    mBtmDummy.setVisibility(View.VISIBLE);
                    mBtnInput_flag=true;
                }
            }
        });
    }

}
