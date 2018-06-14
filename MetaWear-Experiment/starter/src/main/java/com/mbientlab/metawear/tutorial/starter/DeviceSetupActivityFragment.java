/*
 * Copyright 2015 MbientLab Inc. All rights reserved.
 *
 * IMPORTANT: Your use of this Software is limited to those specific rights
 * granted under the terms of a software license agreement between the user who
 * downloaded the software, his/her employer (which must be your employer) and
 * MbientLab Inc, (the "License").  You may not use this Software unless you
 * agree to abide by the terms of the License which can be found at
 * www.mbientlab.com/terms . The License limits your use, and you acknowledge,
 * that the  Software may not be modified, copied or distributed and can be used
 * solely and exclusively in conjunction with a MbientLab Inc, product.  Other
 * than for the foregoing purpose, you may not use, reproduce, copy, prepare
 * derivative works of, modify, distribute, perform, display or sell this
 * Software and/or its documentation for any purpose.
 *
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
 * PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
 * MBIENTLAB OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT, NEGLIGENCE,
 * STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE
 * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED
 * TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST
 * PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY,
 * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY
 * DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 *
 * Should you have any questions regarding your right to use this Software,
 * contact MbientLab Inc, at www.mbientlab.com.
 */

package com.mbientlab.metawear.tutorial.starter;




import android.Manifest;
import android.content.Context;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.*;
import android.widget.*;

import com.mbientlab.metawear.*;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.builder.filter.Comparison;
import com.mbientlab.metawear.builder.filter.Passthrough;
import com.mbientlab.metawear.data.EulerAngles;
import com.mbientlab.metawear.module.*;
import com.mbientlab.metawear.module.SensorFusionBosch.*;
import com.mbientlab.metawear.module.Switch;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import bolts.Continuation;
import bolts.Task;
import pl.droidsonroids.gif.GifTextView;


/**
 * A placeholder fragment containing a simple view.
 */
public class DeviceSetupActivityFragment extends Fragment implements ServiceConnection, View.OnClickListener{
    public interface FragmentSettings {
        BluetoothDevice getBtDevice();
    }

    // Metawear stuff
    private MetaWearBoard metawear = null;
    private SensorFusionBosch sensorFusion;
    private Settings batteryState;
    private FragmentSettings settings;
    private Led led;

    // Declare UI elements
    Button btExperiment, btTrain, btSettings, btStartExperiment, btChangeSubjectID, btSettingsHome,
            btTrainHome, btExNextLeft, btExNextRight, btGlobalBack, btDelete, btShare, btShare2,
            btGreyBox, btDeviceSubmit, btNextExercise, btSliderSubmit, btDeviceNextL, btDeviceNextR,
            btExFinish, btSettingsToggleBack;

    RelativeLayout rlMainMenu, rlExperiment, rlTrain, rlSettings, rlExImg, rlExNumber, rlExSlider,
            rlExDevice, rlExercisePre, rlExFinish;

    RadioButton rbFemale;
    TextView tvAge, tvStimuli, tvSubjectID, tvTrainScore, tvExId, tvExNum, tvNextExercise,
            tvSliderMin, tvSliderMax;
    GifTextView gifArm, gifWrist, gifSlider;
    SeekBar sbAge, sbStimuli, sbExSlider;
    EditText etSubjectID;
    android.widget.Switch swTrain;

    // Public Variables
    ArrayList<RelativeLayout> layouts = new ArrayList<RelativeLayout>();
    Toast t;
    int nStimuli, subjectID, currentStimuli, currentStimuliOrder, currentType, globalBack;
    SharedPreferences mPrefs;
    SharedPreferences.Editor mEditor;
    String[] subjectData = {"","",""};

    // Order for mixed experiments
    int[][] order = new int[][]{
            { 0, 1, 5, 2, 4, 3 },
            { 1, 2, 0, 3, 5, 4 },
            { 2, 3, 1, 4, 0, 5 },
            { 3, 4, 2, 5, 1, 0 },
            { 4, 5, 3, 0, 2, 1 },
            { 5, 0, 4, 1, 3, 2 }};
    ArrayList<Integer> thisOrder = new ArrayList<>();
    int trialsRan = 0;

    // 50 shades of grey
    String[] greyColors = new String[]{
            "#ffffff", "#f9f9f9", "#f3f3f3", "#ededed", "#e8e8e8",
            "#e2e2e2", "#dcdcdc", "#d6d6d6", "#d1d1d1", "#cbcbcb",
            "#c5c5c5", "#c0c0c0", "#bababa", "#b4b4b4", "#afafaf",
            "#a9a9a9", "#a4a4a4", "#9f9f9e", "#999999", "#949494",
            "#8e8e8e", "#898989", "#848484", "#7f7f7f", "#797979",
            "#747474", "#6f6f6f", "#6a6a6a", "#656565", "#606060",
            "#5b5b5b", "#565656", "#525252", "#4d4d4d", "#484848",
            "#434343", "#3f3f3f", "#3a3a3a", "#363636", "#313131",
            "#2d2d2d", "#282828", "#242424", "#202020", "#1c1c1c",
            "#181818", "#141414", "#0e0e0e", "#070707", "#000000"};

    ArrayList<Integer> greyStimuli = new ArrayList<>();
    ArrayList<Integer> numStimuli = new ArrayList<>();

    double scaleValueHand, scaleValueArm;

    final short samples = 1;
    private short buzzTime = 200;
    dataVariable rawData = new dataVariable();
    String stateNow;
    Boolean lrSlider = true, sliderReady = true, lrDevice = true, deviceReady = true,
            batterReay = false;
    long time0, timeExStart;

    public DeviceSetupActivityFragment() {
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind UI elements
        rlMainMenu = (RelativeLayout) view.findViewById(R.id.rl_main_menu);
        rlExperiment = (RelativeLayout) view.findViewById(R.id.rl_experiment);
        rlTrain = (RelativeLayout) view.findViewById(R.id.rl_train);
        rlSettings = (RelativeLayout) view.findViewById(R.id.rl_settings);
        rlExImg = (RelativeLayout) view.findViewById(R.id.rl_ex_image);
        rlExNumber = (RelativeLayout) view.findViewById(R.id.rl_ex_number);
        rlExSlider = (RelativeLayout) view.findViewById(R.id.rl_ex_slider);
        rlExDevice = (RelativeLayout) view.findViewById(R.id.rl_ex_device);
        rlExercisePre = (RelativeLayout) view.findViewById(R.id.rl_ex_pre);
        rlExFinish = (RelativeLayout) view.findViewById(R.id.rl_ex_finish);

        btExperiment = (Button) view.findViewById(R.id.bt_experiment);
        btTrain = (Button) view.findViewById(R.id.bt_train);
        btSettings = (Button) view.findViewById(R.id.bt_settings);
        btStartExperiment = (Button) view.findViewById(R.id.bt_start_experiment);
        btChangeSubjectID = (Button) view.findViewById(R.id.bt_subject_ID);
        btSettingsHome = (Button) view.findViewById(R.id.bt_setting_home);
        btTrainHome = (Button) view.findViewById(R.id.bt_train_home);
        btExNextLeft = (Button) view.findViewById(R.id.bt_next_l);
        btExNextRight = (Button) view.findViewById(R.id.bt_next_r);
        btGlobalBack = (Button) view.findViewById(R.id.bt_global_back);
        btDelete = (Button) view.findViewById(R.id.bt_delete);
        btShare = (Button) view.findViewById(R.id.bt_share);
        btShare2 = (Button) view.findViewById(R.id.bt_share2);
        btGreyBox = (Button) view.findViewById(R.id.bt_grey_box);
        btDeviceSubmit = (Button) view.findViewById(R.id.bt_device_submit);
        btNextExercise = (Button) view.findViewById(R.id.bt_next_ex);
        btSliderSubmit = (Button) view.findViewById(R.id.bt_slider_submit);
        btDeviceNextL = (Button) view.findViewById(R.id.bt_device_next_l);
        btDeviceNextR = (Button) view.findViewById(R.id.bt_device_next_r);
        btExFinish = (Button) view.findViewById(R.id.bt_ex_finish);
        btSettingsToggleBack = (Button) view.findViewById(R.id.bt_settings_toogle_back);

        tvAge = (TextView) view.findViewById(R.id.tv_age);
        tvStimuli = (TextView) view.findViewById(R.id.tv_stimuli);
        tvSubjectID = (TextView) view.findViewById(R.id.tv_subject_ID);
        tvTrainScore = (TextView) view.findViewById(R.id.tv_train_score);
        tvExId = (TextView) view.findViewById(R.id.tv_ex_id);
        tvExNum = (TextView) view.findViewById(R.id.tv_ex_number);
        tvNextExercise = (TextView) view.findViewById(R.id.tv_next_ex);
        tvSliderMin = (TextView) view.findViewById(R.id.tv_slider_min);
        tvSliderMax = (TextView) view.findViewById(R.id.tv_slider_max);

        sbAge = (SeekBar) view.findViewById(R.id.sb_age);
        sbStimuli = (SeekBar) view.findViewById(R.id.sb_stimuli);
        sbExSlider = (SeekBar) view.findViewById(R.id.sb_ex_slider);

        etSubjectID = (EditText) view.findViewById(R.id.et_subject_ID);
        rbFemale = (RadioButton) view.findViewById(R.id.rb_female);
        swTrain = (android.widget.Switch) view.findViewById(R.id.sw_hand_arm);
        gifArm = (GifTextView) view.findViewById(R.id.gif_arm);
        gifWrist = (GifTextView) view.findViewById(R.id.gif_wrist);
        gifSlider = (GifTextView) view.findViewById(R.id.gif_slider);

        // Add layouts to array
        layouts.add(rlMainMenu);
        layouts.add(rlExperiment);
        layouts.add(rlTrain);
        layouts.add(rlSettings);
        layouts.add(rlExImg);
        layouts.add(rlExNumber);
        layouts.add(rlExSlider);
        layouts.add(rlExDevice);
        layouts.add(rlExercisePre);
        layouts.add(rlExFinish);

        // Add listener to menu buttons
        btExperiment.setOnClickListener(this);
        btTrain.setOnClickListener(this);
        btSettings.setOnClickListener(this);
        btStartExperiment.setOnClickListener(this);
        btChangeSubjectID.setOnClickListener(this);
        btSettingsHome.setOnClickListener(this);
        btTrainHome.setOnClickListener(this);
        btGlobalBack.setOnClickListener(this);
        btExNextLeft.setOnClickListener(this);
        btExNextRight.setOnClickListener(this);
        btDelete.setOnClickListener(this);
        btShare.setOnClickListener(this);
        btShare2.setOnClickListener(this);
        btDeviceSubmit.setOnClickListener(this);
        btNextExercise.setOnClickListener(this);
        btSliderSubmit.setOnClickListener(this);
        btDeviceNextL.setOnClickListener(this);
        btDeviceNextR.setOnClickListener(this);
        btExFinish.setOnClickListener(this);
        btSettingsToggleBack.setOnClickListener(this);


        final Context con = this.getContext().getApplicationContext();
        mPrefs = con.getSharedPreferences("label", 0);
        mEditor = mPrefs.edit();
        nStimuli = mPrefs.getInt("stimuli",50);
        globalBack = mPrefs.getInt("globalBack",-1);
        subjectID = mPrefs.getInt("subjectID",-1);

        checkHeader();

        btDeviceSubmit.setVisibility(View.INVISIBLE);
        btDeviceNextL.setVisibility(View.INVISIBLE);
        btDeviceNextR.setVisibility(View.INVISIBLE);
        btExNextLeft.setVisibility(View.INVISIBLE);
        btExNextRight.setVisibility(View.INVISIBLE);
        btSliderSubmit.setVisibility(View.INVISIBLE);
        tvNextExercise.setVisibility(View.INVISIBLE);



        if(globalBack == 1){
            btGlobalBack.setVisibility(View.VISIBLE);
        }else{
            btGlobalBack.setVisibility(View.GONE);
        }

        mainMenuAction(new int[]{-1});
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);

        sbAge.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                tvAge.setText("Age: "+i);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        sbStimuli.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                nStimuli = i*5;
                tvStimuli.setText("Number of stimuli: "+nStimuli);
                mEditor.putInt("stimuli", nStimuli).commit();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        sbExSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(sliderReady) {
                    sbExSlider.setThumb(getResources().getDrawable(R.drawable.blue_thumb_v2));
                    btSliderSubmit.setVisibility(View.VISIBLE);
                    sliderReady = false;
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        rawData.setListener(new dataVariable.ChangeListener() {
            @Override
            public void onChange() {
                dataIn(stateNow);
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_experiment:
                tvExId.setText("ID: "+Integer.toString(subjectID));
                mainMenuAction(new int[]{1});
                break;

            case R.id.bt_train:
                mainMenuAction(new int[]{2});
                stateNow = "train";
                startTrain();
                break;

            case R.id.bt_settings:
                tvStimuli.setText("Number of stimuli: "+nStimuli);
                tvSubjectID.setText("Next Subject ID: "+subjectID);
                sbStimuli.setProgress(nStimuli/5);
                mainMenuAction(new int[]{3});
                break;

            case R.id.bt_start_experiment:
                timeExStart = System.currentTimeMillis();
                startExperiment();
                break;

            case R.id.bt_subject_ID:
                changeSubjectID();
                break;

            case R.id.bt_next_l:
            case R.id.bt_next_r:
                btExNextLeft.setVisibility(View.INVISIBLE);
                btExNextRight.setVisibility(View.INVISIBLE);
                sliderReady = true;
                nextTrial();
                btGreyBox.setVisibility(View.VISIBLE);
                tvExNum.setVisibility(View.VISIBLE);
                sbExSlider.setVisibility(View.VISIBLE);
                tvSliderMin.setVisibility(View.VISIBLE);
                tvSliderMax.setVisibility(View.VISIBLE);
                break;

            case R.id.bt_slider_submit:
                sbExSlider.setThumb(getResources().getDrawable(android.R.color.transparent));
                btSliderSubmit.setVisibility(View.INVISIBLE);
                btGreyBox.setVisibility(View.INVISIBLE);
                tvExNum.setVisibility(View.INVISIBLE);
                sbExSlider.setVisibility(View.GONE);
                tvSliderMin.setVisibility(View.GONE);
                tvSliderMax.setVisibility(View.GONE);
                if(lrSlider){
                    btExNextLeft.setVisibility(View.VISIBLE);
                }else{
                    btExNextRight.setVisibility(View.VISIBLE);
                }
                lrSlider = !lrSlider;
                saveTrial();
                break;

            case R.id.bt_device_next_l:
            case R.id.bt_device_next_r:
                btDeviceNextL.setVisibility(View.INVISIBLE);
                btDeviceNextR.setVisibility(View.INVISIBLE);
                nextTrial();
                btGreyBox.setVisibility(View.VISIBLE);
                tvExNum.setVisibility(View.VISIBLE);
                deviceReady = true;
                break;

            case R.id.bt_device_submit:
                btDeviceSubmit.setVisibility(View.INVISIBLE);
                btGreyBox.setVisibility(View.INVISIBLE);
                tvExNum.setVisibility(View.INVISIBLE);
                if(lrDevice){
                    btDeviceNextL.setVisibility(View.VISIBLE);
                }else{
                    btDeviceNextR.setVisibility(View.VISIBLE);
                }
                lrDevice = !lrDevice;
                saveTrial();
                break;

            case R.id.bt_delete:
                confirmDialogDelete(getContext());
                break;

            case R.id.bt_share:
                shareData(getContext());
                break;

            case R.id.bt_share2:
                moveData(getContext());
                break;

            case R.id.bt_next_ex:
                sbExSlider.setVisibility(View.VISIBLE);
                tvSliderMin.setVisibility(View.VISIBLE);
                tvSliderMax.setVisibility(View.VISIBLE);
                tvExNum.setVisibility(View.VISIBLE);
                btGreyBox.setVisibility(View.VISIBLE);
                btDeviceSubmit.setVisibility(View.INVISIBLE);
                btDeviceNextL.setVisibility(View.INVISIBLE);
                btDeviceNextR.setVisibility(View.INVISIBLE);
                btExNextRight.setVisibility(View.INVISIBLE);
                btExNextLeft.setVisibility(View.INVISIBLE);
                btSliderSubmit.setVisibility(View.INVISIBLE);
                deviceReady = true;
                sliderReady = true;
                startNextExercise();
                break;

            case R.id.bt_settings_toogle_back:
                print(globalBack);
                if(globalBack == 1){
                    btGlobalBack.setVisibility(View.GONE);
                    globalBack = 0;
                    mEditor.putInt("globalBack",0).commit();

                }else{
                    btGlobalBack.setVisibility(View.VISIBLE);
                    globalBack = 1;
                    mEditor.putInt("globalBack",1).commit();
                }
                print(globalBack);
                print(mPrefs.getInt("globalBack",-1));
                break;

            default:
                mainMenuAction(new int[]{-1});
                tvNextExercise.setVisibility(View.INVISIBLE);
                clearMacro();
                break;
        }
    }

    private void dataIn(String stateNow) {
        EulerAngles euler = rawData.isData().value(EulerAngles.class);
        scaleValueHand = (Math.round((Math.abs(euler.roll())/9.0)*100.0)/100.0);
        if(Math.abs(euler.pitch())>90){
            scaleValueArm = (Math.round((90.0-(Math.abs(euler.pitch())-90.0))/9.0 *100.0)/100.0);
        }else{
            scaleValueArm = (Math.round((Math.abs(euler.pitch())/9.0)*100.0)/100.0);
        }

        switch (stateNow){
            case "train":
                String tmpString;
                if(swTrain.isChecked()){
                    tmpString = "Score:\n"+scaleValueArm;
                }else{
                    tmpString = "Score:\n"+scaleValueHand;
                }
                Handler mainHandler = new Handler(getContext().getMainLooper());
                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        tvTrainScore.setText(tmpString);
                    }
                };
                mainHandler.post(myRunnable);
                break;

            case "wrist_grey":
            case "wrist_num":
            case "arm_grey":
            case "arm_num":
                toast("Button pressed");
                Handler mainHandler2 = new Handler(getContext().getMainLooper());
                Runnable myRunnable2 = new Runnable() {
                    @Override
                    public void run() {
                        if(deviceReady) {
                            btDeviceSubmit.setVisibility(View.VISIBLE);
                            deviceReady = false;
                        }
                    }
                };
                mainHandler2.post(myRunnable2);
                break;

            default:
                toast("Unused data");
                break;

        }
    }



    private void startTrain() {
        saveMacro();
    }

    public void changeSubjectID(){
        if(!etSubjectID.getText().toString().equals("")){
            subjectID = Integer.parseInt(etSubjectID.getText().toString());
            mEditor.putInt("subjectID", subjectID).commit();
            tvSubjectID.setText("Next Subject ID: "+subjectID);
        }else{
            toast("Insert value");
        }
    };

    public void tmpButton(View v){
        mainMenuAction(new int[]{-1});
    }

    public void mainMenuAction(int[] id){

        for(int i = 0; i<layouts.size(); i++){
            layouts.get(i).setVisibility(View.GONE);
        }

        for(int i = 0; i<id.length;i++){
            if(id[i] <= layouts.size() && id[i] >= 0){
                layouts.get(id[i]).setVisibility(View.VISIBLE);
            }else{
                rlMainMenu.setVisibility(View.VISIBLE);
                tvNextExercise.setVisibility(View.INVISIBLE);
            }
        }

        if(id[0] == 0 || id[0] == 3 || id[0] == -1 && batterReay){
            readBattery();
        }


    }

    public void readBattery(){
        batteryState.battery().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.stream(new Subscriber() {
                    @Override
                    public void apply(Data data, Object ... env) {
                        toast("Battery: "+data.value(Settings.BatteryState.class).charge+"%");
                    }
                });
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {
                batteryState.battery().read();
                return null;
            }
        });
    }

    public void startExperiment(){
        String gender;
        if(rbFemale.isChecked()){
            gender = "Female";
        }else{
            gender = "Male";
        }
        toast("Start Experiement: "+gender+" "+sbAge.getProgress());
        subjectData[0] = Integer.toString(subjectID);
        mEditor.putInt("subjectID", subjectID).commit();
        subjectData[1] = gender;
        subjectData[2] = Integer.toString(sbAge.getProgress());

        thisOrder = new ArrayList<>();
        for(int i=0; i<6; i++){
            thisOrder.add(order[subjectID%6][i]);
        }
        String tmpS = Integer.toString(thisOrder.get(0));
        for(int i=1;i<6;i++){
            tmpS += ","+Integer.toString(thisOrder.get(i));
        }
        subjectID++;
        mEditor.putInt("subjectID", subjectID).commit();

        //mainMenuAction(new int[]{4,6});
        nextExercise();
    }

    public void nextExercise(){
        trialsRan = 0;
        clearMacro();
        int nextEx = -1;
        if(thisOrder.size()==0){
            stateNow = "finished";
            tvNextExercise.setVisibility(View.INVISIBLE);
            if(!subjectData[2].equals("0")){
                long timeExTotal = System.currentTimeMillis() - timeExStart;
                Date currentTime = Calendar.getInstance().getTime();
                String subject = subjectData[0]+","+subjectData[1]+","+subjectData[2];
                //"id,gender,age,time,number_of_stimuli_per_exercise,time_to_complete\n",
                saveData(subject+","+currentTime+","+nStimuli+","+timeExTotal/60000.0+"\n",
                        "timeExComplete.csv");
            }
            mainMenuAction(new int[]{9});
            return;
        }else{
            nextEx = thisOrder.get(0);
            currentType = nextEx;
            thisOrder.remove(0);
        }


        switch (nextEx){
            case 0:// slider grey
                stateNow = "slider_grey";
                tvNextExercise.setText("Adjust the slider to fit the color on screen");
                break;
            case 1:// slider num
                stateNow = "slider_num";
                tvNextExercise.setText("Adjust the slider to fit the number on screen");
                break;
            case 2:// wrist grey
                stateNow = "wrist_grey";
                tvNextExercise.setText("Turn your wrist to fit the color on screen");
                break;
            case 3:// wrist num
                stateNow = "wrist_num";
                tvNextExercise.setText("Turn your wrist to fit the number on screen");
                break;
            case 4:// arm grey
                stateNow = "arm_grey";
                tvNextExercise.setText("Elevate your arm to fit the color on screen");
                break;
            case 5:// arm num
                stateNow = "arm_num";
                tvNextExercise.setText("Elevate your arm to fit the number on screen");
                break;

            default:
                toast("Error: Next exercise");
                break;

        }

        if(stateNow.contains("arm")){
            gifArm.setVisibility(View.VISIBLE);
            gifWrist.setVisibility(View.GONE);
            gifSlider.setVisibility(View.GONE);
        }else if(stateNow.contains("wrist")){
            gifArm.setVisibility(View.GONE);
            gifWrist.setVisibility(View.VISIBLE);
            gifSlider.setVisibility(View.GONE);
        }else{
            gifArm.setVisibility(View.GONE);
            gifWrist.setVisibility(View.GONE);
            gifSlider.setVisibility(View.VISIBLE);
        }

        mainMenuAction(new int[]{8});
        tvNextExercise.setVisibility(View.VISIBLE);

    }

    public void startNextExercise(){
        switch (stateNow){
            case "slider_grey":// slider grey
                setStimuli();
                mainMenuAction(new int[]{4,6});
                break;
            case "slider_num":// slider num
                setStimuli();
                mainMenuAction(new int[]{5,6});
                break;
            case "wrist_grey":// wrist grey
                setStimuli();
                saveMacro();
                mainMenuAction(new int[]{4,7});
                break;
            case "wrist_num":// wrist num
                setStimuli();
                saveMacro();
                mainMenuAction(new int[]{5,7});
                break;
            case "arm_grey":// arm grey
                setStimuli();
                saveMacro();
                mainMenuAction(new int[]{4,7});
                break;
            case "arm_num":// arm num
                setStimuli();
                saveMacro();
                mainMenuAction(new int[]{5,7});
                break;

            default:
                toast("Error: Next exercise");
                break;

        }

    }

    // 0 = grey 1 = num
    private int nextStimuli(int c){
        int r = -1;
        switch (c){
            case 0:
                if(greyStimuli.size()==0){
                    for(int i=0; i<50; i++){
                        greyStimuli.add(i);
                    }
                    // Randomize the stimuli
                    Collections.shuffle(greyStimuli);
                }
                r = greyStimuli.get(0);
                currentStimuliOrder = 50 - greyStimuli.size();
                greyStimuli.remove(0);
                break;
            case 1:
                if(numStimuli.size()==0){
                    for(int i=0; i<101; i++){
                        numStimuli.add(i);
                    }
                    // Randomize the stimuli
                    Collections.shuffle(numStimuli);
                }
                r = numStimuli.get(0);
                currentStimuliOrder = 101 - numStimuli.size();
                numStimuli.remove(0);
                return r;
            default:
                toast("next stimuli error");
                break;
        }
        return r;
    }

    private void setStimuli(){
        time0 = System.currentTimeMillis();
        Handler mainHandler = new Handler(getContext().getMainLooper());
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                if(stateNow.contains("grey")){
                    currentStimuli = getStimuli();
                    btGreyBox.setBackgroundColor(Color.parseColor(greyColors[currentStimuli]));
                }else if(stateNow.contains("num")){
                    currentStimuli = getStimuli();
                    tvExNum.setText(Integer.toString(currentStimuli));
                }
            }
        };
        mainHandler.post(myRunnable);
    }

    private int getStimuli(){
        String fileName = stateNow+".csv";
        ArrayList<Integer> s = new ArrayList<>();
        int r = -1;
        final Context con = this.getContext().getApplicationContext();

        // Check if file already exist
        File file = new File(con.getFilesDir()+"/"+fileName);
        if(!file.exists()) {
            // Create new stimuli
            s = newStimuli();
            r = s.get(0);
            if(stateNow.contains("num")){
                currentStimuliOrder = 101 - s.size();
            }else{
                currentStimuliOrder = 50 - s.size();
            }
            s.remove(0);

        // Read file
        }else{
            try {
                // Open file
                FileInputStream fis = con.openFileInput(fileName);
                InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(isr);
                StringBuilder sb = new StringBuilder();
                String line;
                // Create an array list to store the strings so the order can be reveresed later
                ArrayList<String> readData = new ArrayList<String>();
                String thisColor, prettyTime, prettyScale;

                // Read all lines in "data.csv"
                while ((line = bufferedReader.readLine()) != null) {
                    // Split each line
                    String[] parts = line.split(",");
                    r = Integer.parseInt(parts[0]);
                    if(stateNow.contains("num")){
                        currentStimuliOrder = 101 - parts.length;
                    }else{
                        currentStimuliOrder = 50 - parts.length;
                    }
                    if(parts.length == 1){
                        s = newStimuli();
                    }else{
                        for(int i = 1; i<parts.length;i++){
                            s.add(Integer.parseInt(parts[i]));
                        }
                    }
                }
                // catch exceptions
            } catch (FileNotFoundException e) {

            } catch (UnsupportedEncodingException e) {

            } catch (IOException e) {

            }
        }

        // Save stimuli to file
        String data = Integer.toString(s.get(0));
        for(int i=1; i<s.size();i++){
            data += ","+Integer.toString(s.get(i));
        }

        FileOutputStream outputStream;
        try {
            outputStream = this.getContext().openFileOutput(fileName, 0);
            outputStream.write(data.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            // In case of error, let the user now
            toast("Error: saving stimuli");
        }

        // return one stimuli
        return r;
    }

    private ArrayList<Integer> newStimuli(){
        ArrayList<Integer> r = new ArrayList<>();
        if(stateNow.contains("num")){
            for(int i = 0; i<101; i++){
                r.add(i);
            }
            Collections.shuffle(r);
        }else{
            for(int i = 0; i<50; i++){
                r.add(i);
            }
            Collections.shuffle(r);
        }
        return r;


    }

    private void nextTrial() {
        trialsRan++;
        if(trialsRan<nStimuli){
            setStimuli();
        }else{
            nextExercise();
        }
    }




    public void print(Object s){
        System.out.println("###");
        System.out.println(s);
        System.out.println("###");
    }

    public void toast(String txt){
        Handler mainHandler = new Handler(getContext().getMainLooper());
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                // Try to cancel previous toast, if any
                try{
                    t.cancel();
                }catch (Exception e){
                    // Do nothing
                }
                // Create Toast dispaying the string txt
                t = Toast.makeText(getActivity(), txt,Toast.LENGTH_SHORT);
                //this, txt, Toast.LENGTH_SHORT);
                t.show();
            }
        };
        mainHandler.post(myRunnable);


    }

    // Permissions to read/write external storage
    public static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;

    void getWritePermission(){
        if (ContextCompat.checkSelfPermission(this.getContext().getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(DeviceSetupActivityFragment.this.getActivity(),
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant. The callback method gets the
            // result of the request.

        }
    }
    void getReadPermission(){
        if (ContextCompat.checkSelfPermission(this.getContext().getApplicationContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(DeviceSetupActivityFragment.this.getActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant. The callback method gets the
            // result of the request.
        }
    }

    // This method is a modified version of what can be found here: https://developer.android.com/training/permissions/requesting.html
    void checkPermission(){
        getWritePermission();
        getReadPermission();
    }


    // Method to create a confirm dialog when deleting mood history
    // This method is an altered version of this: http://stackoverflow.com/questions/2478517/how-to-display-a-yes-no-dialog-box-on-android
    public void confirmDialogDelete(Context context) {
        // Create Alert Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());

        // Title and message
        builder.setTitle("Delete Data");
        builder.setMessage("Are you sure? This action can't be undone");

        // When "YES" is chosen, delete mood history and disable buttons
        builder.setPositiveButton("DELETE", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {

                context.deleteFile("data.csv");
                context.deleteFile("slider_grey.csv");
                context.deleteFile("slider_num.csv");
                context.deleteFile("wrist_grey.csv");
                context.deleteFile("wrist_num.csv");
                context.deleteFile("arm_grey.csv");
                context.deleteFile("arm_num.csv");
                checkHeader();
                toast("Data deleted");
                /*
                checkHeader();
                tvShowDownload.setText("");
                checkShare(true);
                */
                dialog.dismiss();
            }
        });

        // When "NO" is chosen, do nothing
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    public void moveData(Context context){
        checkPermission();
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File destinationFile = new File(dir, "ExperimentData.csv");
        File sourceFile = new File(context.getFilesDir()+"/"+"data.csv");

        // Copy internal "data.csv" to external "ExperimentData.csv"
        try{
            copyFile(sourceFile,destinationFile);
            toast("Data moved to \"Downloads\"");
        }catch (IOException e){
            e.printStackTrace();
            toast("Error: moving data1");
        }

        File destinationFile2 = new File(dir, "completionTime.csv");
        File sourceFile2 = new File(context.getFilesDir()+"/"+"timeExComplete.csv");
        try{
            copyFile(sourceFile2,destinationFile2);
            toast("Data moved to \"Downloads\"");
        }catch (IOException e){
            e.printStackTrace();
            toast("Error: moving data2");
            print(e);
        }
    }

    public void startVideo(String fileName){

    }

    public void shareData(Context context){
        moveData(context);
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File destinationFile = new File(dir, "ExperimentData.csv");

        // Send Mood as email attachement
        Uri uri = Uri.parse("mailto:");
        ArrayList<Uri> uris = new ArrayList<Uri>();
        uris.add(Uri.fromFile(destinationFile));
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, uri);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Experiment Data");
        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(destinationFile));
        startActivity(emailIntent);

    }

    // Method to copy file from internal to external storage (and vise versa)
    // This method is taken from http://stackoverflow.com/questions/4770004/how-to-move-rename-file-from-internal-app-storage-to-external-storage-on-android
    public void copyFile(File src, File dst) throws IOException {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try
        {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        }
        finally
        {
            if (inChannel != null)
                inChannel.close();
            if (outChannel != null)
                outChannel.close();
        }
    }

    public void clearMacro(){
        metawear.tearDown();
        sensorFusion.eulerAngles().stop();
        sensorFusion.stop();
    }

    public boolean createDataStream(){
        // Stream eulerAngles from the board
        sensorFusion.eulerAngles().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.limit(Passthrough.COUNT, (short) 0).name("euler_passthrough").stream(new Subscriber() {
                    @Override
                    public void apply(Data data, Object... env) {
                        rawData.setData(data);
                    }
                });
            }
        });
        sensorFusion.eulerAngles().start();
        sensorFusion.start();
        return true;
    }

    public void saveMacro() {
        clearMacro();
        boolean tmp = createDataStream();
        if (tmp) {
            metawear.getModule(Switch.class).state().addRouteAsync(new RouteBuilder() {
                @Override
                public void configure(RouteComponent source) {
                    source.filter(Comparison.NEQ, 0).react(new RouteComponent.Action() {
                        @Override
                        public void execute(DataToken token) {
                            led.editPattern(Led.Color.BLUE, Led.PatternPreset.SOLID).commit();
                            led.play();
                            final DataProcessor dataPross = metawear.getModule(DataProcessor.class);
                            dataPross.edit("euler_passthrough", DataProcessor.PassthroughEditor.class).set(samples);
                            metawear.getModule(Haptic.class).startBuzzer(buzzTime);
                        }
                    });
                }
            }).onSuccessTask(new Continuation<Route, Task<Route>>() {
                @Override
                public Task<Route> then(Task<Route> task) throws Exception {
                    return metawear.getModule(Switch.class).state().addRouteAsync(new RouteBuilder() {
                        @Override
                        public void configure(RouteComponent source) {
                            source.filter(Comparison.NEQ, 1).react(new RouteComponent.Action() {
                                @Override
                                public void execute(DataToken token) {
                                    led.stop(true);
                                }
                            });
                        }
                    });
                }
            });
        }else{
            toast("Error saving macro");
        }
    }


    public void checkHeader() {
        // Add column titles if "data.csv" doesn't exist
        final Context con = this.getContext().getApplicationContext();
        File file = new File(con.getFilesDir()+"/"+"data.csv");
        if(!file.exists()) {
            saveData("id,gender,age,time,type,type_order,stim_order,stim,response,pitch,roll,yaw,reaction_time\n",
                    "data.csv");
        }
        File file2 = new File(con.getFilesDir()+"/"+"timeExComplete.csv");
        if(!file2.exists()) {
            saveData("id,gender,age,time,number_of_stimuli_per_exercise,time_to_complete\n",
                    "timeExComplete.csv");
        }
    }

    // Method to save a string to file
    public void saveData(String data, String filename){
        // Save mood and date to "mood.csv"
        FileOutputStream outputStream;

        try {
            outputStream = this.getContext().openFileOutput(filename, Context.MODE_APPEND);
            outputStream.write(data.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            // In case of error, let the user now
            toast("Data lost..");
        }

    }

    String [] type = {"slider_grey","slider_num","wrist_grey","wrist_num","arm_grey","arm_num"};
    public void saveTrial(){
        String subject = subjectData[0]+","+subjectData[1]+","+subjectData[2];
        String response, deviceVal;
        Date currentTime = Calendar.getInstance().getTime();
        long reactionTime = System.currentTimeMillis() - time0;


        switch (stateNow){
            case "wrist_grey":
            case "wrist_num":
            case "arm_grey":
            case "arm_num":
                if(stateNow.contains("wrist")){
                    if(stateNow.contains("num")){
                        response = String.valueOf(scaleValueHand*10.0);
                    }else{
                        response = String.valueOf(scaleValueHand*4.9);
                    }

                }else{
                    if(stateNow.contains("num")){
                        response = String.valueOf(scaleValueArm*10.0);
                    }else{
                        response = String.valueOf(scaleValueArm*4.9);
                    }

                }
                EulerAngles euler = rawData.isData().value(EulerAngles.class);
                deviceVal = euler.pitch()+","+euler.roll()+","+euler.yaw();
                break;

            default:
                deviceVal = "NA,NA,NA";
                if(stateNow.contains("num")){
                    response = Double.toString(sbExSlider.getProgress()/100.0);
                }else {
                    response = Double.toString(sbExSlider.getProgress()/((1000/4.9)));
                }
                break;
        }

        String tmpR = subject+","+currentTime.toString()+","+type[currentType]+","+Integer.toString(5-(thisOrder.size()))+","+Integer.toString(currentStimuliOrder)+","
                +Integer.toString(currentStimuli)+","+response+","+deviceVal+","+Long.toString(reactionTime)+"\n";
        print(tmpR);
        saveData(tmpR,"data.csv");


    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Activity owner= getActivity();
        if (!(owner instanceof FragmentSettings)) {
            throw new ClassCastException("Owning activity must implement the FragmentSettings interface");
        }

        settings= (FragmentSettings) owner;
        owner.getApplicationContext().bindService(new Intent(owner, BtleService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ///< Unbind the service when the activity is destroyed
        getActivity().getApplicationContext().unbindService(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        return inflater.inflate(R.layout.fragment_device_setup, container, false);
    }
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        metawear = ((BtleService.LocalBinder) service).getMetaWearBoard(settings.getBtDevice());
        led = metawear.getModule(Led.class);
        sensorFusion = metawear.getModule(SensorFusionBosch.class);
        batteryState = metawear.getModule(Settings.class);
        sensorFusion.configure()
                .mode(Mode.NDOF)
                .commit();
        clearMacro();
        batterReay = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {}
    /**
     * Called when the app has reconnected to the board
     */
    public void reconnected() { }
}