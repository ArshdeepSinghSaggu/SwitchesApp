package com.example.arsh.switchesapp;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    TextView connectionStatus;
    Button button1,button2,button3,button4,button5;
    BluetoothAdapter myBluetooth;
    BluetoothSocket btSocket;
    final String BOARD_NAME = "SWITCH_BOARD";
    String hardwareName = null;
    String hardwareAddress = null;
    UUID myUUID = UUID.fromString("0000110a-0000-1000-8000-00805F9B34FB");




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        connectionStatus = findViewById(R.id.connectionText);
        myBluetooth = BluetoothAdapter.getDefaultAdapter();
        button1 = (Button) findViewById(R.id.button1);
        button2 = (Button) findViewById(R.id.button2);
        button3 = (Button) findViewById(R.id.button3);
        button4 = (Button) findViewById(R.id.button4);
        button5 = (Button) findViewById(R.id.button5);


        Method getUuidsMethod = null;
        try {
            getUuidsMethod = BluetoothAdapter.class.getDeclaredMethod("getUuids", null);

            ParcelUuid[] uuids = (ParcelUuid[]) getUuidsMethod.invoke(myBluetooth, null);

            for (ParcelUuid uuid : uuids) {
                Log.d("SWTCHES", "UUID: " + uuid.getUuid().toString());
            }

            myUUID = uuids[0].getUuid();

        } catch (Exception e) {
            e.printStackTrace();
        }


        if (myBluetooth == null) {
            //Show a mensag. that thedevice has no bluetooth adapter
            Toast.makeText(getApplicationContext(), "Bluetooth Device Not Available", Toast.LENGTH_LONG).show();
            //finish apk
            finish();
        } else {
            if (myBluetooth.isEnabled()) {

            } else {
                Log.i("BLUETOOTH LOG", "onCreate: BLUETOOTH NOT ENABLE");
                //Ask to the user turn the bluetooth on
                Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(turnBTon, 1);
            }
        }


    }

    public void buttonClicked(View view) {
        Button btn = ((Button) view);
        String tagName = btn.getTag().toString();

        if (!tagName.equals("T5")) {
            if (btn.getText().equals("Off")) {
                btn.setText("On");

                if (btSocket != null) {
                    try {
                        btSocket.getOutputStream().write(tagName.getBytes());
                    } catch (IOException e) {
                        Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                    }
                }

            } else {
                btn.setText("Off");
                if (btSocket != null) {
                    try {
                        btSocket.getOutputStream().write((tagName + "O").getBytes());
                    } catch (IOException e) {
                        Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                    }
                }
            }

        }else
        {
            if (btn.getText().equals("Off"))
            {
                btn.setText("On");
                button1.setText("On");
                button2.setText("On");
                button3.setText("On");
                button4.setText("On");
                if (btSocket != null) {
                    try {
                        btSocket.getOutputStream().write("T1".getBytes());
                        btSocket.getOutputStream().write("T2".getBytes());
                        btSocket.getOutputStream().write("T3".getBytes());
                        btSocket.getOutputStream().write("T4".getBytes());
                        btSocket.getOutputStream().write(tagName.getBytes());
                    } catch (IOException e) {
                        Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            else
            {
                btn.setText("Off");
                button1.setText("Off");
                button2.setText("Off");
                button3.setText("Off");
                button4.setText("Off");
                if (btSocket != null) {
                    try {
                        btSocket.getOutputStream().write("T1O".getBytes());
                        btSocket.getOutputStream().write("T2O".getBytes());
                        btSocket.getOutputStream().write("T3O".getBytes());
                        btSocket.getOutputStream().write("T4O".getBytes());
                        btSocket.getOutputStream().write((tagName+"O").getBytes());
                    } catch (IOException e) {
                        Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }



    public void bluetoothSetting(View view) {
        Log.i("LOG", "bluetoothSetting: BUTTON CLICKED");
        startActivity(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS));

    }

    public void connect(View view) {
        Set<BluetoothDevice> pairedDevices = myBluetooth.getBondedDevices();

        if (pairedDevices.size() > 0) {
            String deviceName = null;
            BluetoothDevice deviceUnique = null;
            String deviceHardwareAddress = null;
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                deviceName = device.getName();
                deviceHardwareAddress = device.getAddress(); // MAC address

                if (deviceName.equals(BOARD_NAME)) {
                    deviceUnique = device;
                    hardwareName = deviceName;
                    hardwareAddress = deviceHardwareAddress;
                }
            }

            ConnectThread connectThread = new ConnectThread(deviceUnique);
            connectThread.start();


        } else {
            Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
        }
    }


    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createInsecureRfcommSocketToServiceRecord(myUUID);
            } catch (IOException e) {
                Log.e("SWITCHES", "Socket's create() method failed", e);
                e.printStackTrace();
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            myBluetooth.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                connectException.printStackTrace();

                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e("switches", "Could not close the client socket", closeException);
                    closeException.printStackTrace();
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
//            manageMyConnectedSocket(mmSocket);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("Switches", "Could not close the client socket", e);
            }
        }
    }
}


