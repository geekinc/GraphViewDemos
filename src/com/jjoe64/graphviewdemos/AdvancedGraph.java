package com.jjoe64.graphviewdemos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.jjoe64.graphview.BarGraphView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)

public class AdvancedGraph extends Activity {
	private final Handler mHandler = new Handler();
	private Runnable mTimer1;
	private Runnable mTimer2;
	private GraphView graphView1;	
	private GraphViewSeries exampleSeries1;
	private double sensorX = 0;
	private List<GraphViewData> seriesRPM;
	private String currentRPM = "0000";
	int dataCount = 1;
	
	StringBuilder inputData = new StringBuilder();

	private static final String TAG = "BEN";
    private static final boolean D = true;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private InputStream inStream = null;
    
    // Well known SPP UUID (will *probably* map to
    // RFCOMM channel 1 (default) if not in use);
    // see comments in onResume().
    private static final UUID MY_UUID =
                    UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // ==> hardcode your server's MAC address here <==
    private static String address = "00:06:66:08:10:DE";	
	

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.graphs);

		if (D)
			Log.e(TAG, "+++ ON CREATE +++");

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, 
				"Bluetooth is not available.", 
				Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		if (!mBluetoothAdapter.isEnabled()) {
			Toast.makeText(this, 
				"Please enable your BT and re-run this program.", 
				Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		
		if (D)
			Log.e(TAG, "+++ GOT LOCAL BT ADAPTER +++");
		
		
		seriesRPM = new ArrayList<GraphViewData>();
		

		// init example series data
		exampleSeries1 = new GraphViewSeries(new GraphViewData[] {});		
		if (getIntent().getStringExtra("type").equals("bar")) {
			graphView1 = new BarGraphView(
					this // context
					, "GraphViewDemo" // heading
			);
		} else {
			graphView1 = new LineGraphView(
					this // context
					, "GraphViewDemo" // heading
			);
		}
		graphView1.addSeries(exampleSeries1); // data
		LinearLayout layout = (LinearLayout) findViewById(R.id.graph1);
		layout.addView(graphView1);
	}

	/*
	@Override
	public void onSensorChanged(SensorEvent event)
	{
		//if sensor is unreliable, return void
		if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
		{
			return;
		}
		sensorX = event.values[2];
		sensorY = event.values[1];
		sensorZ = event.values[0];

		seriesX.add(new GraphViewData(dataCount, sensorX));
		seriesY.add(new GraphViewData(dataCount, sensorY));
		seriesZ.add(new GraphViewData(dataCount, sensorZ));
		
		dataCount++;
		
		
		if (seriesX.size() > 500) {
			seriesX.remove(0);
			seriesY.remove(0);
			seriesZ.remove(0);
			graphView1.setViewPort(dataCount - 500, 500);
			graphView2.setViewPort(dataCount - 500, 500);
			graphView3.setViewPort(dataCount - 500, 500);
		}
	
	}
	
*/
	
	@Override
	protected void onStop()
	{
		//unregister the sensor listener
		//sManager.unregisterListener(this);
		super.onStop();
	}
	
	@Override
	protected void onPause() {
		mHandler.removeCallbacks(mTimer1);
		super.onPause();
		
		if (D)
			Log.e(TAG, "- ON PAUSE -");

		if (outStream != null) {
			try {
				outStream.flush();
			} catch (IOException e) {
				Log.e(TAG, "ON PAUSE: Couldn't flush output stream.", e);
			}
		}

		try	{
			btSocket.close();
		} catch (IOException e2) {
			Log.e(TAG, "ON PAUSE: Unable to close socket.", e2);
		}		
	}
	

	@Override
	protected void onResume() {
		super.onResume();
		
		// When this returns, it will 'know' about the server,
		// via it's MAC address.
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

		// We need two things before we can successfully connect
		// (authentication issues aside): a MAC address, which we
		// already have, and an RFCOMM channel.
		// Because RFCOMM channels (aka ports) are limited in
		// number, Android doesn't allow you to use them directly;
		// instead you request a RFCOMM mapping based on a service
		// ID. In our case, we will use the well-known SPP Service
		// ID. This ID is in UUID (GUID to you Microsofties)
		// format. Given the UUID, Android will handle the
		// mapping for you. Generally, this will return RFCOMM 1,
		// but not always; it depends what other BlueTooth services
		// are in use on your Android device.
		try {
			btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
		} catch (IOException e) {
			Log.e(TAG, "ON RESUME: Socket creation failed.", e);
		}

		// Discovery may be going on, e.g., if you're running a
		// 'scan for devices' search from your handset's Bluetooth
		// settings, so we call cancelDiscovery(). It doesn't hurt
		// to call it, but it might hurt not to... discovery is a
		// heavyweight process; you don't want it in progress when
		// a connection attempt is made.
		mBluetoothAdapter.cancelDiscovery();

		// Blocking connect, for a simple client nothing else can
		// happen until a successful connection is made, so we
		// don't care if it blocks.
		try {
			btSocket.connect();
			Log.e(TAG, "ON RESUME: BT connection established, data transfer link open.");
		} catch (IOException e) {
			try {
				btSocket.close();
			} catch (IOException e2) {
				Log.e(TAG, 
					"ON RESUME: Unable to close socket during connection failure", e2);
			}
		}

		// Create a data stream so we can talk to server.
		if (D)
			Log.e(TAG, "+ ABOUT TO SAY SOMETHING TO SERVER +");

		try {
			inStream = btSocket.getInputStream();
		} catch (IOException e) {
			Log.e(TAG, "ON RESUME: Input stream creation failed.", e);
		}

		try {
			outStream = btSocket.getOutputStream();
		} catch (IOException e) {
			Log.e(TAG, "ON RESUME: Output stream creation failed.", e);
		}

		String message = "\n";
		byte[] msgBuffer = message.getBytes();
		try {
			outStream.write(msgBuffer);
		} catch (IOException e) {
			Log.e(TAG, "ON RESUME: Exception during write.", e);
		}

		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		message = "pi\n";
		msgBuffer = message.getBytes();
		try {
			outStream.write(msgBuffer);
		} catch (IOException e) {
			Log.e(TAG, "ON RESUME: Exception during write.", e);
		}

		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		message = "raspberry\n";
		msgBuffer = message.getBytes();
		try {
			outStream.write(msgBuffer);
		} catch (IOException e) {
			Log.e(TAG, "ON RESUME: Exception during write.", e);
		}

		
        new Thread(new Task()).start();
		
		

		mTimer1 = new Runnable() {
			@Override
			public void run() {			

				String lines[] = inputData.toString().split("\\r?\\n");
				for (int x = 0; x < lines.length; x++) {
					if (lines[x].length() == 45) { // We have a useable line here
						
						// Parse the id
						String values[] = lines[x].split("\\s+");
						boolean keepGoing = true;
						if ((values[2].compareTo("CF00400") == 0) && keepGoing) {
							currentRPM = values[8] + values[7];
							Log.i(TAG, values[8] + values[7]);
							//seriesX.add(new GraphViewData(dataCount, Integer.parseInt(values[8] + values[7])));
							//seriesRPM.add(values[8] + values[7]);
							//dataCount++;
						
//							if (seriesX.size() > 500) {
//								seriesX.remove(0);
								//graphView1.setViewPort(dataCount - 500, 500);
//							}
							keepGoing = false;							
						}
						// CFE6C00 - VSS
						// CF00300 - APP 
						// CF00400 - RPM
						// 18FEEE00 - ECT
						// 18FEF500 - ???
					}
				}
				inputData.setLength(0);
				//GraphViewData[] gvd = new GraphViewData[seriesX.size()];				
				//seriesX.toArray(gvd);
				//exampleSeries1.resetData(gvd);
				mHandler.postDelayed(this, 100);
			}
		};
		mHandler.postDelayed(mTimer1, 100);
        

		mTimer2 = new Runnable() {
			@Override
			public void run() {
				
				seriesRPM.add(new GraphViewData(dataCount, Integer.parseInt(currentRPM, 16)));
				//int value = Integer.parseInt(currentRPM, 16);
				dataCount++;
				if (seriesRPM.size() > 500) {
					seriesRPM.remove(0);
					graphView1.setViewPort(dataCount - 500, 500);
				}
				
				GraphViewData[] gvd = new GraphViewData[seriesRPM.size()];				
				seriesRPM.toArray(gvd);
				exampleSeries1.resetData(gvd);

				mHandler.postDelayed(this, 100);
			}
		};
		mHandler.postDelayed(mTimer2, 100);
		
/*				
		mTimer1 = new Runnable() {
			@Override
			public void run() {			
				GraphViewData[] gvd = new GraphViewData[seriesX.size()];				
				seriesX.toArray(gvd);
				exampleSeries1.resetData(gvd);
				mHandler.post(this); //, 100);
			}
		};
		mHandler.postDelayed(mTimer1, 100);

		mTimer2 = new Runnable() {
			@Override
			public void run() {
				
				GraphViewData[] gvd = new GraphViewData[seriesY.size()];				
				seriesY.toArray(gvd);
				exampleSeries2.resetData(gvd);

				mHandler.post(this);
			}
		};
		mHandler.postDelayed(mTimer2, 100);

	
		mTimer3 = new Runnable() {
			@Override
			public void run() {
				
				GraphViewData[] gvd = new GraphViewData[seriesZ.size()];				
				seriesZ.toArray(gvd);
				exampleSeries3.resetData(gvd);

				mHandler.post(this);
			}
		};
		mHandler.postDelayed(mTimer3, 100);
	*/
	}
	
	
	class Task implements Runnable {
		@Override
		public void run() {
			byte[] buffer = new byte[1024];
	        int bytes; // bytes returned from read()

	        /*
			try {
				inStream.read(msgBuffer);
				Log.i(TAG, msgBuffer.toString());
			} catch (IOException e) {
				Log.e(TAG, "ON RESUME: Exception during read.", e);
			}
	         */
	        
	        // Keep listening to the InputStream until an exception occurs
	        while (true) {
	            try {
	                // Read from the InputStream
	                bytes = inStream.read(buffer);
	                // Send the obtained bytes to the UI activity
	                if (bytes > 0) {
	                	String v = new String( Arrays.copyOfRange(buffer, 0, bytes), Charset.forName("UTF-8") );
	                	inputData.append(v);
	                	//Log.i(TAG, v);
	                	
	                }
	                //mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
	                //        .sendToTarget();
	            } catch (IOException e) {
	            	Log.e(TAG, "**********READ EXCEPTION***********");
	                break;
	            }
	        }			
/*			
			for (int i = 0; i <= 10; i++) {
				final int value = i;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				//bar.setProgress(value);
*/
			
		}

	}	
}
