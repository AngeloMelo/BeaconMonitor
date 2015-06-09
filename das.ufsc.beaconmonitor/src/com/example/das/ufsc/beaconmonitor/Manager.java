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
	
	//TODO remover
	private String beaconMac;
	
	private Date startDiscoveryTS;
	private Map<String, ConnectionPerformanceInfo> currentConnectedBeacons;
	
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
				ui.showToast("conectado ao beacon ");
				
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
		
		this.currentConnectedBeacons = new HashMap<String, ConnectionPerformanceInfo>();
	}


	private void readTic(String msgRead) 
	{
		if(msgRead != null)
		{
			ui.showToast("msg recebida: " + msgRead);
			
			//interrmpe conexao
			sendAckMessage();
						
			try 
			{
				JSONObject json = new JSONObject(msgRead);
				if(json.has(BeaconDefaults.TIC_KEY))
				{
					int tic = json.getInt(BeaconDefaults.TIC_KEY);
					prepareNewCall(tic);
				}
			} 
			catch (JSONException e) 
			{
				e.printStackTrace();
			}
		}
	}
	
	
	private void sendAckMessage() 
	{
		try 
		{
			ConnectionPerformanceInfo connPerformanceInfo = this.currentConnectedBeacons.get(beaconMac);
			String jsonString = BeaconDefaults.formatJson(btAdapter.getAddress(), getOppMode(), connPerformanceInfo);
						
			comunicationService.sendMessage(jsonString);	
				
		} 
		catch (IOException e) 
		{

			e.printStackTrace();
		}

	}


	//TODO build this method
	private int getOppMode() 
	{
		return BeaconDefaults.OPP_MODE_AUTHENTIC;
	}


	private void prepareNewCall(int tic)
	{
		CallWaiter callWaiter = new CallWaiter(tic * 1000);
		callWaiter.start();
	}
	
	
	private void callBeacon()
	{
		BluetoothDevice beaconDevice = btAdapter.getRemoteDevice(beaconMac);
		try 
		{
			this.comunicationService.connect(beaconDevice);
		} 
		catch (IOException e) 
		{
			ui.showToast(e.getMessage());
		}
	}
	
	private void startDiscovery() 
	{
		//record start discovery
		this.startDiscoveryTS = new Date();
		//start discovery
		if(btAdapter.startDiscovery())
		{
			ui.showToast("Executing discovery");
		}
	}



	public void turnOnBluetooth() 
	{
		//liga bluetooth
		if (btAdapter.isEnabled()) 
		{
			btAdapter.disable(); 
		}
		btAdapter.enable(); 	
	}

	

	public void stopBeacon() 
	{
		try
		{
			//para o servico de comunicacao
			comunicationService.stop();
			
			//desliga o bluetooth
			btAdapter.disable();
		}
		catch(Exception e)
		{
			ui.showToast(e.getMessage());
		}
		
	}



	public void onBluetoothOn() 
	{
		//inicia o servico de comunicacao
		comunicationService.start();
		
		
		//obtem o endereco de hardware do dispositivo
		String address = btAdapter.getAddress();
		String name = btAdapter.getName();
		String statusText = "Device name: " + name + " MAC:" + address;		
		
		ui.showBluetoothProperties(statusText);	
		
		startDiscovery();
	}



	public void connect(BluetoothDevice remoteDevice) 
	{
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
			} 
			catch (IOException e)
			{
				ui.showToast(e.getMessage());
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
		
		//records the connection ts
		if(connPerformanceInfo.isFirstConnection())
		{
			connPerformanceInfo.setFirstConnectionTS(new Date());
		}
		else
		{
			connPerformanceInfo.setLastAcceptedConnectionTS(new Date());
		}
	}
	
	public Date getStartDiscoveryTS() 
	{
		return startDiscoveryTS;
	}
	
	private class CallWaiter extends Thread
	{
		private long timeToWait;
		
		public CallWaiter(long time)
		{
			super();
			this.timeToWait = time;
		}
		
		public void run()
		{
			try 
			{
				sleep(timeToWait);
			} 
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
		
			callBeacon();
		}
	}
}
