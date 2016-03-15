package app.ble.jean.scanning;

import android.bluetooth.BluetoothDevice;
import android.util.*;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private String TAG = "Main";

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner mLeScanner;
    private ScanSettings mLeScanSettings;
    private List<ScanFilter> mScanFilter = new ArrayList<>();

    private Handler mHandler;

    private ListView mResultList;
    private LeDeviceListAdapter mLeDeviceListAdapter;

    // stop scanning after 30 seconds
    private static final long SCAN_PERIOD = 30000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mResultList = (ListView)findViewById(R.id.lv_scan_resut);

        // Initialize Bluetooth adapter
        bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);

        bluetoothAdapter = bluetoothManager.getAdapter();
        mLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        mLeDeviceListAdapter = new LeDeviceListAdapter();
        mResultList.setAdapter(mLeDeviceListAdapter);

        // setDelayReport not supported, therefore, can't get batch results
        mLeScanSettings = new ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .build();

        mHandler = new Handler();
        scanLeDevice(true);
    }



    private void scanLeDevice(final boolean enable){
        if (enable){
            // stops scanning after a pre-defined scan period
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    mLeScanner.stopScan(mScanCallback);
                }
            }, SCAN_PERIOD);


            mLeScanner.startScan(mScanFilter, mLeScanSettings, mScanCallback);

        }else{

                mLeScanner.stopScan(mScanCallback);


        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);

        if(mLeDeviceListAdapter != null)
            mLeDeviceListAdapter.clear();
    }



    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.d(TAG, "onScanResult");

            Log.d(TAG, result.getDevice().getName() + ", " + result.getRssi());

            mLeDeviceListAdapter.addResult(result);
            mLeDeviceListAdapter.notifyDataSetChanged();

        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d(TAG, "onScanFailed with errorCode["+errorCode+"]");
        }
    };


    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRSSI;
    }


    // Adapter for holding devices found through scanning
    private class LeDeviceListAdapter extends BaseAdapter{
//        private HashMap<BluetoothDevice, String> device_rssi = new HashMap<BluetoothDevice, String>();
        private ArrayList<ScanResult> mLeResult;
        private ArrayList<BluetoothDevice> mLeDevice;
        private LayoutInflater inflater;

        public LeDeviceListAdapter() {
//            super();  // do we need this?

            // initialize
            mLeResult = new ArrayList<ScanResult>();        // RSSI data source
            mLeDevice = new ArrayList<BluetoothDevice>();   // devices data
            inflater = MainActivity.this.getLayoutInflater();
        }

        public void addResult(ScanResult result){

            BluetoothDevice device = result.getDevice();

            if (mLeDevice.contains(device)){
                int pre_index = mLeDevice.indexOf(device);
                // update RSSI value, avoid bouncing 
                if (Math.abs(result.getRssi() -mLeResult.get(pre_index).getRssi()) > 3){
                    mLeResult.set(pre_index, result);
                    Log.d(TAG, "update rssi index["+pre_index+"], rssi["+result.getRssi()+"]");
                }


            }else{
                Log.d(TAG, "mLeResult.add(result);");
                mLeResult.add(result);
                mLeDevice.add(device);
            }
        }

        public void addDevice(BluetoothDevice device){
            mLeDevice.add(device);
        }

//        private ScanResult getResult(int position){ return mLeResult.get(position)};

        public void clear(){mLeResult.clear();};

        @Override
        public int getCount(){ return mLeResult.size();};

        @Override
        public Object getItem(int position) {
            return mLeResult.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        // when is this method get called?
        @Override
        public View getView(int position, View view, ViewGroup parent) {
            Log.d(TAG, "getView launched");

            ViewHolder viewHolder;

            // when sliding, if there is a data without view?
            if (view == null){
                view = inflater.inflate(R.layout.listview_item, null);

                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView)view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView)view.findViewById(R.id.device_name);
                viewHolder.deviceRSSI = (TextView)view.findViewById(R.id.device_rssi);

                view.setTag(viewHolder);
            }else{
                viewHolder = (ViewHolder)view.getTag();
            }

            ScanResult result = mLeResult.get(position);
            BluetoothDevice device = result.getDevice();
            String deviceName = device.getName();

            if(deviceName != null && deviceName.length()>0){
                viewHolder.deviceName.setText(deviceName);
            }else{
                viewHolder.deviceName.setText("Unknown Device");
            }

            viewHolder.deviceAddress.setText(device.getAddress());
            viewHolder.deviceRSSI.setText(String.valueOf(result.getRssi()));

            return view;
        }
    }




}
