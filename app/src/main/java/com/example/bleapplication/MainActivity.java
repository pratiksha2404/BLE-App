package com.example.bleapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.example.playfiblesdk.PlayFiBLESDK;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity
{
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private Button bStatus, mScan, mStopScan;
    private TextView mText;
    private ListView mDeviceListView;
    private String TAG = "BLELogs";
    private PlayFiBLESDK mPlayFiBLESDK;
    private PlayFiBLESDK.OnScanDeviceFoundListener listener;
    private PlayFiBLESDK.OnDeviceConnectionListener deviceConnectionListener;
    private ArrayList< BluetoothDevice > mDeviceList;
    int mErrorCode;
    BluetoothAdapter btAdapter;

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
        bStatus = findViewById( R.id.button );
        mScan = findViewById( R.id.button2 );
        mStopScan = findViewById( R.id.button3 );
        mText = findViewById( R.id.textView );
        mDeviceListView = findViewById( R.id.listview );
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        mDeviceList = new ArrayList<>();
        ArrayAdapter arrayAdapter = new ArrayAdapter( MainActivity.this, android.R.layout.activity_list_item, android.R.id.text1, mDeviceList );
        mDeviceListView.setAdapter( arrayAdapter );

        mPlayFiBLESDK = PlayFiBLESDK.getInstance();
        mPlayFiBLESDK.initializeSDK( getApplicationContext() );

//        //Check if your device support Bluetooth Low Energy
//        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
//            Log.d(TAG,"Support BLUETOOTH_LE");
//        }else{
//            Log.d(TAG,"NOT Support BLUETOOTH_LE");
//        }

        listener = new PlayFiBLESDK.OnScanDeviceFoundListener()
        {
            @Override
            public void onScanResult( BluetoothDevice device )
            {
                Log.d( TAG, "onScanResult: Device name = " + device.getName() );
                Log.d( TAG, "onScanResult: Device Address = " + device.getAddress() );
                Log.d( TAG, "onScanResult: Device uuids = " + Arrays.toString( device.getUuids() ) );
                arrayAdapter.add( device );
                arrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void onScanFailed( int errorCode )
            {
                mErrorCode = errorCode;
            }

        };

        if( !mPlayFiBLESDK.isEnable() )
        {
            Log.d( TAG, "onCreate: Bluetooth is off..!!" );
            bStatus.setEnabled( true );
            bStatus.setText( "TURN ON" );
        }
        else
        {
            Log.d( TAG, "onCreate: Bluetooth is on..!!" );
            bStatus.setEnabled( false );
        }

        bStatus.setOnClickListener( new View.OnClickListener()
        {
            @SuppressLint( "MissingPermission" )
            @Override
            public void onClick( View view )
            {
                if( !mPlayFiBLESDK.isEnable() )
                {
                    Log.d( TAG, "onClick: Bluetooth is not enabled..!!" );
                    Intent enableBtIntent = new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE );
                    startActivityForResult( enableBtIntent, 1 );
                }
                else
                {
                    Log.d( TAG, "onClick: Bluetooth is enabled..!!" );
                    bStatus.setEnabled( false );
                }
            }
        } );

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M )
        {
            if( this.checkSelfPermission( Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED )
            {
                final AlertDialog.Builder builder = new AlertDialog.Builder( this );
                builder.setTitle( "This app needs location access" );
                builder.setMessage( "Please grant location access so this app can detect peripherals." );
                builder.setPositiveButton( android.R.string.ok, null );
                builder.setOnDismissListener( new DialogInterface.OnDismissListener()
                {
                    @Override
                    public void onDismiss( DialogInterface dialog )
                    {
                        requestPermissions( new String[]{ Manifest.permission.ACCESS_FINE_LOCATION }, PERMISSION_REQUEST_COARSE_LOCATION );
                    }
                } );
                builder.show();
            }
        }

        // Device scan callback.


        mPlayFiBLESDK.setOnDeviceFoundListener( listener );
        mScan.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick( View view )
            {
                arrayAdapter.clear();
                arrayAdapter.notifyDataSetChanged();
                mPlayFiBLESDK.scanDevices();
            }
        } );

        mStopScan.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick( View view )
            {
                mPlayFiBLESDK.stopScanning();
            }
        } );

        deviceConnectionListener = new PlayFiBLESDK.OnDeviceConnectionListener()
        {
            @Override
            public void onConnectionStateChange( BluetoothGatt gatt, int status, int newState )
            {
                Log.d( TAG, "onConnectionStateChange() called with: gatt = [" + gatt + "], status = [" + status + "], newState = [" + newState + "]" );
                if( newState == BluetoothProfile.STATE_CONNECTED )
                {
                    // successfully connected to the GATT Server

                }
                else if( newState == BluetoothProfile.STATE_DISCONNECTED )
                {
                    // disconnected from the GATT Server
                }
            }

            @Override
            public void onServicesDiscovered( BluetoothGatt gatt, int status )
            {
                Log.d( TAG, "onServicesDiscovered() called with: gatt = [" + gatt + "], status = [" + status + "]" );
            }

            @Override
            public void onCharacteristicRead( BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status )
            {
                Log.d( TAG, "onCharacteristicRead() called with: gatt = [" + gatt + "], characteristic = [" + characteristic + "], status = [" + status + "]" );

                if( characteristic.getUuid().toString().equalsIgnoreCase( "0000100C-6B83-4B7E-8C82-DB4E5B93C221" ) )
                {
                    mText.setText( characteristic.getStringValue( 0 ) );
                }
            }

            @Override
            public void onCharacteristicWrite( BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status )
            {
                Log.d( TAG, "onCharacteristicWrite() called with: gatt = [" + gatt + "], characteristic = [" + characteristic + "], status = [" + status + "]" );
            }
        };

        mPlayFiBLESDK.setOnDeviceConnectionListener( deviceConnectionListener );
        mDeviceListView.setOnItemClickListener( new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick( AdapterView< ? > parent, View view, int position, long id )
            {
                BluetoothDevice device = ( BluetoothDevice ) arrayAdapter.getItem( position );
                Log.d( TAG, "onItemClick: pos = " + device.getName() );
                Log.d( TAG, "onItemClick: pos = " + device.getAddress() );
                mPlayFiBLESDK.connect( device );
            }

        } );
    }

    @Override
    public void onRequestPermissionsResult( int requestCode,
                                            String permissions[], int[] grantResults )
    {
        super.onRequestPermissionsResult( requestCode, permissions, grantResults );
        switch( requestCode )
        {
            case PERMISSION_REQUEST_COARSE_LOCATION:
            {
                if( grantResults[ 0 ] == PackageManager.PERMISSION_GRANTED )
                {
                    System.out.println( "coarse location permission granted" );
                }
                else
                {
                    final AlertDialog.Builder builder = new AlertDialog.Builder( this );
                    builder.setTitle( "Functionality limited" );
                    builder.setMessage( "Since location access has not been granted, this app will not be able to discover beacons when in the background." );
                    builder.setPositiveButton( android.R.string.ok, null );
                    builder.setOnDismissListener( new DialogInterface.OnDismissListener()
                    {

                        @Override
                        public void onDismiss( DialogInterface dialog )
                        {
                        }

                    } );
                    builder.show();
                }
                return;
            }
        }
    }

}