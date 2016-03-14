package com.example.glmeyer.synchronizedforspotify;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DeviceList extends AppCompatActivity {
BluetoothAdapter mBluetooth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
        mBluetooth = BluetoothAdapter.getDefaultAdapter();
        findPaired();


    }

    private btDeviceAdapter knownAdapter;
    private btDeviceAdapter unknownAdapter;
    private Set<BluetoothDevice> knownSet;
    private ArrayList<BluetoothDevice> knownList;
    private  ArrayList<BluetoothDevice> unknownList;

    public void findPaired(){
        knownSet = mBluetooth.getBondedDevices();
        knownList = new ArrayList<BluetoothDevice>();
        knownAdapter = new btDeviceAdapter(this, R.layout.list_device, knownList);
        ListView knownListView = (ListView)findViewById(R.id.knownlist);
        knownListView.setAdapter(knownAdapter);
        for(BluetoothDevice dev: knownSet){
            knownAdapter.add(dev);
        }

        unknownList = new ArrayList<BluetoothDevice>();
        unknownAdapter = new btDeviceAdapter(this, R.layout.list_device, unknownList);
        ListView unknownListView = (ListView)findViewById(R.id.unknownlist);
        unknownListView.setAdapter(unknownAdapter);
        mBluetooth.startDiscovery();
        final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(BluetoothDevice.ACTION_FOUND.equals(action)){
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    unknownAdapter.add(device);
                }
            }
        };
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver,filter);

        unknownListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice bluetoothDevice = unknownAdapter.getItem(position);
                Intent returnIntent = new Intent();
                returnIntent.putExtra("btMac", bluetoothDevice.getAddress());
                returnIntent.putExtra("btName", bluetoothDevice.getName());
                setResult(Activity.RESULT_OK, returnIntent);
                unregisterReceiver(mReceiver);
                mBluetooth.cancelDiscovery();
                finish();
            }
        });

        knownListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice bluetoothDevice = knownAdapter.getItem(position);
                Intent returnIntent = new Intent();
                returnIntent.putExtra("btMac", bluetoothDevice.getAddress());
                returnIntent.putExtra("btName", bluetoothDevice.getName());
                setResult(Activity.RESULT_OK, returnIntent);
                unregisterReceiver(mReceiver);
                mBluetooth.cancelDiscovery();
                finish();
            }
        });
    }

    private class btDeviceAdapter extends ArrayAdapter<BluetoothDevice> {
        // Generic overriding code (Super, etc.)
        // plus some local global variables
        int resource;
        Context context;

        public btDeviceAdapter(Context _context, int _resource, List<BluetoothDevice> knownDevice) {
            super(_context, _resource, knownDevice);
            resource = _resource;
            context = _context;
            this.context = _context;

        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            LinearLayout newView;
            BluetoothDevice w = getItem(position);
            Log.d("DevAddress: ", w.getAddress());

            // Inflate if one does not currently exist.
            if(convertView == null){
                newView = new LinearLayout(getContext());
                String inflater = Context.LAYOUT_INFLATER_SERVICE;
                LayoutInflater vi = (LayoutInflater) getContext().getSystemService(inflater);
                vi.inflate(resource, newView, true);
            } else {
                newView = (LinearLayout) convertView;
            }
            TextView devName = (TextView) newView.findViewById(R.id.devicename);
            TextView devMac = (TextView) newView.findViewById(R.id.devicemac);
            devName.setText(w.getName());
            devMac.setText(w.getAddress());

            return newView;
        }
    }

    public void makeDiscoverable(View v){
        Intent discoverIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverIntent);
    }
}
