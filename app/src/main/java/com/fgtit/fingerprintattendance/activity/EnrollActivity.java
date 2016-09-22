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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fgtit.fingerprintattendance.R;
import com.fgtit.fingerprintattendance.app.AppConfig;
import com.fgtit.fingerprintattendance.doa.EnrollDataSource;
import com.fgtit.fingerprintattendance.model.EnrollItem;
import com.fgtit.fingerprintattendance.rest.ApiClient;
import com.fgtit.fingerprintattendance.rest.ApiInterface;
import com.fgtit.fingerprintattendance.service.BluetoothService;
import com.fgtit.fingerprintattendance.widget.RoundImage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EnrollActivity extends AppCompatActivity implements View.OnClickListener {

    // Debugging
    private static final String TAG = CaptureActivity.class.getSimpleName();
    private static final boolean D = true;

    //private Subscription subscription;
    //Observable<EnrollItem> observable;

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
    private static final int REQUEST_CAMERA = 3;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothService mBluetoothService = null;


    private final static byte CMD_GETBAT = 0x21;
    private final static byte CMD_GETIMAGE = 0x30;
    private final static byte CMD_GETCHAR = 0x31;

    private byte mDeviceCmd = 0x00;
    private boolean mIsWork = false;
    private byte mCmdData[] = new byte[10240];
    private int mCmdSize = 0;

    private Timer mTimerTimeout = null;
    private TimerTask mTaskTimeout = null;
    private Handler mHandlerTimeout;

    public byte mMatData[] = new byte[512];
    public int mMatSize = 0;

    public byte mBat[] = new byte[2];
    public byte mUpImage[] = new byte[73728];//36864
    public int mUpImageSize = 0;

    private int whichThumb = 0;

    private String dataImage;
    private String dataLeftThumb;
    private String dataRightThumb;

    private boolean isCaptureImage;
    private boolean isEnrollLeftThumb;
    private boolean isEnrollRightThumb;

    private MenuItem bluetoothMenu;
    private ImageView enrollImage, leftThumbImage, rightThumbImage;
    private Button enrollButton;
    private EditText inputFirstName, inputLastName;
    private ProgressBar progressBar;
    private TextInputLayout inputLayoutFirstName, inputLayoutLastName;

    private EnrollDataSource enrollDataSource;
    private JSONObject parameters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enroll);

        Toolbar toolbar = (Toolbar) findViewById(R.id.enroll_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);

        //this.subscription = observable.subscribe(this);
        enrollDataSource = new EnrollDataSource(this);

        inputLayoutFirstName = (TextInputLayout) findViewById(R.id.input_enroll_layout_first_name);
        inputLayoutLastName = (TextInputLayout) findViewById(R.id.input_enroll_layout_last_name);

        inputFirstName = (EditText) findViewById(R.id.input_enroll_first_name);
        inputFirstName.addTextChangedListener(new MyTextWatcher(inputFirstName));

        inputLastName = (EditText) findViewById(R.id.input_enroll_last_name);
        inputLastName.addTextChangedListener(new MyTextWatcher(inputLastName));

        enrollImage = (ImageView) findViewById(R.id.enroll_image);
        enrollImage.setOnClickListener(this);

        leftThumbImage = (ImageView) findViewById(R.id.img_left_thumb);
        leftThumbImage.setOnClickListener(this);

        rightThumbImage = (ImageView) findViewById(R.id.img_right_thumb);
        rightThumbImage.setOnClickListener(this);

        enrollButton = (Button) findViewById(R.id.btn_enroll);
        enrollButton.setOnClickListener(this);

        progressBar = (ProgressBar) findViewById(R.id.enroll_progressBar);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            showSnackBar("Bluetooth is not available");
            finish();
        }


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
        //this.subscription.unsubscribe();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.enroll_image:
                captureImage();
                break;

            case R.id.img_left_thumb: {
                SendCommand(CMD_GETCHAR, null, 0);
                whichThumb = 1;
            }
            break;

            case R.id.img_right_thumb: {
                SendCommand(CMD_GETCHAR, null, 0);
                whichThumb = 2;
            }
            break;

            case R.id.btn_enroll: {
                enroll();
            }
            break;

            default:
                break;
        }
    }


    private void blueToothSetup() {
        Log.d(TAG, "setupChat()");

        mBluetoothService = BluetoothService.getInstance(this, mHandler);   // Initialize the BluetoothChatService to perform bluetooth connections
        mOutStringBuffer = new StringBuffer("");                    // Initialize the buffer for outgoing messages

        checkBluetoothStatus();
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

    private int calcCheckSum(byte[] buffer, int size) {
        int sum = 0;
        for (int i = 0; i < size; i++) {
            sum = sum + buffer[i];
        }
        return (sum & 0x00ff);
    }

    public void TimeOutStart() {
        if (mTimerTimeout != null) {
            return;
        }
        mTimerTimeout = new Timer();
        mHandlerTimeout = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                TimeOutStop();
                if (mIsWork) {
                    mIsWork = false;
                    showSnackBar("Error! Time Out");
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


    private void SendCommand(byte cmdid, byte[] data, int size) {
        if (mIsWork) return;

        int sendsize = 9 + size;
        byte[] sendbuf = new byte[sendsize];
        sendbuf[0] = 'F';
        sendbuf[1] = 'T';
        sendbuf[2] = 0;
        sendbuf[3] = 0;
        sendbuf[4] = cmdid;
        sendbuf[5] = (byte) (size);
        sendbuf[6] = (byte) (size >> 8);
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                sendbuf[7 + i] = data[i];
            }
        }
        int sum = calcCheckSum(sendbuf, (7 + size));
        sendbuf[7 + size] = (byte) (sum);
        sendbuf[8 + size] = (byte) (sum >> 8);

        mIsWork = true;
        TimeOutStart();
        mDeviceCmd = cmdid;
        mCmdSize = 0;
        mBluetoothService.write(sendbuf);

        switch (sendbuf[4]) {
            case CMD_GETBAT:
                //AddStatusList("Get Battery Value ...");
                break;
            case CMD_GETIMAGE:
                mUpImageSize = 0;
                showSnackBar("Processing...");
                break;
            case CMD_GETCHAR:
                showSnackBar("Initializing...");
                break;
        }
    }

    private byte[] changeByte(int data) {
        byte b4 = (byte) ((data) >> 24);
        byte b3 = (byte) (((data) << 8) >> 24);
        byte b2 = (byte) (((data) << 16) >> 24);
        byte b1 = (byte) (((data) << 24) >> 24);
        byte[] bytes = {b1, b2, b3, b4};
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


    public byte[] getFingerprintImage(byte[] data, int width, int height) {
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


    private void memcpy(byte[] dstbuf, int dstoffset, byte[] srcbuf, int srcoffset, int size) {
        for (int i = 0; i < size; i++) {
            dstbuf[dstoffset + i] = srcbuf[srcoffset + i];
        }
    }

    private void ReceiveCommand(byte[] databuf, int datasize) {
        if (mDeviceCmd == CMD_GETIMAGE) {
            memcpy(mUpImage, mUpImageSize, databuf, 0, datasize);
            mUpImageSize = mUpImageSize + datasize;
            if (mUpImageSize >= 15200) {
                byte[] bmpdata = getFingerprintImage(mUpImage, 152, 200);
                Bitmap bmp = BitmapFactory.decodeByteArray(bmpdata, 0, bmpdata.length);
                Drawable roundedImage = new RoundImage(bmp);
                if (whichThumb == 1) {
                    RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.left_thumb_relative_layout);
                    relativeLayout.setBackgroundResource(R.drawable.white_circle);
                    leftThumbImage.setScaleType(ImageView.ScaleType.FIT_XY);
                    leftThumbImage.setImageDrawable(roundedImage);
                    isEnrollLeftThumb = true;
                    whichThumb = 0;
                } else if (whichThumb == 2) {
                    RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.right_thumb_relative_layout);
                    relativeLayout.setBackgroundResource(R.drawable.white_circle);
                    rightThumbImage.setScaleType(ImageView.ScaleType.FIT_XY);
                    rightThumbImage.setImageDrawable(roundedImage);
                    isEnrollRightThumb = true;
                    whichThumb = 0;
                }
                mUpImageSize = 0;
                mIsWork = false;
				/*
				try {
					Thread.currentThread();
					Thread.sleep(200);
				}catch (InterruptedException e){
					e.printStackTrace();
				}

				SendCommand(CMD_GETCHAR,null,0);
				*/
                showSnackBar("Data Captured");
            }
        } else {
            memcpy(mCmdData, mCmdSize, databuf, 0, datasize);
            mCmdSize = mCmdSize + datasize;
            int totalsize = (byte) (mCmdData[5]) + ((mCmdData[6] << 8) & 0xFF00) + 9;
            if (mCmdSize >= totalsize) {
                mCmdSize = 0;
                mIsWork = false;
                if ((mCmdData[0] == 'F') && (mCmdData[1] == 'T')) {
                    switch (mCmdData[4]) {

                        case CMD_GETBAT: {
                            int size = (byte) (mCmdData[5]) + ((mCmdData[6] << 8) & 0xFF00) - 1;
                            if (size > 0) {
                                memcpy(mBat, 0, mCmdData, 8, size);
                                //AddStatusList("Battery Value:"+Integer.toString(mBat[0]/10)+"."+Integer.toString(mBat[0]%10)+"V");
                            } else ;
                            //AddStatusList("Get Battery Value Fail");
                        }
                        break;
                        case CMD_GETCHAR: {
                            int size = (byte) (mCmdData[5]) + ((mCmdData[6] << 8) & 0xFF00) - 1;
                            if (mCmdData[7] == 1) {
                                memcpy(mMatData, 0, mCmdData, 8, size);
                                mMatSize = size;

                                if (whichThumb == 1)
                                    dataLeftThumb = Base64.encodeToString(mMatData, 0, mMatSize, Base64.DEFAULT); //hexToString(mMatData,mMatSize);
                                else if (whichThumb == 2)
                                    dataRightThumb = Base64.encodeToString(mMatData, 0, mMatSize, Base64.DEFAULT); //hexToString(mMatData,mMatSize);

                                SendCommand(CMD_GETIMAGE, null, 0);
                            } else
                                showSnackBar("Error! Getting Data.");
                        }
                        break;
                    }
                }
            }
        }
    }


    private String hexToString(byte[] data, int size) {
        String str = "";
        for (int i = 0; i < size; i++) {
            str = str + "," + Integer.toHexString(data[i] & 0xFF).toUpperCase();
        }
        return str;
    }


    // The Handler that gets information back from the BluetoothService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if (D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
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
                    ReceiveCommand(readBuf, msg.arg1);
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    /*Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();*/
                    showSnackBar("Connected to " + mConnectedDeviceName);
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
        if (D) Log.d(TAG, "onActivityResult " + resultCode);

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
            case REQUEST_CAMERA:
                if (resultCode == Activity.RESULT_OK) {
                    Bitmap bmp = (Bitmap) data.getExtras().get("data");

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byte[] img = stream.toByteArray();
                    dataImage = Base64.encodeToString(img, Base64.DEFAULT);
                    //Toast.makeText(this, dataImage, Toast.LENGTH_SHORT).show();

                    Drawable roundedImage = new RoundImage(bmp);
                    RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.enroll_relative_layout);
                    relativeLayout.setBackgroundResource(R.drawable.white_circle);
                    enrollImage.setScaleType(ImageView.ScaleType.FIT_XY);
                    enrollImage.setImageDrawable(roundedImage);

                    isCaptureImage = true;
                } else {
                    Log.d(TAG, "Image not captured");
                    showSnackBar("Image not captured");
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

            case R.id.bluetooth: {
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


    private void captureImage() {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, REQUEST_CAMERA);
    }

    private boolean validateInput(EditText EdTxt, TextInputLayout inputLayout) {
        if (EdTxt.getText().toString().trim().isEmpty()) {
            inputLayout.setError(getString(R.string.err_msg_input));
            requestFocus(EdTxt);
            return false;
        } else {
            inputLayout.setErrorEnabled(false);
        }

        return true;
    }

    private void requestFocus(View view) {
        if (view.requestFocus()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    private void enroll() {
        if (!validateInput(inputFirstName, inputLayoutFirstName)) {
            return;
        }

        if (!validateInput(inputLastName, inputLayoutLastName)) {
            return;
        }

        if (!isCaptureImage) {
            showSnackBar("Image not Captured.");
            return;
        }
        if (!isEnrollLeftThumb) {
            showSnackBar("Left Thumb not Enrolled");
            return;
        }
        if (!isEnrollRightThumb) {
            showSnackBar("Right Thumb not Enrolled");
            return;
        }

        EnrollItem item = new EnrollItem();
        item.setImage(dataImage);
        item.setFirstName(inputFirstName.getText().toString().trim());
        item.setLastName(inputLastName.getText().toString().trim());
        item.setLeftThumb(dataLeftThumb);
        item.setRightThumb(dataRightThumb);
        item.setIsSync(false);

        progressBar.setVisibility(View.VISIBLE);
        enrollButton.setVisibility(View.GONE);

        enrollDataSource.open();
        enrollDataSource.createEnroll(item);
        enrollDataSource.close();

        parameters = payload(dataImage, inputFirstName.getText().toString().trim(),
                inputLastName.getText().toString().trim(), dataLeftThumb, dataRightThumb);

        submitPayload(payload(dataImage, inputFirstName.getText().toString().trim(),
                inputLastName.getText().toString().trim(), dataLeftThumb, dataRightThumb));
    }


    private JSONObject payload(String image, String firstName, String lastName, String leftThumb, String rightThumb) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("image", image);
            obj.put("firstName", firstName);
            obj.put("lastName", lastName);
            obj.put("leftThumb", leftThumb);
            obj.put("rightThumb", rightThumb);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }


    private void submitPayload(JSONObject payLoad) {

        ApiInterface apiService =
                ApiClient.getClient().create(ApiInterface.class);

        Call<EnrollItem> call = apiService.enroll(payLoad);
        call.enqueue(new Callback<EnrollItem>() {
            @Override
            public void onResponse(Call<EnrollItem>call, Response<EnrollItem> response) {
                //response.body().;
            }

            @Override
            public void onFailure(Call<EnrollItem>call, Throwable t) {
                // Log error here since request failed
                Log.e(TAG, t.toString());
            }
        });

        /*Observable<EnrollItem> call = apiService.enroll(payLoad);
        Subscription subscription = call
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<EnrollItem>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        // cast to retrofit.HttpException to get the response code
                        if (e instanceof HttpException) {
                            HttpException response = (HttpException) e;
                            int code = response.code();
                        }
                    }

                    @Override
                    public void onNext (EnrollItem item){
                    }

                });*/
    }


    private class submitPayload extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            HttpURLConnection urlConnection = null;
            DataOutputStream wr = null;
            InputStream is;

            try {
                URL url = new URL(AppConfig.ENROLL);
                urlConnection = (HttpURLConnection) url.openConnection();
                String data = parameters.toString();

                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);
                urlConnection.setDoInput(true);
                urlConnection.setUseCaches(false);
                urlConnection.setConnectTimeout(15000);
                urlConnection.setReadTimeout(30000);
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("Authorization", AppConfig.AUTHORIZATION);
                urlConnection.setFixedLengthStreamingMode(data.getBytes().length);
                urlConnection.connect();

                wr = new DataOutputStream(urlConnection.getOutputStream());
                wr.writeBytes(data);

                wr.flush();

                is = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                return buffer.toString();

            } catch (IOException e) {
                Log.e(TAG, "Error ", e);
                // If the code didn't successfully get the  data, there's no point in attempting
                // to parse it.
                showSnackBar(getString(R.string.err_network_timeout));
                /*try {

                    SyncItem item = new SyncItem();
                    item.setEnumerationCode(parameters.getString("enumerationCode"));
                    item.setLongitude(parameters.getDouble("longitude"));
                    item.setLatitude(parameters.getDouble("latitude"));
                    item.setPicture(parameters.getString("picture"));
                    item.setOfficerCode(parameters.getString("officerCode"));
                    item.setClientCode(parameters.getString("clientCode"));

                    syncDataSource.createSync(item);

                }catch (JSONException jEx) {
                    Log.d(TAG, "Parse Json Error: " + jEx.getMessage());
                }*/
                return null;
            } finally {

                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (wr != null) {
                    try {
                        wr.close();
                    } catch (final IOException e) {
                        Log.e(TAG, "Error closing stream", e);
                    }
                }
            }
        }

        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            if (s != null) {

                try {
                    JSONObject responseObj = new JSONObject(s);
                    // Parsing json object response
                    // response will be a json object
                    JSONObject response = responseObj.getJSONObject("response");


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            enrollButton.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);

        }
    }

    private class MyTextWatcher implements TextWatcher {

        private View view;

        private MyTextWatcher(View view) {
            this.view = view;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void afterTextChanged(Editable editable) {
            switch (view.getId()) {

                case R.id.input_enroll_first_name:
                    inputLayoutFirstName.setErrorEnabled(false);
                    //validatePhoneNumber(inputLoginPhone, inputLayoutLoginPhone);
                    break;

                case R.id.input_enroll_last_name:
                    inputLayoutLastName.setErrorEnabled(false);
                    //validatePin(inputLoginPin, inputLayoutLoginPin);
                    break;
            }
        }
    }

    //SnackBar function
    private void showSnackBar(String msg) {
        CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.enroll_coordinator_layout);
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
