package com.fgtit.fingerprintattendance.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fgtit.fingerprintattendance.R;
import com.fgtit.fingerprintattendance.doa.CaptureDataSource;
import com.fgtit.fingerprintattendance.doa.EnrollDataSource;
import com.fgtit.fingerprintattendance.model.CaptureItem;
import com.fgtit.fingerprintattendance.model.EnrollItem;
import com.fgtit.fingerprintattendance.service.BluetoothService;
import com.fgtit.fingerprintattendance.widget.GPSTracker;
import com.fgtit.fingerprintattendance.widget.RoundImage;
import com.fgtit.fpcore.FPMatch;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class CaptureActivity extends AppCompatActivity implements View.OnClickListener{

    // Debugging
    private static final String TAG = CaptureActivity.class.getSimpleName();
    private static final boolean D = true;
    private CaptureDataSource captureDataSource;
    private EnrollDataSource enrollDataSource;

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

    private final static byte CMD_GETCHAR=0x31;
    private final static byte CMD_GETIMAGE=0x30;

    public byte mUpImage[]=new byte[73728];//36864
    public int mUpImageSize=0;

    private byte mDeviceCmd=0x00;
    private boolean mIsWork=false;
    private byte  mCmdData[]=new byte[10240];
    private int	  mCmdSize=0;

    private Timer mTimerTimeout=null;
    private TimerTask mTaskTimeout=null;
    private Handler mHandlerTimeout;

    public byte mMatData[]=new byte[512];
    public int mMatSize=0;


    private double longitude;
    private double latitude;
    private long timestamp;

    private ImageView mImage;
    private TextView mStatus, mDate, mTime, mLongitude, mLatitude;
    private MenuItem bluetoothMenu;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        Toolbar toolbar = (Toolbar) findViewById(R.id.sign_on_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);

        captureDataSource = new CaptureDataSource(this);
        enrollDataSource = new EnrollDataSource(this);

        mImage = (ImageView)findViewById(R.id.sign_on_image);
        mImage.setOnClickListener(this);
        mStatus = (TextView)findViewById(R.id.sign_on_status);
        mDate = (TextView)findViewById(R.id.sign_on_date);
        mTime = (TextView)findViewById(R.id.sign_on_time);
        mLongitude = (TextView)findViewById(R.id.sign_on_longitude);
        mLatitude = (TextView)findViewById(R.id.sign_on_latitude);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Runnable runnable = new mTimeLocRunner();
        Thread myThread= new Thread(runnable);
        myThread.start();

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mBluetoothService != null) mBluetoothService.stop();
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.sign_on_image: {
                if (mBluetoothService != null) {
                    // Only if the state is STATE_NONE, do we know that we haven't started already
                    switch (mBluetoothService.getState()) {
                        case BluetoothService.STATE_CONNECTED: {
                            if(!mIsWork) {
                                SendCommand(CMD_GETCHAR, null, 0);
                            }
                        }
                        break;

                        default:
                            showSnackBar("Device not connected");
                            break;
                    }
                }
            }
            break;
        }
    }


    private void initMatcher() {
        if(FPMatch.getInstance().InitMatch()==0){
            Toast.makeText(getApplicationContext(), "Init Matcher Fail!", Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(getApplicationContext(), "Init Matcher OK!", Toast.LENGTH_LONG).show();
        }

    }
    private void blueToothSetup() {
        Log.d(TAG, "setupChat()");

        mBluetoothService = BluetoothService.getInstance(this, mHandler);   // Initialize the BluetoothChatService to perform bluetooth connections
        mOutStringBuffer = new StringBuffer("");                    // Initialize the buffer for outgoing messages

        checkBluetoothStatus();
        initMatcher();
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

    public void doWork() {
        runOnUiThread(new Runnable() {
            public void run() {
                try{
                    getDateTime();
                    geoCapture();
                }catch (Exception e) {}
            }
        });
    }

    class mTimeLocRunner implements Runnable {
        // @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    doWork();
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                }
            }
        }
    }

    private void getDateTime() {
        Calendar calendar = Calendar.getInstance();
        timestamp = calendar.getTimeInMillis();
        Date dt = calendar.getTime();
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);
        String curTime = hours + ":" + minutes + ":" + seconds;
        String curDate = day + "/" + month + "/" + year;
        mDate.setText("Date : "+curDate);
        mTime.setText("TIME : "+curTime);
    }

    private void geoCapture() {
        GPSTracker gps = new GPSTracker(this);
        // check if GPS enabled
        if(gps.canGetLocation()) {
            longitude = gps.getLongitude();
            latitude = gps.getLatitude();

            this.mLongitude.setText(" LON : "+String.valueOf(longitude));
            this.mLatitude.setText(" LAT : "+String.valueOf(latitude));

        }else {
            gps.showSettingsAlert();
        }
    }



    private int calcCheckSum(byte[] buffer,int size) {
        int sum=0;
        for(int i=0;i<size;i++) {
            sum=sum+buffer[i];
        }
        return (sum & 0x00ff);
    }


    public void TimeOutStart() {
        if(mTimerTimeout!=null){
            return;
        }
        mTimerTimeout = new Timer();
        mHandlerTimeout = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                TimeOutStop();
                if(mIsWork){
                    mIsWork=false;
                    showSnackBar("Error! Time Out");
                    mStatus.setText("Place Finger on Device & Tap Screen");
                }
                super.handleMessage(msg);
            }
        };
        mTaskTimeout = new TimerTask() {
            @Override
            public void run() {
                Message message = new Message();
                message.what = 1;
                mHandlerTimeout.sendMessage(message);
            }
        };
        mTimerTimeout.schedule(mTaskTimeout, 10000, 10000);
    }


    public void TimeOutStop() {
        if (mTimerTimeout != null) {
            mTimerTimeout.cancel();
            mTimerTimeout = null;
            mTaskTimeout.cancel();
            mTaskTimeout = null;
        }
    }


    private void SendCommand(byte cmdid,byte[] data,int size) {
        if(mIsWork)return;

        int sendsize=9+size;
        byte[] sendbuf = new byte[sendsize];
        sendbuf[0]='F';
        sendbuf[1]='T';
        sendbuf[2]=0;
        sendbuf[3]=0;
        sendbuf[4]=cmdid;
        sendbuf[5]=(byte)(size);
        sendbuf[6]=(byte)(size>>8);
        if(size>0) {
            for(int i=0;i<size;i++) {
                sendbuf[7+i]=data[i];
            }
        }
        int sum=calcCheckSum(sendbuf,(7+size));
        sendbuf[7+size]=(byte)(sum);
        sendbuf[8+size]=(byte)(sum>>8);

        mIsWork=true;
        TimeOutStart();
        mDeviceCmd=cmdid;
        mCmdSize=0;
        mBluetoothService.write(sendbuf);

        switch(sendbuf[4]) {
            case CMD_GETCHAR:
                mStatus.setText("Reading Fingerprint...");
                break;
        }
    }


    private byte[] changeByte(int data) {
        byte b4 = (byte) ((data) >> 24);
        byte b3 = (byte) (((data) << 8) >> 24);
        byte b2 = (byte) (((data) << 16) >> 24);
        byte b1 = (byte) (((data) << 24) >> 24);
        byte[] bytes = { b1, b2, b3, b4 };
        return bytes;
    }

    private byte[] toBmpByte(int width, int height, byte[] data) {
        byte[] buffer = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            int bfType = 0x424d;
            int bfSize = 54 + 1024 + width * height;
            int bfReserved1 = 0;
            int bfReserved2 = 0;
            int bfOffBits = 54 + 1024;

            dos.writeShort(bfType);
            dos.write(changeByte(bfSize), 0, 4);
            dos.write(changeByte(bfReserved1), 0, 2);
            dos.write(changeByte(bfReserved2), 0, 2);
            dos.write(changeByte(bfOffBits), 0, 4);

            int biSize = 40;
            int biWidth = width;
            int biHeight = height;
            int biPlanes = 1;
            int biBitcount = 8;
            int biCompression = 0;
            int biSizeImage = width * height;
            int biXPelsPerMeter = 0;
            int biYPelsPerMeter = 0;
            int biClrUsed = 256;
            int biClrImportant = 0;

            dos.write(changeByte(biSize), 0, 4);
            dos.write(changeByte(biWidth), 0, 4);
            dos.write(changeByte(biHeight), 0, 4);
            dos.write(changeByte(biPlanes), 0, 2);
            dos.write(changeByte(biBitcount), 0, 2);
            dos.write(changeByte(biCompression), 0, 4);
            dos.write(changeByte(biSizeImage), 0, 4);
            dos.write(changeByte(biXPelsPerMeter), 0, 4);
            dos.write(changeByte(biYPelsPerMeter), 0, 4);
            dos.write(changeByte(biClrUsed), 0, 4);
            dos.write(changeByte(biClrImportant), 0, 4);

            byte[] palatte = new byte[1024];
            for (int i = 0; i < 256; i++) {
                palatte[i * 4] = (byte) i;
                palatte[i * 4 + 1] = (byte) i;
                palatte[i * 4 + 2] = (byte) i;
                palatte[i * 4 + 3] = 0;
            }
            dos.write(palatte);

            dos.write(data);
            dos.flush();
            buffer = baos.toByteArray();
            dos.close();
            baos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buffer;
    }


    public byte[] getFingerprintImage(byte[] data,int width,int height) {
        if (data == null) {
            return null;
        }
        byte[] imageData = new byte[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            imageData[i * 2] = (byte) (data[i] & 0xf0);
            imageData[i * 2 + 1] = (byte) (data[i] << 4 & 0xf0);
        }
        byte[] bmpData = toBmpByte(width, height, imageData);
        return bmpData;
    }


    private void memcpy(byte[] dstbuf,int dstoffset,byte[] srcbuf,int srcoffset,int size) {
        for(int i=0;i<size;i++) {
            dstbuf[dstoffset+i]=srcbuf[srcoffset+i];
        }
    }

    public static boolean bytesEquals(byte d1[], byte d2[])
    {
        if(d1 == null && d2 == null)
            return true;
        if(d1 == null || d2 == null)
            return false;
        if(d1.length != d2.length)
            return false;
        for(int i = 0; i < d1.length; i++)
            if(d1[i] != d2[i])
                return false;

        return true;
    }



    private void ReceiveCommand(byte[] databuf,int datasize) {
        if(mDeviceCmd==CMD_GETIMAGE) {
            memcpy(mUpImage,mUpImageSize,databuf,0,datasize);
            mUpImageSize=mUpImageSize+datasize;
            if(mUpImageSize>=15200){
                byte[] bmpdata=getFingerprintImage(mUpImage,152,200);
                Bitmap bmp = BitmapFactory.decodeByteArray(bmpdata, 0,bmpdata.length);
                Drawable roundedImage = new RoundImage(bmp);

                mUpImageSize=0;
                mIsWork=false;
				/*
				try {
					Thread.currentThread();
					Thread.sleep(200);
				}catch (InterruptedException e){
					e.printStackTrace();
				}

				SendCommand(CMD_GETCHAR,null,0);
				*/
                //showSnackBar("Data Captured");
                //isVerifying=false;
            }
        }else {
            memcpy(mCmdData, mCmdSize, databuf, 0, datasize);
            mCmdSize = mCmdSize + datasize;
            int totalsize = (byte) (mCmdData[5]) + ((mCmdData[6] << 8) & 0xFF00) + 9;
            if (mCmdSize >= totalsize) {
                mCmdSize = 0;
                mIsWork = false;
                if ((mCmdData[0] == 'F') && (mCmdData[1] == 'T')) {
                    switch (mCmdData[4]) {

                        case CMD_GETCHAR: {
                            SendCommand(CMD_GETIMAGE, null, 0);
                            int size = (byte) (mCmdData[5]) + ((mCmdData[6] << 8) & 0xFF00) - 1;
                            if (mCmdData[7] == 1) {
                                memcpy(mMatData, 0, mCmdData, 8, size);
                                mMatSize = size;

                                String suspectData = hexToString(mMatData, mMatSize);
                                mStatus.setText("Identifying...");
                                enrollDataSource.open();
                                List<EnrollItem> items = enrollDataSource.fetchAllEnroll();
                                enrollDataSource.close();
                                for(EnrollItem item : items) {
                                       /* Conversions.getInstance().StdChangeCoord(Base64.decode(item.getLeftThumb(),Base64.DEFAULT), 256, left_coord, 1);
                                    Conversions.getInstance().StdChangeCoord(Base64.decode(item.getRightThumb(),Base64.DEFAULT), 256, right_coord, 1);
                                    Conversions.getInstance().StdChangeCoord(mMatData, 256, susp_coord, 1);

                                    Conversions.getInstance().StdToIso(2, left_coord, left_iso);
                                    Conversions.getInstance().StdToIso(2, right_coord, right_iso);
                                    Conversions.getInstance().StdToIso(2, susp_coord, susp_iso);

                                   int leftThumb = FPMatch.getInstance().MatchTemplate(susp_iso, left_iso);
                                   int rightThumb = FPMatch.getInstance().MatchTemplate(susp_iso, right_iso);*/

                                    int leftThumb = FPMatch.getInstance().MatchTemplate(mMatData, Base64.decode(item.getLeftThumb(),Base64.DEFAULT));
                                    int rightThumb = FPMatch.getInstance().MatchTemplate(mMatData, Base64.decode(item.getRightThumb(),Base64.DEFAULT));
                                    if(leftThumb > 60 || rightThumb> 60) {

                                        String name = item.getFirstName()+" "+item.getLastNme();
                                        mStatus.setText(name);

                                        CaptureItem captureItem = new CaptureItem();
                                        captureItem.setCaptureId(item.getId());
                                        captureItem.setLongitude(longitude);
                                        captureItem.setLatitude(latitude);
                                        captureItem.setTimeStamp(String.valueOf(timestamp));

                                        byte[] imgData = Base64.decode(item.getImage(),Base64.DEFAULT);
                                        Bitmap bmp = BitmapFactory.decodeByteArray(imgData,0,imgData.length);
                                        Drawable roundedImage = new RoundImage(bmp);
                                        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.sign_on_relative_layout);
                                        relativeLayout.setBackgroundResource(R.drawable.white_circle);
                                        mImage.setScaleType(ImageView.ScaleType.FIT_XY);
                                        mImage.setImageDrawable(roundedImage);

                                        captureDataSource.open();
                                        captureDataSource.createCapture(captureItem);
                                        captureDataSource.close();
                                    }
                                }
                                showSnackBar("Match Found.");
                            } else {
                                showSnackBar("Error! Getting Data.");
                            }

                        }
                        break;

                        default:
                            break;
                    }
                }
            }
        }
    }


    private String hexToString(byte[] data,int size) {
        String str="";
        for(int i=0;i<size;i++) {
            str=str+","+Integer.toHexString(data[i]&0xFF).toUpperCase();
        }
        return str;
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
                        case BluetoothService.STATE_CONNECTING: {
                            updateBluetoothStatus(1);
                            showSnackBar("Please wait connecting...");
                        }
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
                    ReceiveCommand(readBuf,msg.arg1);
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
                break;
        }
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
            case 0: {
                bluetoothMenu.setIcon(getResources().getDrawable(R.drawable.ic_bluetooth_disabled));
            }
            break;

            case 1: {
                bluetoothMenu.setIcon(getResources().getDrawable(R.drawable.ic_bluetooth_connecting));

            }
            break;

            case 2: {
                bluetoothMenu.setIcon(getResources().getDrawable(R.drawable.ic_bluetooth_connected));
            }
            break;
        }
    }

    private void bluetoothConnect() {
        // Launch the BluetoothActivity to see devices and do scan
        Intent serverIntent = new Intent(this, BluetoothActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }


    //SnackBar function
    private void showSnackBar(String msg) {
        CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.sign_on_coordinator_layout);
        Snackbar snackbar = Snackbar
                .make(coordinatorLayout, msg, Snackbar.LENGTH_LONG);

        // Changing message text color
        snackbar.setActionTextColor(Color.RED);

        // Changing action button text color
        View sbView = snackbar.getView();
        TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        snackbar.show();

    }

}
