package com.fgtit.fingerprintattendance.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.fgtit.fingerprintattendance.R;
import com.fgtit.fingerprintattendance.service.BluetoothService;

public class DashBoardActivity extends AppCompatActivity {

    // Debugging
    private static final String TAG = CaptureActivity.class.getSimpleName();
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothService mBluetoothService = null;

    private MenuItem bluetoothMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dash_board);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);

        Toolbar toolbar = (Toolbar) findViewById(R.id.dash_board_toolbar);
        setSupportActionBar(toolbar);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            showSnackBar("Bluetooth is not available");
            finish();
        }
    }


   /* @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            if (mBluetoothService == null) blueToothSetup();
        }
    } */

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mBluetoothService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mBluetoothService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                mBluetoothService.start();
            }
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mBluetoothService != null) mBluetoothService.stop();
    }


    private void blueToothSetup() {
        Log.d(TAG, "setupChat()");

        mBluetoothService =  BluetoothService.getInstance(this, mHandler);    // Initialize the BluetoothChatService to perform bluetooth connections
        mOutStringBuffer = new StringBuffer("");                    // Initialize the buffer for outgoing messages
    }

    private void bluetoothEnable() {
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            if (mBluetoothService == null) blueToothSetup();
        }
    }

    // The Handler that gets information back from the BluetoothService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            updateBluetoothStatus(2);
                            //mConversationArrayAdapter.clear();
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            updateBluetoothStatus(1);
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            updateBluetoothStatus(0);
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    //String writeMessage = new String(writeBuf);
                    //AddStatusList("Send:  " + writeMessage);
                    //AddStatusListHex(writeBuf,writeBuf.length);
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    //AddStatusList("Len="+Integer.toString(msg.arg1));
                    //AddStatusListHex(readBuf,msg.arg1);
                    //ReceiveCommand(readBuf,msg.arg1);
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    /*Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();*/
                    showSnackBar( "Connected to " + mConnectedDeviceName);
                    break;
                case MESSAGE_TOAST:
                    /*Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();*/
                    showSnackBar(msg.getData().getString(TOAST));
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras()
                            .getString(BluetoothActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BLuetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    mBluetoothService.connect(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    blueToothSetup();
                } else {
                    // User did not enable Bluetooth or an error occured
                    Log.d(TAG, "BT not enabled");
                    /*Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();*/
                    showSnackBar(getResources().getString(R.string.bt_not_enabled));
                    finish();
                }
        }
    }


    public void onClickRecords(View v) {

        Intent intent = new Intent(DashBoardActivity.this, RecordsActivity.class);
        startActivity(intent);
    }

    public void onClickSignOn(View v) {

        Intent intent = new Intent(DashBoardActivity.this, CaptureActivity.class);
        startActivityForResult(intent,1);
    }

    public void onClickEnroll(View v) {

        Intent intent = new Intent(DashBoardActivity.this, EnrollActivity.class);
        startActivity(intent);
    }

    public void onClickHistory(View v) {

        Intent intent = new Intent(DashBoardActivity.this, HistoryActivity.class);
        startActivity(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.dash_board_menu, menu);
        bluetoothMenu = menu.findItem(R.id.bluetooth);
        bluetoothEnable();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {

            case R.id.bluetooth:{
                bluetoothConnect();
            }
            break;


            default:
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateBluetoothStatus(int status) {
        switch (status) {
            case 0:
                bluetoothMenu.setIcon(getResources().getDrawable(R.drawable.ic_bluetooth_disabled));
                break;
            case 1:
                bluetoothMenu.setIcon(getResources().getDrawable(R.drawable.ic_bluetooth_connecting));
                break;
            case 2:
                bluetoothMenu.setIcon(getResources().getDrawable(R.drawable.ic_bluetooth_connected));
                break;
        }
    }

    private void bluetoothConnect() {
        // Launch the BluetoothActivity to see devices and do scan
        Intent serverIntent = new Intent(this, BluetoothActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }


    private void checkBluetoothStatus() {
        if (mBluetoothService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            switch (mBluetoothService.getState()) {
                case BluetoothService.STATE_CONNECTED:
                    updateBluetoothStatus(2);
                    //mConversationArrayAdapter.clear();
                    break;
                case BluetoothService.STATE_CONNECTING:
                    updateBluetoothStatus(1);
                    break;
                case BluetoothService.STATE_LISTEN:
                case BluetoothService.STATE_NONE:
                    updateBluetoothStatus(0);
                    break;
            }
        }
    }



    //SnackBar function
    private void showSnackBar(String msg) {
        CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.dash_board_coordinator_layout);
        Snackbar snackbar = Snackbar
                .make(coordinatorLayout, msg, Snackbar.LENGTH_LONG);
       /* if(id == 0){
            snackbar.setAction("RETRY", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (cd.isConnectingToInternet())
                        submitLogin();
                    else
                        showSnackBar(0, getString(R.string.err_no_internet));
                }
            });
        }*/


        // Changing message text color
        snackbar.setActionTextColor(Color.RED);

        // Changing action button text color
        View sbView = snackbar.getView();
        TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        snackbar.show();

    }
}
