package com.example.playfiblesdk;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class PlayFiBLESDK
{
    private static final String TAG = "BLELogs";
    private Context mContext;
    private BluetoothAdapter bluetoothAdapter;
    private static PlayFiBLESDK mPlayFiSDK;
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean scanning;
    Set< BluetoothDevice > mDeviceList = new HashSet<>();
    BluetoothGatt bluetoothGatt;
    List< BluetoothGattCharacteristic > mBluetoothGattCharacteristic;
    private Handler mHandler;
    private final String STATE_CHARACTERISTICS = "00001004-6B83-4B7E-8C82-DB4E5B93C221";
    private final String SCAN_WIFI_CHARACTERISTICS = "00001008-6B83-4B7E-8C82-DB4E5B93C221";
    private final String SCAN_RESULTS_CHARACTERISTICS = "0000100C-6B83-4B7E-8C82-DB4E5B93C221";
    private final String WIFI_SSID_CHARACTERISTICS = "00001010-6B83-4B7E-8C82-DB4E5B93C221";
    private final String WIFI_PASSWORD_CHARACTERISTICS = "00001014-6B83-4B7E-8C82-DB4E5B93C221";
    private final String CONNECT_CHARACTERISTICS = "00001018-6B83-4B7E-8C82-DB4E5B93C221";

    private static final String PLAYFIDEVICE = "PlayFiDevice";
    private static final String PLAYFI2DEVICE = "PlayFi2Device";

    private OnScanDeviceFoundListener mListener;
    private OnDeviceConnectionListener mDeviceConnectionListener;
    StateCharacteristics mCurrentState = StateCharacteristics.NONE;

    enum StateCharacteristics
    {
        SERVICE_AVAILABILITY,
        READ_SCAN_WIFI,
        WRITE_SCAN_WIFI,
        READ_AND_POLL_SCAN_WIFI,
        READ_SCAN_RESULTS,
        WRITE_WIFI_SSID,
        WRITE_WIFI_PASSWORD,
        WRITE_CONNECT,
        NONE
    }
    public void initializeSDK( Context context )
    {
        mContext = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = new Handler();
    }

    public void setOnDeviceFoundListener( OnScanDeviceFoundListener mListener )
    {
        this.mListener = mListener;
    }


    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;


    public static PlayFiBLESDK getInstance()
    {
        if( mPlayFiSDK == null )
        {
            mPlayFiSDK = new PlayFiBLESDK();
        }
        return mPlayFiSDK;
    }

    public interface OnScanDeviceFoundListener
    {
        void onScanResult( BluetoothDevice device );

        void onScanFailed( int errorCode );
    }

    public boolean isEnable()
    {
        if( bluetoothAdapter == null )
        {
            Log.d( TAG, "Device doesn't support Bluetooth..!!" );
            // Device doesn't support Bluetooth
            return false;
        }
        else
        {
            return bluetoothAdapter.isEnabled();
        }
    }

    ScanCallback scanCallback = new ScanCallback()
    {
        @Override
        public void onScanResult( int callbackType, ScanResult result )
        {
            Log.d( TAG, "onScanResult() called with: callbackType = [" + callbackType + "], result = [" + result + "]" );
            if( mListener != null )
            {
                Log.d( TAG, "onScanResult: Device name : " + result.getDevice().getName() );
                if( result.getDevice().getName() != null ) //&& result.getDevice().getName().startsWith( "PlayFi" ) )
                {
                    if( mDeviceList.add( result.getDevice() ) )
                    {
                        mListener.onScanResult( result.getDevice() );
                    }
                }
                else
                {
                    Log.d( TAG, "onScanResult: other devices..!!" );
                }
            }
            else
            {
                Log.d( TAG, "onScanResult: mListener is null..!!!" );
            }
        }

        @Override
        public void onScanFailed( int errorCode )
        {
            Log.e( TAG, "onScanFailed: " + errorCode );
            if( mListener != null )
                mListener.onScanFailed( errorCode );
        }
    };

    public void scanDevices()
    {
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        if( !scanning )
        {
            // Stops scanning after a predefined scan period.
            mHandler.postDelayed( new Runnable()
            {
                @Override
                public void run()
                {
                    scanning = false;
                    Log.d( TAG, "stopScan() called" );
                    bluetoothLeScanner.stopScan( scanCallback );
                }
            }, SCAN_PERIOD );

            scanning = true;

//            UUID serviceUUIDs = UUID.fromString( "00001000-6B83-4B7E-8C82-DB4E5B93C221" );

            List< ScanFilter > scanFilterList = new ArrayList<>();
            scanFilterList.add( new ScanFilter.Builder()
                                        .build() );
//                                        .setDeviceName( "PlayFi2DeviceF00A527048" )

            ScanSettings scanSettings = new ScanSettings.Builder().build();
//                    .setScanMode( ScanSettings.SCAN_MODE_LOW_POWER )
//                    .build();
            mDeviceList.clear();
            if( scanCallback != null && bluetoothLeScanner != null )
            {
                bluetoothLeScanner.startScan( scanCallback );
            }
            else
            {
                Toast.makeText( mContext, "No device Found", Toast.LENGTH_SHORT ).show();
            }
        }
        else
        {
            scanning = false;
            if( scanCallback != null && bluetoothLeScanner != null )
            {
                bluetoothLeScanner.stopScan( scanCallback );
            }
            else
            {
                Toast.makeText( mContext, "No device Found", Toast.LENGTH_SHORT ).show();
            }
        }
    }


    @SuppressLint( "MissingPermission" )
    public void stopScanning()
    {
        if( scanCallback != null && bluetoothLeScanner != null )
        {
            bluetoothLeScanner.stopScan( scanCallback );
        }
        else
        {
            Toast.makeText( mContext, "No device Found", Toast.LENGTH_SHORT ).show();
        }
    }

    BluetoothGattCallback gattCallback = new BluetoothGattCallback()
    {
        @Override
        public void onConnectionStateChange( BluetoothGatt gatt, int status, int newState )
        {
            Log.d( TAG, "onConnectionStateChange() called with: gatt = [" + gatt + "], status = [" + status + "], newState = [" + newState + "]" );
            if( newState == BluetoothProfile.STATE_CONNECTED )
            {
                // successfully connected to the GATT Server
                Log.d( TAG, "onConnectionStateChange: Successfully connected to the GATT Server" );
                if( gatt.discoverServices() )
                {
                    Log.d( TAG, "onConnectionStateChange: discoverServices() success..!!" );
                }
                else
                {
                    Log.d( TAG, "onConnectionStateChange: discoverServices() failed..!!" );
                }
            }
            else if( newState == BluetoothProfile.STATE_DISCONNECTED )
            {
                // disconnected from the GATT Server
                Log.d( TAG, "onConnectionStateChange: Disconnected from the GATT Server" );
            }
            mDeviceConnectionListener.onConnectionStateChange( gatt, status, newState );
        }

        @Override
        public void onServicesDiscovered( BluetoothGatt gatt, int status )
        {
            Log.d( TAG, "onServicesDiscovered() called with: gatt = [" + gatt + "], status = [" + status + "]" );
            if( status == BluetoothGatt.GATT_SUCCESS )
            {
                Log.d( TAG, "onServicesDiscovered: Gatt_success....!!!!" );
                if( gatt != null )
                {
                    List< BluetoothGattService > bluetoothGattServices = gatt.getServices();
                    for( BluetoothGattService bluetoothGattService : bluetoothGattServices )
                    {
                        if( bluetoothGattService.getUuid().equals( UUID.fromString( "00001000-6B83-4B7E-8C82-DB4E5B93C221" ) ) )
                        {
                            Log.d( TAG, "\n\n******************Play-Fi Setup GATT service characteristics ****************************************" );
                            Log.d( TAG, "onServicesDiscovered:  Found Play-Fi Setup GATT service with uuid = " + bluetoothGattService.getUuid() );
                            Log.d( TAG, "onServicesDiscovered:  Found Play-Fi Setup GATT service Characteristics size = " + bluetoothGattService.getCharacteristics().size() );
                            mBluetoothGattCharacteristic = bluetoothGattService.getCharacteristics();
                            mCurrentState = StateCharacteristics.SERVICE_AVAILABILITY;
                            handleState( gatt );
                        }
                        Log.d( TAG, "**********************************************************\n" );
                        Log.d( TAG, "onServicesDiscovered() called with: gatt = [" + gatt + "], status = [" + status + "]" );
                        Log.d( TAG, "onServicesDiscovered: getUuid = " + bluetoothGattService.getUuid() );
                    }
                }
            }
            else
            {
                Log.d( TAG, "onServicesDiscovered received: " + status );
            }
        }

        @Override
        public void onCharacteristicRead( BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status )
        {
            Log.d( TAG, "onCharacteristicRead() called with: gatt = [" + gatt + "], characteristic = [" + characteristic + "], status = [" + status + "]" );
            Log.d( TAG, "onCharacteristicRead: uuid = " + characteristic.getUuid() );
            Log.d( TAG, "onCharacteristicRead: string valuee = " + Arrays.toString( characteristic.getValue() ) );

            if( characteristic.getUuid().toString().equalsIgnoreCase( SCAN_RESULTS_CHARACTERISTICS ) )
            {
                mDeviceConnectionListener.onCharacteristicRead( gatt, characteristic, status );
            }
            readWriteCharactristics( gatt, characteristic );

        }

        @Override
        public void onCharacteristicWrite( BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status )
        {
            Log.d( TAG, "onCharacteristicWrite() called with: gatt = [" + gatt + "], characteristic = [" + characteristic + "], status = [" + status + "]" );
            Log.d( TAG, "onCharacteristicWrite: write value = " + Arrays.toString( characteristic.getValue() ) );
            readWriteCharactristics( gatt, characteristic );
        }
    };

    public void handleState( BluetoothGatt gatt )
    {
        if( gatt != null )
        {
            switch( mCurrentState )
            {
                case SERVICE_AVAILABILITY:
                {
                    for( BluetoothGattCharacteristic bluetoothGattCharacteristic : mBluetoothGattCharacteristic )
                    {
                        Log.d( TAG, "onServicesDiscovered: Play-Fi Setup GATT service characteristics getUUids = " + bluetoothGattCharacteristic.getUuid() );
                        if( bluetoothGattCharacteristic.getUuid().toString().equalsIgnoreCase( STATE_CHARACTERISTICS ) )
                        {
                            gatt.readCharacteristic( bluetoothGattCharacteristic );
                        }
                    }
                    break;
                }
                case WRITE_SCAN_WIFI:
                {
                    for( BluetoothGattCharacteristic bluetoothGattCharacteristic : mBluetoothGattCharacteristic )
                    {
                        Log.d( TAG, "onServicesDiscovered: Play-Fi Setup GATT service characteristics getUUids = " + bluetoothGattCharacteristic.getUuid() );
                        if( bluetoothGattCharacteristic.getUuid().toString().equalsIgnoreCase( SCAN_WIFI_CHARACTERISTICS ) )
                        {
                            byte[] b = { 0x01 };
                            bluetoothGattCharacteristic.setValue(b);

                            Log.d( TAG, "handleState: " + Arrays.toString( bluetoothGattCharacteristic.getValue() ) );
                            gatt.writeCharacteristic( bluetoothGattCharacteristic );
                        }
                    }
                    break;
                }
                case READ_SCAN_WIFI:
                {
                    for( BluetoothGattCharacteristic bluetoothGattCharacteristic : mBluetoothGattCharacteristic )
                    {
                        Log.d( TAG, "onServicesDiscovered: Play-Fi Setup GATT service characteristics getUUids = " + bluetoothGattCharacteristic.getUuid() );
                        if( bluetoothGattCharacteristic.getUuid().toString().equalsIgnoreCase( SCAN_WIFI_CHARACTERISTICS ) )
                        {
                            gatt.readCharacteristic( bluetoothGattCharacteristic );
                        }
                    }
                    break;
                }
                case READ_AND_POLL_SCAN_WIFI:
                case READ_SCAN_RESULTS:
                {
                    for( BluetoothGattCharacteristic bluetoothGattCharacteristic : mBluetoothGattCharacteristic )
                    {
                        Log.d( TAG, "onServicesDiscovered: Play-Fi Setup GATT service characteristics getUUids = " + bluetoothGattCharacteristic.getUuid() );
                        if( bluetoothGattCharacteristic.getUuid().toString().equalsIgnoreCase( SCAN_RESULTS_CHARACTERISTICS ) )
                        {
                            gatt.readCharacteristic( bluetoothGattCharacteristic );
                        }
                    }
                    break;
                }
                case WRITE_WIFI_SSID:
                case WRITE_WIFI_PASSWORD:
                case WRITE_CONNECT:
            }
        }
    }

    public void readWriteCharactristics( BluetoothGatt gatt, BluetoothGattCharacteristic characteristic )
    {
        String uuid = String.valueOf( characteristic.getUuid() );
        switch( uuid.toUpperCase( Locale.ROOT ) )
        {
            case STATE_CHARACTERISTICS:
            {
                byte[] value = {0};
                if( Arrays.equals( characteristic.getValue(), value ) )
                {
                    Log.d( TAG, "onCharacteristicRead: 'State' value is 0..!!" );
                    Log.d( TAG, "Speaker is ready to proceed with setup..!!" );
                    mCurrentState = StateCharacteristics.WRITE_SCAN_WIFI;
                    handleState( gatt );
                }
                break;
            }
            case SCAN_WIFI_CHARACTERISTICS:
            {
                byte[] value = {2};
                byte[] notScanning = {0};
                byte[] busy = {1};
                if( Arrays.equals( characteristic.getValue(), value ) )
                {
                    Log.d( TAG, "onCharacteristicRead: 'ScanWiFi' value is 2..!!" );
                    mCurrentState = StateCharacteristics.READ_SCAN_RESULTS;
                    handleState( gatt );
                }
                else if( Arrays.equals( characteristic.getValue(), busy ) )
                {
                    Log.d( TAG, "readWriteCharactristics: Busy in Scanning" );
                    mCurrentState = StateCharacteristics.READ_SCAN_RESULTS;
                    handleState( gatt );
                }
                else if( Arrays.equals( characteristic.getValue(), notScanning ) )
                {
                    Log.d( TAG, "readWriteCharactristics: Not Scanning" );
                    mCurrentState = StateCharacteristics.WRITE_SCAN_WIFI;
                    handleState( gatt );
                }
                break;
            }
            case SCAN_RESULTS_CHARACTERISTICS:
            {
                Log.d( TAG, "readWriteCharactristics: Scan list of devices....!!!!!" );
                Log.d( TAG, "readWriteCharactristics: " );
                String string = characteristic.getStringValue( 0 );
                Log.d( TAG, "readWriteCharactristics: Values = " + string );
                break;
            }
            case WIFI_SSID_CHARACTERISTICS:
            {
                break;
            }
            case WIFI_PASSWORD_CHARACTERISTICS:
        }
    }
    public void connect( BluetoothDevice device )
    {
        bluetoothGatt = device.connectGatt( mContext, false, gattCallback );
        if( bluetoothGatt != null )
        {
            Log.d( TAG, "connect: success..!!" );
        }
        else
        {
            Log.d( TAG, "connect: failed..!!" );
        }
    }

    public interface OnDeviceConnectionListener
    {
        public void onConnectionStateChange( BluetoothGatt gatt, int status, int newState );

        public void onServicesDiscovered( BluetoothGatt gatt, int status );

        public void onCharacteristicRead( BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status );

        public void onCharacteristicWrite( BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status );
    }

    public void setOnDeviceConnectionListener( OnDeviceConnectionListener listener )
    {
        mDeviceConnectionListener = listener;
    }
}
