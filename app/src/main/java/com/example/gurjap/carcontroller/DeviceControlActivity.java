package com.example.gurjap.carcontroller;

import android.annotation.SuppressLint;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.Button;
import android.widget.CompoundButton;

import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;


public final class DeviceControlActivity extends BaseActivity {
    private static final String DEVICE_NAME = "DEVICE_NAME";
    private static final String LOG = "LOG";

    private static final SimpleDateFormat timeformat = new SimpleDateFormat("HH:mm:ss.SSS");

    private static String MSG_NOT_CONNECTED;
    private static String MSG_CONNECTING;
    private static String MSG_CONNECTED;

    private static DeviceConnector connector;
    private static BluetoothResponseHandler mHandler;
    SeekBar speed;
    ToggleButton horn;
    SeekBar direction;
    TextView speed_view,speech_view;
    Button stop_btn;
    private boolean hexMode, checkSum, needClean;
    private boolean show_timings, show_direction;
    private String command_ending;
    private String deviceName;
    String speed_var="00";
    String Direction_var="00";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        PreferenceManager.setDefaultValues(this, R.xml.settings_activity, false);

        if (mHandler == null) mHandler = new BluetoothResponseHandler(this);
        else mHandler.setTarget(this);

        MSG_NOT_CONNECTED = getString(R.string.msg_not_connected);
        MSG_CONNECTING = getString(R.string.msg_connecting);
        MSG_CONNECTED = getString(R.string.msg_connected);

        setContentView(R.layout.activity_terminal);

        speed_view= (TextView) findViewById(R.id.speed_view);
        speed= (SeekBar) findViewById(R.id.speed_seekbar);
        direction= (SeekBar) findViewById(R.id.direction_seekbar);
        horn= (ToggleButton) findViewById(R.id.horn);
        speech_view= (TextView) findViewById(R.id.speech_view);
        stop_btn= (Button) findViewById(R.id.stop_btn);

        stop_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speed.setProgress(100);
                direction.setProgress(100);
                onchange_values();
            }
        });
        speed.setProgress(100);
        direction.setProgress(100);
        speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onchange_values();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                speed.setProgress(100);
                onchange_values();

            }
        });
       /* mic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotopromtmic();
            }
        });*/
        horn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onchange_values();
            }
        });
        direction.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onchange_values();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                direction.setProgress(100);
                onchange_values();
            }
        });

        if (isConnected() && (savedInstanceState != null)) {
            setDeviceName(savedInstanceState.getString(DEVICE_NAME));
        } else getSupportActionBar().setSubtitle(MSG_NOT_CONNECTED);





    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(DEVICE_NAME, deviceName);

    }

    @SuppressLint("DefaultLocale")
    void onchange_values()
    {



        if(speed.getProgress()>100)
        {
            speed_var=String.format("%02d",(speed.getProgress()-100));
            speed_var=speed_var+"F";

        }
        else if(speed.getProgress()<100){

            speed_var=String.format("%02d",(99-speed.getProgress()));

            speed_var=speed_var+"B";


        }
        else if(speed.getProgress()==100){
            speed_var="00";
            speed_var=speed_var+"F";

        }

        if(direction.getProgress()>100)
        {
            Direction_var=String.format("%02d",(direction.getProgress()-100));
            Direction_var=Direction_var+"R";

        }
        else if(direction.getProgress()<100)
        {
            Direction_var=String.format("%02d",(99-direction.getProgress()));
            Direction_var=Direction_var+"L";

        }
        else if(direction.getProgress()==100){
            Direction_var="00";
            Direction_var=Direction_var+"R";
        }

        String horn1=horn.isChecked()?"T":"S";
        String a=horn1+speed_var+Direction_var;
        speed_view.setText(a);
        sendCommand(a);

    }

    private boolean isConnected() {
        return (connector != null) && (connector.getState() == DeviceConnector.STATE_CONNECTED);
    }
    private void stopConnection() {
        if (connector != null) {
            connector.stop();
            connector = null;
            deviceName = null;
        }
    }
    private void startDeviceListActivity() {
        stopConnection();
        Intent serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }

    @Override
    public boolean onSearchRequested() {
        if (super.isAdapterReady()) startDeviceListActivity();
        return false;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.device_control_activity, menu);
        final MenuItem bluetooth = menu.findItem(R.id.menu_search);
        if (bluetooth != null) bluetooth.setIcon(this.isConnected() ?
                R.drawable.ic_action_device_bluetooth_connected :
                R.drawable.ic_action_device_bluetooth);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_search:
                if (super.isAdapterReady()) {
                    if (isConnected()) stopConnection();
                    else startDeviceListActivity();
                } else {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    // ============================================================================





    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    BluetoothDevice device = btAdapter.getRemoteDevice(address);
                    if (super.isAdapterReady() && (connector == null)) setupConnector(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                super.pendingRequestEnableBt = false;
                if (resultCode != Activity.RESULT_OK) {
                    Utils.log("BT not enabled");
                }
                break;
        }
    }
    private void setupConnector(BluetoothDevice connectedDevice) {
        stopConnection();
        try {
            String emptyName = getString(R.string.empty_device_name);
            DeviceData data = new DeviceData(connectedDevice, emptyName);
            connector = new DeviceConnector(data, mHandler);
            connector.connect();
        } catch (IllegalArgumentException e) {
            Utils.log("setupConnector failed: " + e.getMessage());
        }
    }
    public void sendCommand(String commandString) {



            byte[] command = commandString.getBytes();
            if (isConnected()) {
                connector.write(command);
            }
        }

    void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
        getSupportActionBar().setSubtitle(deviceName);
    }
    private class BluetoothResponseHandler extends Handler {
        private WeakReference<DeviceControlActivity> mActivity;

        public BluetoothResponseHandler(DeviceControlActivity activity) {
            mActivity = new WeakReference<DeviceControlActivity>(activity);
        }

        public void setTarget(DeviceControlActivity target) {
            mActivity.clear();
            mActivity = new WeakReference<DeviceControlActivity>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            Vibrator v=(Vibrator) DeviceControlActivity.this.getSystemService(Context.VIBRATOR_SERVICE);
            DeviceControlActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case MESSAGE_STATE_CHANGE:

                        Utils.log("MESSAGE_STATE_CHANGE: " + msg.arg1);
                        final android.support.v7.app.ActionBar bar = activity.getSupportActionBar();
                        switch (msg.arg1) {
                            case DeviceConnector.STATE_CONNECTED:
                                if (bar != null) {
                                    bar.setSubtitle(MSG_CONNECTED);
                                }
                                break;
                            case DeviceConnector.STATE_CONNECTING:
                                if (bar != null) {
                                    bar.setSubtitle(MSG_CONNECTING);
                                }
                                break;
                            case DeviceConnector.STATE_NONE:
                                if (bar != null) {
                                    bar.setSubtitle(MSG_NOT_CONNECTED);
                                }
                                break;
                        }
                        activity.invalidateOptionsMenu();
                        break;
                    case MESSAGE_READ:
                        final String readMessage = (String) msg.obj;
                        if (readMessage != null) {
                            if(readMessage.equals("M")){

                                v.vibrate(1000);
                            }
                else if(readMessage.equals("N")){
                                v.cancel();
                            }

                        }
                        break;

                    case MESSAGE_DEVICE_NAME:
                        activity.setDeviceName((String) msg.obj);
                        break;

                    case MESSAGE_WRITE:
                        // stub
                        break;

                    case MESSAGE_TOAST:
                        // stub
                        break;
                }
            }
        }
    }
    // ==========================================================================
}