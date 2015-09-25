package com.example.das.ufsc.beaconmonitor;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;

import com.example.das.ufsc.beaconmonitor.utils.BeaconDefaults;


public class Manager 
{
	private BluetoothAdapter btAdapter;
	private CommunicationService comunicationService;
	private Main ui;
	
	private String beaconMac;
	
	private Date startDiscoveryTS;
	private Map<String, ConnectionPerformanceInfo> currentConnectedBeacons;
	
	private int operationMode;
	private CallWaiter callWaiterThread;
	private volatile boolean running = false;
	private int attempts = 0;
	private int missedCalls = 0;

	
	private final Handler mHandler = new Handler() 
	{
		@Override
		public synchronized void handleMessage(Message msg) 
		{
			switch(msg.what)
			{
			case CommunicationService.MSG_TYPE_MESSAGE_READ:
			{
				byte[] readBuf = (byte[]) msg.obj;
				
				// construct a string from the valid bytes in the buffer
				String readMessage = new String(readBuf, 0, msg.arg1);
				
				readTic(readMessage);
				break;
			}
			case CommunicationService.MSG_TYPE_CONNECTED_TO_BEACON:
			{
				beaconMac =(String) msg.obj;
				setConnectionDate(beaconMac);
				
				break;
			}
			case CommunicationService.MSG_TYPE_EXCEPTION:
			{
				Exception errorMessage =(Exception) msg.obj;
				ui.showError(errorMessage);
				break;
			}
			case CommunicationService.MSG_TYPE_CONNECT_EXCEPTION:
			{
				onConnectionException(msg);

				break;
			}
			
			case CommunicationService.MSG_TYPE_CONNECTION_CLOSED:
			{
				//ui.showError(new Exception("Connection reset by peer"));
				
				break;
			}


			}
		}

	};
	
	
	public Manager(Main uiRef)
	{
		super();
		
		this.ui = uiRef;
		
		//obtem a interface para o hardware bluetooth do dispositivo
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		comunicationService = new CommunicationService(mHandler);
		init();
	}
	
	
	
	private void init()
	{
		this.beaconMac = null;
		this.startDiscoveryTS = null;
		this.running = false;
		this.attempts = 0;
		this.missedCalls = 0;
		this.currentConnectedBeacons = new HashMap<String, ConnectionPerformanceInfo>();
		this.operationMode = BeaconDefaults.OPP_MODE_AUTHENTIC;
	}


	
	private void readTic(String msgRead) 
	{
		if(msgRead != null)
		{						
			try 
			{
				JSONObject json = new JSONObject(msgRead);
				if(json.has(BeaconDefaults.TIC_KEY))
				{
					ConnectionPerformanceInfo connPerformanceInfo = this.currentConnectedBeacons.get(beaconMac);
					connPerformanceInfo.setLastTicReceivedTs(new Date());
					
					int tic = json.getInt(BeaconDefaults.TIC_KEY);
					
					if(tic == BeaconDefaults.INT_CLOSE_CONNECTION)
					{
						comunicationService.shutDown();
					}
					else 
					{
						int lineId = json.getInt(BeaconDefaults.TIC_LINEID_KEY);
						String lineName = json.getString(BeaconDefaults.TIC_LINENM_KEY);
						String lastStop = json.getString(BeaconDefaults.TIC_LASTSTOPNM_KEY);
						
						this.ui.showBeaconInfo("Following Beacon for line " + lineName +"(" + lineId + ")");
						this.ui.showStopInfo(lastStop);
						this.ui.showWarning("");
					
						if(tic == BeaconDefaults.INT_NO_RECALL)
						{
							ui.showNextCallInfo("Final stop");
						}
						else
						{
							ui.showNextCallInfo("Next call in " + tic + "s");
							prepareNewCall(tic);
						}
					}
					
					//send command to close this connection on the peer
					sendAckMessage();
				}
			} 
			catch (JSONException e) 
			{
				ui.showError(e);
			} 
			catch (IOException e) 
			{
				ui.showError(e);
			}
		}
	}
	
	
	private void sendAckMessage() 
	{
		try 
		{
			ConnectionPerformanceInfo connPerformanceInfo = this.currentConnectedBeacons.get(beaconMac);
			connPerformanceInfo.setLastAckSentTs(new Date());
			
			String jsonString = BeaconDefaults.formatJson(btAdapter.getAddress(), getOppMode(), connPerformanceInfo);
			
			comunicationService.sendMessage(jsonString);
		} 
		catch (IOException e) 
		{
			ui.showError(e);
		}

	}


	private int getOppMode() 
	{
		return this.operationMode;
	}


	private void prepareNewCall(int tic)
	{
		if(this.callWaiterThread != null)
		{
			this.callWaiterThread.cancel();
		}
		
		this.callWaiterThread = new CallWaiter(tic * 1000);
		this.callWaiterThread.start();
		this.attempts = 0;
		//BTInterruptor interrupt = new BTInterruptor();
		//interrupt.start();
	}
	
	
	private void callBeacon()
	{
		if( !btAdapter.isEnabled())
		{
			btAdapter.enable();
		}
		
		if(beaconMac == null) 
		{
			String error = "Error: beaconMac is null ";
			//asdf
			ui.showError(error);
			return;
		}
		
		BluetoothDevice beaconDevice = btAdapter.getRemoteDevice(beaconMac);
		ConnectionPerformanceInfo connPerformanceInfo = this.currentConnectedBeacons.get(beaconMac);
		connPerformanceInfo.setLastConnRequestTS(new Date());
		this.comunicationService.connect(beaconDevice);
	}
	
	private void startDiscovery() 
	{
		//record start discovery
		this.startDiscoveryTS = new Date();
		//start discovery
		btAdapter.startDiscovery();
	}



	public void turnOnBluetooth() 
	{
		//liga bluetooth
		if (btAdapter.isEnabled()) 
		{
			btAdapter.disable(); 
		}
		btAdapter.enable(); 	
		
		init();
	}

	

	public void stopBeacon() 
	{
		this.running = false;
		try
		{
			//cancel call waiter thread
			if(this.callWaiterThread != null)
			{
				this.callWaiterThread.cancel();
			}
			
			//stop communication service
			comunicationService.shutDown();
			
			//turn off bluetooth
			btAdapter.disable();
			
			init();
		}
		catch(Exception e)
		{
			ui.showError(e);
		}
	}



	public void onBluetoothOn() 
	{
		//start communication service
		try 
		{
			//comunicationService.start();
			String name = btAdapter.getName();
			String statusText = "Device name: " + name;		
			ui.showBluetoothProperties(statusText);	
			startDiscovery();
			this.running = true;
		} 
		catch (Exception e) 
		{
			ui.showError(e);
		}
	}



	public void connect(BluetoothDevice remoteDevice) 
	{
		if(beaconMac != null) return;
		
		if(isBeacon(remoteDevice))
		{
			//records the first connection initial date
			if(!this.currentConnectedBeacons.containsKey(remoteDevice.getAddress()))
			{
				ConnectionPerformanceInfo connPerformanceInfo = new ConnectionPerformanceInfo();
				connPerformanceInfo.setStartDiscoveryTS(this.getStartDiscoveryTS());
				connPerformanceInfo.setBeaconFoundTS(new Date());
				
				this.currentConnectedBeacons.put(remoteDevice.getAddress(), connPerformanceInfo);
			}
			else
			{
				ConnectionPerformanceInfo connPerformanceInfo = this.currentConnectedBeacons.get(remoteDevice.getAddress());
				connPerformanceInfo.setStartDiscoveryTS(this.getStartDiscoveryTS());
				connPerformanceInfo.setBeaconFoundTS(new Date());
			}
			this.beaconMac = remoteDevice.getAddress();
			comunicationService.connect(remoteDevice);
			this.ui.showBeaconInfo("Beacon found...");
		}
	}


	/**
	 * checks if a remote device is a known beacon device
	 * @param remoteDevice
	 * @return
	 */
	private boolean isBeacon(BluetoothDevice remoteDevice) 
	{
		String mac = remoteDevice.getAddress();
		return BeaconDefaults.checkBeacon(mac);
	}	
	
	
	private void setConnectionDate(String beaconMac)
	{
		ConnectionPerformanceInfo connPerformanceInfo = this.currentConnectedBeacons.get(beaconMac);
		if (connPerformanceInfo == null) return;
		
		//keep the connection time
		if(connPerformanceInfo.isFirstConnection())
		{
			connPerformanceInfo.setFirstConnAcceptanceTS(new Date());
		}
		else
		{
			connPerformanceInfo.setLastConnAcceptanceTs(new Date());
		}
	}
	
	public Date getStartDiscoveryTS() 
	{
		return startDiscoveryTS;
	}
	
	
	public void setDubiousMode(boolean dubious) 
	{
		if(dubious)
		{
			this.operationMode = BeaconDefaults.OPP_MODE_DUBIOUS;
		}
		else
		{
			this.operationMode = BeaconDefaults.OPP_MODE_AUTHENTIC;
		}
	}
	
	
	private void onConnectionException(Message msg) 
	{
		Exception errorMessage = (Exception) msg.obj;
		missedCalls++;
		attempts++;
		
		String wrnMessage = "Conn refused. " + errorMessage + "\nAttempt: " + (attempts);
		ui.showWarning(wrnMessage);
		
		ui.showMissedCalls(missedCalls);
		
		if(callWaiterThread != null)
		{
			callWaiterThread.cancel();
		}
		
		//prepare a new call in 5 seconds
		callWaiterThread = new CallWaiter(5000);
		callWaiterThread.start();
	}

	
	private class CallWaiter extends Thread
	{
		private long timeToWait;
		private boolean running = true;
		
		public CallWaiter(long time)
		{
			super();
			this.timeToWait = time;
		}
		
		public synchronized void cancel()
		{
			this.running = false;
		}
		
		public void run()
		{
			while(timeToWait > 0 && this.running)
			{
				//ui.de
				ui.showNextCallInfo("Next call in " + timeToWait/1000 + "s");
				
				try 
				{
					sleep(1000);
				} 
				catch (InterruptedException e) 
				{
					e.printStackTrace();
				}
				timeToWait = timeToWait - 1000;
			}
			
		
			ui.showNextCallInfo("");
			if(this.running)
			{
				callBeacon();
			}
		}
	}

}
