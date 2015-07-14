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
	private boolean running = false;

	
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
				
				readTic(readMessage, true);
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
				Exception errorMessage =(Exception) msg.obj;

				if(isRunning())
				{
					ui.showError(errorMessage);
					callWaiterThread = new CallWaiter(3000);
					callWaiterThread.start();
				}

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
	
	
	private boolean isRunning()
	{
		return this.isRunning();
	}
	
	
	private void init()
	{
		this.beaconMac = null;
		this.startDiscoveryTS = null;
		this.running = false;
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
					
					if(tic < 0)
					{
						comunicationService.shutDown();
						//btAdapter.disable();
						//btAdapter.enable();
					}
					else
					{
						int lineId = json.getInt(BeaconDefaults.TIC_LINEID_KEY);
						String lineName = json.getString(BeaconDefaults.TIC_LINENM_KEY);
						String lastStop = json.getString(BeaconDefaults.TIC_LASTSTOPNM_KEY);
						
						this.ui.showBeaconInfo("Following Beacon for line " + lineName +"(" + lineId + ")");
						this.ui.showStopInfo(lastStop);
						
						//interrmpe conexao
						sendAckMessage();
						
						ui.showNextCallInfo("Next call in " + tic + "s");
						prepareNewCall(tic);						
					}
					
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
	
	private void readTic(String msgRead, boolean noCall) 
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
					
					int lineId = json.getInt(BeaconDefaults.TIC_LINEID_KEY);
					String lineName = json.getString(BeaconDefaults.TIC_LINENM_KEY);
					String lastStop = json.getString(BeaconDefaults.TIC_LASTSTOPNM_KEY);
					
					this.ui.showBeaconInfo("Following Beacon for line " + lineName +"(" + lineId + ")");
					this.ui.showStopInfo(lastStop);
					
					//interrmpe conexao
					sendAckMessage();
					
					if(tic > 0)
					{
						ui.showNextCallInfo("Next call in " + tic + "s");
						prepareNewCall(tic);						
					}
					else
					{
						ui.showNextCallInfo("Final stop");
					}
					
				}
			} 
			catch (JSONException e) 
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
			
			//new BTInterruptor().start();
		} 
		catch (IOException e) 
		{
			ui.showError(e);
		}

	}


	//TODO build this method
	private int getOppMode() 
	{
		return this.operationMode;
	}


	private void prepareNewCall(int tic)
	{
		this.callWaiterThread = new CallWaiter(tic * 1000);
		this.callWaiterThread.start();
		
		//BTInterruptor interrupt = new BTInterruptor();
		//interrupt.start();
	}
	
	
	private void callBeacon()
	{
		if( !btAdapter.isEnabled())
		{
			btAdapter.enable();
		}
		
		if(beaconMac == null) return;
		
		BluetoothDevice beaconDevice = btAdapter.getRemoteDevice(beaconMac);
		try 
		{
			ConnectionPerformanceInfo connPerformanceInfo = this.currentConnectedBeacons.get(beaconMac);
			connPerformanceInfo.setLastConnRequestTS(new Date());
			this.comunicationService.connect(beaconDevice);
		} 
		catch (IOException e) 
		{
			ui.showError(e);
		}
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
			//comunicationService.stop();
			
			//turn off bluetooth
			btAdapter.disable();
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
			try 
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
				comunicationService.connect(remoteDevice);
				this.ui.showBeaconInfo("Beacon found...");
			} 
			catch (IOException e)
			{
				ui.showError(e);
			}	
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
	
	private class BTInterruptor extends Thread
	{
		public void run()
		{
			try 
			{
				sleep(3 * 1000);
			} 
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
	
			btAdapter.disable();
			btAdapter.enable();
		}
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
