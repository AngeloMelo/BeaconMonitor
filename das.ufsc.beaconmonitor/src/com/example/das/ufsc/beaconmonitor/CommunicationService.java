package com.example.das.ufsc.beaconmonitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import com.example.das.ufsc.beaconmonitor.utils.BeaconDefaults;


public class CommunicationService
{
	public static final int MSG_TYPE_MESSAGE_READ = 0;
	public static final int MSG_TYPE_REFRESH_SLAVELIST = 1;
	public static final int MSG_TYPE_STOP_DISCOVERY = 2;
	public static final int MSG_TYPE_CONNECTED_TO_BEACON = 3;
	
	private ReadWriteThread mReadWriteThread;
	private ConnectThread mConnectThread;
	private BluetoothAdapter mAdapter;
	private final Handler mHandler;
	
	
	public CommunicationService(Handler handler) 
	{
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = handler;
    }
	
	
	public synchronized void start() 
	{
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mReadWriteThread != null) {mReadWriteThread.cancel(); mReadWriteThread = null;}
    }
	
	
	
	public synchronized void stop()
	{
		if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        if (mReadWriteThread != null) {mReadWriteThread.cancel(); mReadWriteThread = null;}
	}
	
	
	/**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
	public synchronized void startTransmission(BluetoothSocket socket) 
    {
		BluetoothDevice remoteDevice = socket.getRemoteDevice();
        // Cancel any thread currently running a connection
        if (mReadWriteThread != null) {mReadWriteThread.cancel(); mReadWriteThread = null;}
        
        // Start the thread to manage the connection and perform transmissions
        mReadWriteThread = new ReadWriteThread(socket);
        mReadWriteThread.start();
        
        mHandler.obtainMessage(MSG_TYPE_CONNECTED_TO_BEACON, remoteDevice.getAddress()).sendToTarget();
    }    
    
	
    public synchronized void connect(BluetoothDevice device) throws IOException 
    {   	
        // Cancel any thread currently running a connection
        if (mReadWriteThread != null) {mReadWriteThread.cancel(); mReadWriteThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device, mAdapter);
        mConnectThread.start();
    }
    

	public synchronized void sendMessage(String msg) throws IOException
    {
    	mReadWriteThread.write(msg.getBytes());
    }
    
	
	public void stopConnectionThreads() 
	{
		if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
	}
	
	
	private class ConnectThread extends Thread 
	{
		private final BluetoothSocket mmSocket;
	    private final BluetoothAdapter mBluetoothAdapter;
	    
	    public ConnectThread(BluetoothDevice device, BluetoothAdapter bluetoothAdapter) throws IOException 
	    {
	    	this.mBluetoothAdapter = bluetoothAdapter;
	        // Use a temporary object that is later assigned to mmSocket, because mmSocket is final
	        BluetoothSocket tmp = null;
	        
	        tmp = device.createInsecureRfcommSocketToServiceRecord(BeaconDefaults.MY_UUID);
	        mmSocket = tmp;
	    }
	 
	    public void run() 
	    {
	        // Cancel discovery because it will slow down the connection
	        mBluetoothAdapter.cancelDiscovery();
	 
	        try 
	        {
	            // Connect the device through the socket. This will block until it succeeds or throws an exception
	            mmSocket.connect();
	        } 
	        catch (IOException connectException) 
	        {
	            // Unable to connect; close the socket and get out
	            try {
	                mmSocket.close();
	                throw connectException;
	            } catch (IOException closeException) { }
	            return;
	        }
	 
	        // Do work to manage the connection (in a separate thread)
	        startTransmission(mmSocket);
	    }
	    
	    /** Will cancel an in-progress connection, and close the socket */
	    public void cancel()
	    {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}
	
	
	
	private class ReadWriteThread extends Thread 
	{
		private final BluetoothSocket mmSocket;
	    private final InputStream mmInStream;
	    private final OutputStream mmOutStream;
	 
	    public ReadWriteThread(BluetoothSocket socket) 
	    {
	        mmSocket = socket;
	        InputStream tmpIn = null;
	        OutputStream tmpOut = null;
	 
	        // Get the input and output streams, using temp objects because member streams are final
	        try {
	            tmpIn = socket.getInputStream();
	            tmpOut = socket.getOutputStream();
	        } catch (IOException e) { }
	 
	        mmInStream = tmpIn;
	        mmOutStream = tmpOut;
	    }
	 
	    public void run() 
	    {
	    	// buffer store for the stream
	        byte[] buffer = new byte[1024];  

	        // bytes returned from read()
	        int bytes; 
	 
	        // Keep listening to the InputStream until an exception occurs
	        while (true) 
	        {
	            try 
	            {
	                // Read from the InputStream
	                bytes = mmInStream.read(buffer);
	                
	                // Send the obtained bytes to the UI activity
	                mHandler.obtainMessage(MSG_TYPE_MESSAGE_READ, bytes, -1, buffer).sendToTarget();
	            } 
	            catch (IOException e) 
	            {
	                break;
	            }
	        }
	    }
	 
	    /* Call this from the main activity to send data to the remote device */
	    public void write(byte[] bytes) throws IOException 
	    {
            mmOutStream.write(bytes);
	    }
	 
	    /* Call this from the main activity to shutdown the connection */
	    public void cancel() 
	    {
	        try 
	        {
	            mmSocket.close();
	        } 
	        catch (IOException e) { }
	    }
	}

}
