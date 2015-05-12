package com.example.das.ufsc.beaconmonitor;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.example.das.ufsc.beaconmonitor.utils.BeaconDefaults;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;


public class Manager 
{
	private BluetoothAdapter btAdapter;
	private CommunicationService comunicationService;
	private Main ui;
	private String beaconMac;
	
	private final Handler mHandler = new Handler() 
	{
		@Override
		public void handleMessage(Message msg) 
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
	}


	private void readTic(String msgRead) 
	{
		if(msgRead != null)
		{
			ui.showToast("msg recebida: " +msgRead);
			
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
			String jsonString = "{";
			jsonString = jsonString + BeaconDefaults.MAC_KEY + ":'" + btAdapter.getAddress();
			jsonString = jsonString + "'," + BeaconDefaults.OPP_MODE_KEY + ":" + getOppMode();
			jsonString = jsonString + "," + BeaconDefaults.ACK_KEY + ":'true'"; 
			jsonString = jsonString + "}";
			
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
		try 
		{
			Thread.sleep(tic * 1000);
		} 
		catch (InterruptedException e) 
		{
			e.printStackTrace();
		}
		
		//ui.showToast("ligando novamente ao beacon " );
		
		BluetoothDevice beaconDevice = btAdapter.getRemoteDevice(beaconMac);
		try 
		{
			this.comunicationService.connect(beaconDevice);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	
	private void startDiscovery() 
	{
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
		//desliga o bluetooth
		btAdapter.disable();
		
		//para o servico de comunicacao
		comunicationService.stop();
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
	
	
}
