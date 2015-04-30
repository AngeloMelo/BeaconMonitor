package com.example.das.ufsc.beaconmonitor;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

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
				
				ui.showToast("conectado ao beacon " );
				
				break;
			}
			}
		}

		
	};
	
	private ScheduledExecutorService executor;
	

	
	public Manager(Main uiRef)
	{
		super();
		
		this.ui = uiRef;
		
		//obtem a interface para o hardware bluetooth do dispositivo
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		comunicationService = new CommunicationService(mHandler);
	}


	private void readTic(String readMessage) 
	{
		if(readMessage != null && readMessage.contains("tic"))
		{
			ui.showToast("msg recebida: " +readMessage);
			//interrmpe conexao
			try {
				comunicationService.sendMessage("ack");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			String sTic = readMessage.replace("tic:", "");
			int tic = Integer.valueOf(sTic);
			
			prepareNewCall(tic);
		}
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
		
		//executor.shutdownNow();
		
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
