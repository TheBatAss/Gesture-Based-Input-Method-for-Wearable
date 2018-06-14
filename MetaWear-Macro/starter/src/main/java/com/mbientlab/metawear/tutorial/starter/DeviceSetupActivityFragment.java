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
import android.text.Html;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.mbientlab.metawear.*;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.builder.*;
import com.mbientlab.metawear.builder.filter.*;
import com.mbientlab.metawear.module.*;
import com.mbientlab.metawear.data.EulerAngles;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import bolts.Continuation;
import bolts.Task;


/**
 * A placeholder fragment containing a simple view.
 */
public class DeviceSetupActivityFragment extends Fragment implements ServiceConnection {
    public interface FragmentSettings {
        BluetoothDevice getBtDevice();
    }

    private MetaWearBoard metawear = null;
    private SensorFusionBosch sensorFusion;
    private Settings batteryState;
    private FragmentSettings settings;
    private Led led;
    private Logging logging;

    Button btDownloadData, btClearData, btShare;
    android.widget.Switch swTime;
    ScrollView svShowEuler;
    TextView tvShowDownload;

    SharedPreferences mPrefs;
    SharedPreferences.Editor mEditor;
    boolean switchState;
    final short samples = 1;
    String string = "";
    Toast t;
    private short buzzTime = 200;
    Led.Color LEDColor;
    String nanValue = "NA";
    List<Data> batteryData = new ArrayList<Data>();
    List<Data> fusionData = new ArrayList<Data>();

    public DeviceSetupActivityFragment() {
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind UI elements
        btDownloadData = (Button) view.findViewById(R.id.bt_download);
        btClearData = (Button) view.findViewById(R.id.bt_clear_data);
        btShare = (Button) view.findViewById(R.id.bt_share);
        svShowEuler = (ScrollView) view.findViewById(R.id.sv_show_euler);
        tvShowDownload = (TextView) view.findViewById(R.id.tv_show_data);
        swTime = (android.widget.Switch) view.findViewById(R.id.sw_time);


        final Context con = this.getContext().getApplicationContext();
        read_file(con, "data.csv");
        checkShare(false);

        mPrefs = con.getSharedPreferences("label", 0);
        mEditor = mPrefs.edit();
        switchState = mPrefs.getBoolean("switch",false);
        swTime.setChecked(switchState);

        checkHeader();

        // Delete Data
        btClearData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDialog(con);

            }
        });

        // Get data from device
        btDownloadData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toast("Import started");
                downloadData();
            }
        });

        // Share Data
        btShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareData(con);
            }
        });

        swTime.setOnCheckedChangeListener((buttonView, isChecked) -> {
            downloadData();

            // Disable switch, wait 3 seconds and enable it again
            swTime.setEnabled(false);
            new CountDownTimer(3000, 1000) {
                public void onTick(long millisUntilFinished) {}
                public void onFinish() {
                    swTime.setEnabled(true);
                }
            }.start();
        });
    }

    public void toast(String txt){
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

    // Method to read "mood.csv" from internal storage
    // This is a heavily edited version of what can be found here: http://stackoverflow.com/questions/14768191/how-do-i-read-the-file-content-from-the-internal-storage-android-app
    public String read_file(Context context, String filename) {
        try {
            // Open file
            FileInputStream fis = context.openFileInput(filename);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            // Colors used to color the different moods
            String[] colors = {"#f8696b","#f98370","#fa9d75","#fcb67a","#fdd180",
                    "#ffeb83","#e0e383","#c1da82","#a2d07f","#83c77d","#63be7b"};

            // Create an array list to store the strings so the order can be reveresed later
            List<String> readData = new ArrayList<String>();
            String thisColor, prettyTime, prettyScale;

            // Read all lines in "data.csv"
            while ((line = bufferedReader.readLine()) != null) {
                // Split each line
                String[] parts = line.split(",");
                try {
                    if(!parts[0].equals("Score (0-10 or NA)")){
                        if (parts[0].equals(nanValue)) {
                            thisColor = "#0099ff";
                            prettyScale = "--";
                        } else {
                            // Get the color for the corresponding mood
                            thisColor = colors[10 - (int) Double.parseDouble(parts[0])];
                            prettyScale = String.format(Locale.ROOT,"%.2f",Double.parseDouble(parts[0]));
                        }

                        prettyTime = parts[1].substring(0,parts[1].length()-9).replace("T"," ");

                        // Use HTML syntax to color and change size of the mood on each line
                        String colorString = "<p><b><big><big><big><font color=" + thisColor + ">" + prettyScale + "</font></big></big></big></b> " + prettyTime + " Battery: " + parts[2] + "%</p>";//" "+parts[2]+
                        // Add to array list
                        readData.add(colorString);
                    }
                }
                catch (Exception e){
                    Log.i(e.toString(),parts[0]);
                }
            }

            string = "";
            // Reverse the order of lines, to get the newest mood on top, and oldest below
            for(int i = readData.size()-1;i>-1;i--){
                // Add lines to textview
                string += readData.get(i);
            }


            Handler mainHandler = new Handler(getContext().getMainLooper());
            Runnable myRunnable = new Runnable() {
                @Override
                public void run() {
                    tvShowDownload.setText("");
                    tvShowDownload.setText(Html.fromHtml(string));
                    tvShowDownload.scrollTo(0,0);
                    checkShare(false);
                }
            };
            mainHandler.post(myRunnable);


            return "Succes";

            // catch exceptions
        } catch (FileNotFoundException e) {
            return "";
        } catch (UnsupportedEncodingException e) {
            return "";
        } catch (IOException e) {
            return "";
        }
    }

    // Method to create a confirm dialog when deleting mood history
    // This method is an altered version of this: http://stackoverflow.com/questions/2478517/how-to-display-a-yes-no-dialog-box-on-android
    public void confirmDialog(Context context) {
        // Create Alert Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());

        // Title and message
        builder.setTitle("Delete Data");
        builder.setMessage("Are you sure? This action can't be undone");

        // When "YES" is chosen, delete mood history and disable buttons
        builder.setPositiveButton("DELETE", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {

                context.deleteFile("data.csv");
                toast("Data deleted");
                checkHeader();
                tvShowDownload.setText("");
                checkShare(true);
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




    public void shareData(Context context){
        checkPermission();
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File destinationFile = new File(dir, "MetaWearData.csv");
        File sourceFile = new File(context.getFilesDir()+"/"+"data.csv");

        // Copy internal "data.csv" to external "MetaWearData.csv"
        try{
            copyFile(sourceFile,destinationFile);
        }catch (IOException e){
            e.printStackTrace();
        }

        // Send Mood as email attachement
        Uri uri = Uri.parse("mailto:");
        Intent moodEmailIntent = new Intent(Intent.ACTION_SENDTO, uri);
        moodEmailIntent.putExtra(Intent.EXTRA_SUBJECT, "MetaWear Data");
        moodEmailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(destinationFile));
        startActivity(moodEmailIntent);
    }

    // Method to copy file from internal to external storage (and vise versa)
    // This method is taken from http://stackoverflow.com/questions/4770004/how-to-move-rename-file-from-internal-app-storage-to-external-storage-on-android
    public static void copyFile(File src, File dst) throws IOException
    {
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

    // Method to check if there is no data, and then disable the buttons and vise versa
    public void checkShare(boolean disable){
        if(tvShowDownload.getText() == "" || disable){
            btShare.setEnabled(false);
            //btShare.setBackgroundColor(Color.LTGRAY);
            btClearData.setEnabled(false);
            //btClearData.setBackgroundColor(Color.LTGRAY);
        }else{
            btShare.setEnabled(true);
            //btShare.setBackgroundColor(getResources().getColor(R.color.b9));
            btClearData.setEnabled(true);
            //btClearData.setBackgroundColor(getResources().getColor(R.color.b1));
        }
    }

    public void clearMacro(){
        metawear.tearDown();
        sensorFusion.eulerAngles().stop();
        sensorFusion.stop();
    }

    public void saveMacro() {
        clearMacro();
        boolean tmp = createDataLog();
        if(switchState){
            LEDColor = Led.Color.BLUE;
        }else{
            LEDColor = Led.Color.GREEN;
        }

        if (tmp) {
            metawear.getModule(Switch.class).state().addRouteAsync(new RouteBuilder() {
                @Override
                public void configure(RouteComponent source) {
                    source.filter(Comparison.NEQ, 0).react(new RouteComponent.Action() {
                        @Override
                        public void execute(DataToken token) {
                            led.editPattern(LEDColor, Led.PatternPreset.SOLID).commit();
                            led.play();

                            // Log Data
                            logging.start(true);
                            final DataProcessor dataPross = metawear.getModule(DataProcessor.class);
                            dataPross.edit("battery_passthrough", DataProcessor.PassthroughEditor.class).set(samples);
                            dataPross.edit("euler_passthrough", DataProcessor.PassthroughEditor.class).set(samples);
                            batteryState.battery().read();
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
                                    //metawear.getModule(Haptic.class).startBuzzer((short) buzzTime);

                                    // Stop Log
                                    //sensorFusion.eulerAngles().stop();
                                    //sensorFusion.stop();
                                    logging.stop();
                                    metawear.getModule(Haptic.class).startBuzzer((short) buzzTime);
                                }

                            });
                        }
                    });
                }
            });
        }
    }
    public void downloadData(){
        final Context con = this.getContext().getApplicationContext();
        // download log data and send 100 progress updates during the download
        logging.downloadAsync(100, new Logging.LogDownloadUpdateHandler() {
            @Override
            public void receivedUpdate(long nEntriesLeft, long totalEntries) {
                Log.i("MainActivity", "Progress Update = " + nEntriesLeft + "/" + totalEntries);
            }
        }).continueWithTask(new Continuation<Void, Task<Void>>() {
            @Override
            public Task<Void> then(Task<Void> task) throws Exception {
                Log.i("MainActivity", "Download completed");
                logging.clearEntries();
                EulerAngles tmpEuler;
                String niceTime;
                Calendar rawTime;
                String localTime;
                String rawEuler;
                double scaleValue;

                if(fusionData.size()!=0){
                    for(int i=0 ; i<batteryData.size() ; i++){
                        tmpEuler = fusionData.get(i).value(EulerAngles.class);
                        rawEuler = Float.toString(tmpEuler.pitch())+","+Float.toString(tmpEuler.roll())+","+Float.toString(tmpEuler.yaw());
                        rawTime = batteryData.get(i).timestamp();
                        niceTime = batteryData.get(i).formattedTimestamp();
                        DateFormat date = new SimpleDateFormat("Z");
                        localTime = date.format(rawTime.getTime());

                        scaleValue = (double) (Math.round((Math.abs(tmpEuler.roll())/9.0)*100.0)/100.0);
                        saveData(scaleValue+","+niceTime+localTime+","+batteryData.get(i).value(Settings.BatteryState.class).charge+","+rawEuler+"\n");

                    }
                }else{
                    for(int i=0 ; i<batteryData.size() ; i++){
                        rawTime = batteryData.get(i).timestamp();
                        niceTime = batteryData.get(i).formattedTimestamp();
                        DateFormat date = new SimpleDateFormat("Z");
                        localTime = date.format(rawTime.getTime());
                        saveData(nanValue+","+niceTime+localTime+","+batteryData.get(i).value(Settings.BatteryState.class).charge+",NA,NA,NA"+"\n");
                    }}

                batteryData = new ArrayList<Data>();
                fusionData = new ArrayList<Data>();

                checkSwitchState();
                read_file(con, "data.csv");
                return null;
            }
        });
    }

    public void checkSwitchState(){
        boolean newState = swTime.isChecked();
        String tMessage;
        try {
            if (switchState != newState) {
                if (newState) {
                    mEditor.putBoolean("switch", true).commit();
                    tMessage = "Time only";

                } else {
                    mEditor.putBoolean("switch", false).commit();
                    tMessage = "Scale and Time";
                }
                // Update switchState
                switchState = mPrefs.getBoolean("switch", false);
                Handler mainHandler = new Handler(getContext().getMainLooper());
                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        toast(tMessage);
                    }
                };
                mainHandler.post(myRunnable);
                saveMacro();
            }
        }catch (Exception e){
            Log.i(e.toString(),"checkSwitchState error");
        }
    }

    public void checkHeader() {
        // Add column titles if "data.csv" doesn't exist
        final Context con = this.getContext().getApplicationContext();
        File file = new File(con.getFilesDir()+"/"+"data.csv");
        if(!file.exists()) {
            saveData("Score (0-10 or NA),Time (ISO 8601),Battery Charge (%),Pitch (degrees or NA),Roll (degrees or NA),Yaw (degrees or NA)\n");
        }
    }

    // Method to save a data to csv-file.
    public void saveData(String data){
        // Save mood and date to "mood.csv"
        String filename = "data.csv";
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


    public boolean createDataLog(){
        // Log battery State
        batteryState.battery().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.limit(Passthrough.COUNT, samples).name("battery_passthrough").log(new Subscriber() {
                    @Override
                    public void apply(Data data, Object... env) {
                        batteryData.add(data);
                    }
                });
            }
        });

        // log eulerAngles on the board
        sensorFusion.eulerAngles().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.limit(Passthrough.COUNT, samples).name("euler_passthrough").log(new Subscriber() {
                    @Override
                    public void apply(Data data, Object... env) {
                        fusionData.add(data);
                    }
                });
            }
        });

        if(switchState){
            sensorFusion.stop();
        }else{
            sensorFusion.eulerAngles().start();
            sensorFusion.start();
        }
        return true;
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
        logging = metawear.getModule(Logging.class);
        batteryState = metawear.getModule(Settings.class);

        sensorFusion = metawear.getModule(SensorFusionBosch.class);
        sensorFusion.configure()
                .mode(Mode.NDOF)
                .commit();

        saveMacro();

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    /**
     * Called when the app has reconnected to the board
     */
    public void reconnected() { }
}