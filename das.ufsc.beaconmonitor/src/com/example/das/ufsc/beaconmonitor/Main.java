package com.example.das.ufsc.beaconmonitor;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;


public class Main extends Activity 
{
	private Manager manager;
	private Button turnOn;
	private Button disconnect;
	private TextView statusUpdate;
	private TextView beaconInfo;
	private TextView stopInfo;
	private TextView errorMessage;
	private TextView nextCallInfo;
	private TextView missedCalls;

	private CheckBox cbDubious;
	
	BroadcastReceiver bluetoothState = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{
			String stateExtra = BluetoothAdapter.EXTRA_STATE;
			int state = intent.getIntExtra(stateExtra, -1);
			switch(state)
			{
			case(BluetoothAdapter.STATE_TURNING_ON):
			{
				//showToast("Bluetooth turning On");
				break;
			}
			case(BluetoothAdapter.STATE_ON):
			{
				//showToast("Bluetooth On");
				manager.onBluetoothOn();
				break;
			}
			case(BluetoothAdapter.STATE_TURNING_OFF):
			{
				//showToast("Bluetooth turning Off");
				break;
			}
			case(BluetoothAdapter.STATE_OFF):
			{
				//showToast("Bluetooth Off");
				hideBluetoothProperties();
				break;
			}
			}
			
		};
	};
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		this.manager = new Manager(this);
		
		setupUI();
	}
	
	
	private void setupUI() 
	{
		statusUpdate = (TextView) findViewById(R.id.result);
		beaconInfo = (TextView) findViewById(R.id.beaconInfo);
		stopInfo = (TextView) findViewById(R.id.stopInfo);
		errorMessage = (TextView) findViewById(R.id.errorMessage);
		missedCalls = (TextView) findViewById(R.id.missedCalls);
		nextCallInfo = (TextView) findViewById(R.id.nextCallInfo);
		
		turnOn = (Button) findViewById(R.id.turnonBtn);
		disconnect = (Button) findViewById(R.id.disconnectBtn);		
		//exit = (Button) findViewById(R.id.exitBtn);		
		cbDubious = (CheckBox)findViewById(R.id.cbDubious);		
		
		disconnect.setVisibility(View.GONE);
		cbDubious.setVisibility(View.GONE);
		beaconInfo.setVisibility(View.GONE);
		stopInfo.setVisibility(View.GONE);

		errorMessage.setVisibility(View.GONE);
		errorMessage.setTextColor(Color.RED);
		errorMessage.setMovementMethod(new ScrollingMovementMethod());
		
		
		missedCalls.setVisibility(View.GONE);
		missedCalls.setTextColor(Color.BLACK);
		
		nextCallInfo.setVisibility(View.GONE);
		nextCallInfo.setTextColor(Color.BLUE);
		nextCallInfo.setText("");
		
		//cria actionListeners para o botao turnOn
		turnOn.setOnClickListener(
			new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					//registra no monitor de estados a action para ligar o bluetooth
					String actionStateChanged = BluetoothAdapter.ACTION_STATE_CHANGED;
					IntentFilter filter = new IntentFilter(actionStateChanged);
					registerReceiver(bluetoothState, filter);
					
					manager.turnOnBluetooth();
				}
			}
		);


		
		disconnect.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				manager.stopBeacon();
				disconnect.setVisibility(View.GONE);
				cbDubious.setVisibility(View.GONE);
				beaconInfo.setVisibility(View.GONE);
				stopInfo.setVisibility(View.GONE);
				errorMessage.setText("");
				errorMessage.setVisibility(View.GONE);
				
				missedCalls.setText("");
				missedCalls.setVisibility(View.GONE);
				
				turnOn.setVisibility(View.VISIBLE);
				
				nextCallInfo.setText("");
				nextCallInfo.setVisibility(View.GONE);
			}
		});
		
		
		
		/*exit.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				manager.stopBeacon();

				finish();
			}
		});*/
		
		
		cbDubious.setOnClickListener(new OnClickListener() 
		{
			
			@Override
			public void onClick(View v) 
			{
				manager.setDubiousMode(cbDubious.isChecked());			
			}
		});
		
		//register the broadcast receiver to handle the action found
		//every time a device is found it calls the discoveryResult's method onReceive 
		registerReceiver(discoveryResult, new IntentFilter(BluetoothDevice.ACTION_FOUND));
	}
	
	BroadcastReceiver discoveryResult = new BroadcastReceiver()
	{
		/**
		 * onReceive is called for every device found on inquiry procedure while the discovery is running
		 */
		@Override
		public void onReceive(Context context, Intent intent)
		{
			BluetoothDevice remoteDevice = intent.getParcelableExtra( BluetoothDevice.EXTRA_DEVICE );
			manager.connect(remoteDevice);
		}
	};
	
	
	public void showError(Exception e)
	{
		CharSequence oldMsg = this.errorMessage.getText();
		String msg = "Error: " + e.getMessage() + "\nST: ";
		for(StackTraceElement stel :  e.getStackTrace())
		{
			msg = msg + " " + stel.toString() + "\n";
		}
		msg = msg + "\n" + oldMsg;
		this.errorMessage.setText(msg);
		this.errorMessage.setVisibility(View.VISIBLE);
	}
	
	
	public void showMissedCalls(int missedCalls)
	{
		final String msg = "Missed calls: " + missedCalls;
		
		final TextView text = (TextView) Main.this.findViewById(R.id.missedCalls);
		Main.this.runOnUiThread(new Runnable() 
        {
             public void run() 
             {
            	 text.setText(msg);
            	 text.setVisibility(View.VISIBLE);
             }
        });
	}
	
	
	
	public void showWarning(String msg)
	{
		this.errorMessage.setText(msg);
		this.errorMessage.setVisibility(View.VISIBLE);
	}
	
	
	public void showBeaconInfo(String msg)
	{
		beaconInfo.setText(msg);
		beaconInfo.setVisibility(View.VISIBLE);
	}
	

	public void showStopInfo(String lastStop) 
	{
		stopInfo.setText(lastStop);
		stopInfo.setVisibility(View.VISIBLE);
	}
	

	public void showBluetoothProperties(String statusText)
	{
		statusUpdate.setText(statusText);
		disconnect.setVisibility(View.VISIBLE);
		//cbDubious.setVisibility(View.VISIBLE);
		turnOn.setVisibility(View.GONE);
	}
	
	
	public void showNextCallInfo(final String msg)
	{
		final TextView text = (TextView) Main.this.findViewById(R.id.nextCallInfo);
		Main.this.runOnUiThread(new Runnable() 
        {
             public void run() 
             {
            	 text.setText(msg);
            	 text.setVisibility(View.VISIBLE);
             }
        });
	}
	
	
	public void showError(final String msg)
	{
		final TextView text = (TextView) Main.this.findViewById(R.id.errorMessage);
		Main.this.runOnUiThread(new Runnable() 
        {
             public void run() 
             {
            	 text.setText(msg);
            	 text.setVisibility(View.VISIBLE);
             }
        });
	}

	
	public void hideBluetoothProperties() 
	{
		statusUpdate.setText("Bluetooth is not on");
		disconnect.setVisibility(View.GONE);
		cbDubious.setVisibility(View.GONE);
		turnOn.setVisibility(View.VISIBLE);
	}
	

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
