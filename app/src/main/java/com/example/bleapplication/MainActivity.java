package com.example.bleapplication;

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
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.playfiblesdk.PlayFiBLESDK;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Arrays;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity
{
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int REQUEST_CHECK_SETTINGS = 2;
    private Button bStatus, mScan, mStopScan, bLocation;
    private TextView mText;
    private ListView mDeviceListView;
    private String TAG = "BLELogs";
    private PlayFiBLESDK mPlayFiBLESDK;
    private PlayFiBLESDK.OnScanDeviceFoundListener listener;
    private PlayFiBLESDK.OnDeviceConnectionListener deviceConnectionListener;
    private ArrayList< BluetoothDevice > mDeviceList;
    int mErrorCode;
    private boolean bGotAllPermissions = false;

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
        bStatus = findViewById( R.id.button );
        bLocation = findViewById( R.id.button4 );
        mScan = findViewById( R.id.button2 );
        mStopScan = findViewById( R.id.button3 );
        mText = findViewById( R.id.textView );
        mDeviceListView = findViewById( R.id.listview );

        mDeviceList = new ArrayList<>();
        ArrayAdapter< BluetoothDevice > arrayAdapter = new ArrayAdapter< BluetoothDevice >( MainActivity.this,
                                                                                            android.R.layout.simple_list_item_1,
                                                                                            android.R.id.text1, mDeviceList );
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

        bLocation.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick( View v )
            {
                enableLocation();
            }
        } );


        checkPermissionsFlow();

        // Device scan callback.


        mPlayFiBLESDK.setOnDeviceFoundListener( listener );
        mScan.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick( View view )
            {
                if( bGotAllPermissions )
                {
                    arrayAdapter.clear();
                    arrayAdapter.notifyDataSetChanged();
                    mPlayFiBLESDK.scanDevices();
                }
                else
                {
                    checkPermissionsFlow();
                }
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
        mDeviceListView.setOnItemClickListener( ( parent, view, position, id ) -> {
            BluetoothDevice device = ( BluetoothDevice ) arrayAdapter.getItem( position );
            Log.d( TAG, "onItemClick: pos = " + device.getName() );
            Log.d( TAG, "onItemClick: pos = " + device.getAddress() );
            mPlayFiBLESDK.connect( device );
        } );
    }

    private void checkPermissionsFlow()
    {
        Log.d( TAG, "checking for permissions..." );

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M )
        {
            if( ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission( this, Manifest.permission.BLUETOOTH_SCAN ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission( this, Manifest.permission.BLUETOOTH_CONNECT ) == PackageManager.PERMISSION_GRANTED )
            {
                Log.d( TAG, "All permissions are granted!" );
                bGotAllPermissions = true;
                Toast.makeText( getApplicationContext(), "All permissions are granted!", Toast.LENGTH_SHORT ).show();
            }
            else if( shouldShowRequestPermissionRationale( Manifest.permission.ACCESS_FINE_LOCATION ) ||
                    shouldShowRequestPermissionRationale( Manifest.permission.BLUETOOTH_SCAN ) ||
                    shouldShowRequestPermissionRationale( Manifest.permission.BLUETOOTH_CONNECT ) )
            {
                // In an educational UI, explain to the user why your app requires this
                // permission for a specific feature to behave as expected. In this UI,
                // include a "cancel" or "no thanks" button that allows the user to
                // continue using your app without granting the permission.
                showContextUI();
                Log.d( TAG, "Showing context UI" );
            }
            else
            {
                Log.d( TAG, "permissions not granted... already shown rationale dialog... requesting for manuel..." );
                // You can directly ask for the permission.
                if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.S )
                {
                    requestPermissions( new String[]{ Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.BLUETOOTH_SCAN,
                                                Manifest.permission.BLUETOOTH_CONNECT },
                                        PERMISSION_REQUEST_COARSE_LOCATION );

                }
                else
                {
                    requestPermissions( new String[]{ Manifest.permission.ACCESS_FINE_LOCATION },
                                        PERMISSION_REQUEST_COARSE_LOCATION );
                }
            }
        }
    }

    private void showContextUI()
    {
        final AlertDialog.Builder builder = new AlertDialog.Builder( this );
        builder.setTitle( "We need those permissions" );
        builder.setMessage( "All those permissions are required to function this app properly" );
        builder.setPositiveButton( android.R.string.ok, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialog, int which )
            {
                if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.S )
                {
                    requestPermissions( new String[]{ Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.BLUETOOTH_SCAN,
                                                Manifest.permission.BLUETOOTH_CONNECT },
                                        PERMISSION_REQUEST_COARSE_LOCATION );
                }
            }
        } );
        builder.show();
    }

    private void enableLocation()
    {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest( createLocationRequest() )
                .setNeedBle( true );
        SettingsClient client = LocationServices.getSettingsClient( this );
        Task< LocationSettingsResponse > task = client.checkLocationSettings( builder.build() );
        task.addOnSuccessListener( this, locationSettingsResponse -> {
            // All location settings are satisfied. The client can initialize
            // location requests here.
            // ...
            Log.d( TAG, "enableLocationAndBLE: Success... " );

            Toast.makeText( getApplicationContext(), "Location is enabled", Toast.LENGTH_SHORT ).show();
        } );

        task.addOnFailureListener( this, e -> {
            if( e instanceof ResolvableApiException )
            {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                Log.d( TAG, "enableLocationAndBLE: Failure... " );

                try
                {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    Toast.makeText( getApplicationContext(), "Location is NOT enabled", Toast.LENGTH_SHORT ).show();

                    ResolvableApiException resolvable = ( ResolvableApiException ) e;
                    resolvable.startResolutionForResult( MainActivity.this,
                                                         REQUEST_CHECK_SETTINGS );
                }
                catch ( IntentSender.SendIntentException sendEx )
                {
                    // Ignore the error.
                }
            }
        } );

    }

    protected LocationRequest createLocationRequest()
    {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval( 10000 );
        locationRequest.setFastestInterval( 5000 );
        locationRequest.setPriority( LocationRequest.PRIORITY_HIGH_ACCURACY );
        return locationRequest;
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
                Log.d( TAG, "onRequestPermissionsResult() called with: requestCode = [" + requestCode + "], permissions = [" + Arrays.toString( permissions ) + "], grantResults = [" + Arrays.toString( grantResults ) + "]" );
                if( grantResults[ 0 ] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[ 1 ] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[ 2 ] == PackageManager.PERMISSION_GRANTED )
                {
                    bGotAllPermissions = true;
                    Log.d( TAG, "onRequestPermissionsResult() called -- All permissions granted." );
                }
                else
                {
                    final AlertDialog.Builder builder = new AlertDialog.Builder( this );
                    builder.setTitle( "Functionality limited" );
                    builder.setMessage( "Some permissions have not been granted. please grant access to all permissions from settings." );
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