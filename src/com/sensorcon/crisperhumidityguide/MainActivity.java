package com.sensorcon.crisperhumidityguide;

import android.annotation.SuppressLint;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import com.sensorcon.sensordrone.DroneEventListener;
import com.sensorcon.sensordrone.DroneEventObject;
import com.sensorcon.sensordrone.DroneStatusListener;
import com.sensorcon.sensordrone.android.Drone;
import com.sensorcon.sensordrone.android.tools.DroneConnectionHelper;
import com.sensorcon.sensordrone.android.tools.DroneQSStreamer;

@SuppressLint("NewApi")
public class MainActivity extends Activity {

	/*
	 * GUI Variables
	 */
	private ImageButton button1;
	private ImageButton button2;
	private ImageButton button3;
	private ImageButton xButton;
	private ImageButton xButton2;
	private PopupWindow popup;
	private PopupWindow instPopup;
	private TextView tv_humidityVal;
	private TextView tv_tempVal;
	private TextView tv_noConnect;
	private TextView tv_popupDescription;
	private TextView tv_popupHumidity;
	private TextView tv_popupExamples;
	private TextView link;
	public AlertInfo myInfo;
	/*
	 * IO variables
	 */
	PreferencesStream pStream;
	/*
	 * Data variables
	 */
	private int humidityVal;
	private int tempVal_F;
	private int tempVal_C;
	/*
	 * Sensordone variables
	 */
	protected Drone myDrone;
	public DroneConnectionHelper myHelper;
	private Handler myHandler = new Handler();


    // Holds the sensor of interest - the CO precision sensor
    public int sensor1;
    public int sensor2;

    // Our Listeners
    public DroneEventListener droneEventListener;
    public DroneStatusListener droneStatusListener;
    public String MAC = "";

    // GUI variables
    public TextView statusView;
    public TextView tvConnectionStatus;
    public TextView tvConnectInfo;

    // Streams data from sensor
    public DroneQSStreamer streamer1;
    public DroneQSStreamer streamer2;




	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		popup = new PopupWindow(this);

		humidityVal = 0;
		tempVal_F = 0;
		tempVal_C = 0;
		tv_humidityVal = (TextView)findViewById(R.id.humidityValue);
		tv_tempVal = (TextView)findViewById(R.id.tempValue);
		tv_noConnect = (TextView)findViewById(R.id.noConnect);
		button1 = (ImageButton)findViewById(R.id.button1);
		button2 = (ImageButton)findViewById(R.id.button2);
		button3 = (ImageButton)findViewById(R.id.button3);
		
		tv_tempVal.setVisibility(View.GONE);
		tv_humidityVal.setVisibility(View.GONE);

		LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View layout = inflater.inflate(R.layout.popup,
				(ViewGroup) findViewById(R.id.popup_element));

		int popupW;
		
		if((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE) {
			popupW = 700;
		}
		else if((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_SMALL) {
			popupW = 300;
		}
		else {
			popupW = 350;
		}
		
		popup = new PopupWindow(
				layout, 
				popupW, 
				LayoutParams.WRAP_CONTENT,
				true);

		LayoutInflater inflater2 = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View layout2 = inflater2.inflate(R.layout.inst_popup,
				(ViewGroup) findViewById(R.id.popup_element));
		
		link = (TextView)layout2.findViewById(R.id.inst2);
		
		instPopup = new PopupWindow(
				layout2, 
				LayoutParams.MATCH_PARENT, 
				LayoutParams.WRAP_CONTENT, 
				true);
		
		tv_popupHumidity = (TextView)layout.findViewById(R.id.popupHumidity);
		tv_popupDescription = (TextView)layout.findViewById(R.id.popupDescription);
		tv_popupExamples = (TextView)layout.findViewById(R.id.popupExamples);
		
		xButton = (ImageButton)layout.findViewById(R.id.xButton);
		xButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				popup.dismiss();
			}	
		});
		
		xButton2 = (ImageButton)layout2.findViewById(R.id.xButton);
		xButton2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				instPopup.dismiss();
			}	
		});

		button1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showPopup1();
			}		
		});	
		
		button2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showPopup2();
			}		
		});	
		
		button3.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showPopup3();
			}		
		});	
		

		myDrone = new Drone();
		myHelper = new DroneConnectionHelper();
		
		myInfo = new AlertInfo(this);
		
		// Check to see if user still wants intro screen to show
		pStream = new PreferencesStream();
		pStream.initFile(this);
		String[] preferences = new String[1];
		preferences = pStream.readPreferences();
		
		if(!preferences[0].equals("DISABLE INTRO")){
			showIntroDialog();
		}

        // Initialize sensor
        sensor1 = myDrone.QS_TYPE_HUMIDITY;
        sensor2 = myDrone.QS_TYPE_TEMPERATURE;


        streamer1 = new DroneQSStreamer(myDrone, sensor1);
        streamer2 = new DroneQSStreamer(myDrone, sensor2);


        droneEventListener = new DroneEventListener() {

            @Override
            public void connectEvent(DroneEventObject arg0) {


                quickMessage("Connected!");

                streamer1.enable();
                streamer2.enable();
                myDrone.quickEnable(sensor1);
                myDrone.quickEnable(sensor2);


                myDrone.setLEDs(0,0,126);

                tv_tempVal.setVisibility(View.VISIBLE);
                tv_humidityVal.setVisibility(View.VISIBLE);
                tv_noConnect.setVisibility(View.GONE);

                myHandler.post(displayHumidityRunnable);
            }


            @Override
            public void connectionLostEvent(DroneEventObject arg0) {


                quickMessage("Connection lost! Trying to re-connect!");

                // Try to reconnect once, automatically
                if (myDrone.btConnect(myDrone.lastMAC)) {
                    // A brief pause
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                } else {
                    quickMessage("Re-connect failed");
                    doOnDisconnect();
                }
            }

            @Override
            public void disconnectEvent(DroneEventObject arg0) {
                quickMessage("Disconnected!");
            }

            @Override
            public void humidityMeasured(DroneEventObject arg0) {
                humidityVal = (int)myDrone.humidity_Percent;
                Log.d("chris", Integer.toString(humidityVal));

                streamer1.streamHandler.postDelayed(streamer1, 250);
            }

            @Override
            public void temperatureMeasured(DroneEventObject arg0) {
                tempVal_F = (int)myDrone.temperature_Fahrenheit;
                tempVal_C = (int)myDrone.temperature_Celsius;
                Log.d("chris", Integer.toString(humidityVal));

                streamer2.streamHandler.postDelayed(streamer2, 250);
            }

            /*
             * Unused events
             */
            @Override
            public void customEvent(DroneEventObject arg0) {}
            @Override
            public void adcMeasured(DroneEventObject arg0) {}
            @Override
            public void precisionGasMeasured(DroneEventObject arg0) {}
            @Override
            public void altitudeMeasured(DroneEventObject arg0) {}
            @Override
            public void capacitanceMeasured(DroneEventObject arg0) {}
            @Override
            public void i2cRead(DroneEventObject arg0) {}
            @Override
            public void irTemperatureMeasured(DroneEventObject arg0) {}
            @Override
            public void oxidizingGasMeasured(DroneEventObject arg0) {}
            @Override
            public void pressureMeasured(DroneEventObject arg0) {}
            @Override
            public void reducingGasMeasured(DroneEventObject arg0) {}
            @Override
            public void rgbcMeasured(DroneEventObject arg0) {}
            @Override
            public void uartRead(DroneEventObject arg0) {}
            @Override
            public void unknown(DroneEventObject arg0) {}
            @Override
            public void usbUartRead(DroneEventObject arg0) {}
        };

			/*
			 * Set up our status listener
			 *
			 * see adcStatus for the general flow for sensors.
			 */
        droneStatusListener = new DroneStatusListener() {

            @Override
            public void humidityStatus(DroneEventObject arg0) {
                streamer1.run();
            }
            @Override
            public void temperatureStatus(DroneEventObject arg0) {
                streamer2.run();
            }

            /*
             * Unused statuses
             */
            @Override
            public void adcStatus(DroneEventObject arg0) {}
            @Override
            public void altitudeStatus(DroneEventObject arg0) {}
            @Override
            public void batteryVoltageStatus(DroneEventObject arg0) {}
            @Override
            public void capacitanceStatus(DroneEventObject arg0) {}
            @Override
            public void chargingStatus(DroneEventObject arg0) {}
            @Override
            public void customStatus(DroneEventObject arg0) {}
            @Override
            public void precisionGasStatus(DroneEventObject arg0) {}
            @Override
            public void irStatus(DroneEventObject arg0) {}
            @Override
            public void lowBatteryStatus(DroneEventObject arg0) {}
            @Override
            public void oxidizingGasStatus(DroneEventObject arg0) {}
            @Override
            public void pressureStatus(DroneEventObject arg0) {}
            @Override
            public void reducingGasStatus(DroneEventObject arg0) {}
            @Override
            public void rgbcStatus(DroneEventObject arg0) {}
            @Override
            public void unknownStatus(DroneEventObject arg0) {}
        };

        // Register the listeners
        myDrone.registerDroneEventListener(droneEventListener);
        myDrone.registerDroneStatusListener(droneStatusListener);

	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (isFinishing()) {
			// Try and nicely shut down
			doOnDisconnect();
			// A brief delay
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// Unregister the listener
			myDrone.unregisterDroneEventListener(droneEventListener);
			myDrone.unregisterDroneStatusListener(droneStatusListener);

		} else { 
			//It's an orientation change.
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
		case R.id.disconnect:
			// Only disconnect if it's connected
			if (myDrone.isConnected) {
				// Run our routine of things to do on disconnect
				doOnDisconnect();
			} else {
			}
			break;
		case R.id.connect:
			myHelper.scanToConnect(myDrone, MainActivity.this , this, false);
			break;
		case R.id.instructions:
			showInstructions();
			break;
		case R.id.reconnect:
			if (!myDrone.isConnected) {
				// This option is used to re-connect to the last connected MAC
				if (!myDrone.lastMAC.equals("")) {
					if (!myDrone.btConnect(myDrone.lastMAC)) {
						myInfo.connectFail();
					}
				} else {
					// Notify the user if no previous MAC was found.
					quickMessage("Last MAC not found... Please scan");
				} 
			} else {
				quickMessage("Already connected...");
			}
			break;
		}

		return true;
	}

	/**
	 * Loads the dialog shown at startup
	 */
	public void showIntroDialog() {

		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setCancelable(false);
		alert.setTitle("Introduction").setMessage(R.string.instructions);
		alert.setPositiveButton("Don't Show Again", new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) { 
		            pStream.disableIntroDialog();
		        }
		     })
		    .setNegativeButton("Okay", new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) { 
		            // do nothing
		        }
		     }).show();
	}
	
	/**
	 * Shows a simple message on the screen
	 * 
	 * @param msg	Message to be displayed
	 */
	public void quickMessage(final String msg) {
		this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	/**
	 * Makes informative popup appear
	 */
	public void showPopup1() {
		tv_popupHumidity.setText(R.string.humidity1);
		tv_popupDescription.setText(R.string.description1);
		tv_popupExamples.setText(R.string.examples1);
		
		popup.showAtLocation(findViewById(R.id.anchor), Gravity.CENTER, 0, 0);
	}
	
	/**
	 * Makes informative popup appear
	 */
	public void showPopup2() {
		tv_popupHumidity.setText(R.string.humidity2);
		tv_popupDescription.setText(R.string.description2);
		tv_popupExamples.setText(R.string.examples2);
		
		popup.showAtLocation(findViewById(R.id.anchor), Gravity.CENTER, 0, 0);
	}
	
	/**
	 * Makes informative popup appear
	 */
	public void showPopup3() {
		tv_popupHumidity.setText(R.string.humidity3);
		tv_popupDescription.setText(R.string.description3);
		tv_popupExamples.setText(R.string.examples3);
		
		popup.showAtLocation(findViewById(R.id.anchor), Gravity.CENTER, 0, 0);
	}
	
	/**
	 * Makes informative popup appear
	 */
	public void showInstructions() {
		instPopup.showAtLocation(findViewById(R.id.anchor), Gravity.CENTER, 0, 0);
	}

	/**
	 * Things to do when drone is disconnected
	 */
	public void doOnDisconnect() {

		// Shut off any sensors that are on
		this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				tv_tempVal.setVisibility(View.GONE);
				tv_humidityVal.setVisibility(View.GONE);
				tv_noConnect.setVisibility(View.VISIBLE);

				tv_tempVal.setVisibility(View.GONE);

				// Make sure the LEDs go off
				if (myDrone.isConnected) {
					myDrone.setLEDs(0, 0, 0);
				}

				// Only try and disconnect if already connected
				if (myDrone.isConnected) {
					myDrone.disconnect();
				}
			}
		});
	}

	public Runnable displayHumidityRunnable = new Runnable() {

		@Override
		public void run() {
			if(myDrone.isConnected) {
				tv_humidityVal.setText(Integer.toString(humidityVal) + "% R.H.");
				tv_tempVal.setText(Integer.toString(tempVal_F) + (char) 0x00B0 + " F / " + Integer.toString(tempVal_C) + (char) 0x00B0 + " C");
				
				myHandler.postDelayed(this, 1000);
			}
			else {
				
			}
		}
	};
	

}