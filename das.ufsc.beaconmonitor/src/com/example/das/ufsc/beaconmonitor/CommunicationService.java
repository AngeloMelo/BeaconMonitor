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
	public static final int MSG_TYPE_EXCEPTION = 4;
	public static final int MSG_TYPE_CONNECT_EXCEPTION = 5;
	public static final int MSG_TYPE_CONNECTION_CLOSED = 6;
	
	private ReadWriteThread mReadWriteThread;
	private ConnectThread mConnectThread;
	private final Handler mHandler;
	
	
	public CommunicationService(Handler handler) 
	{
        mHandler = handler;
    }
	
	
	public void shutDown() throws IOException
	{
		if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
		if (mReadWriteThread != null) {mReadWriteThread.cancel(); mReadWriteThread = null;}
	}

	
	/**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
	 * @throws IOException 
     */
	private synchronized void startTransmission(BluetoothSocket socket) throws IOException 
    {
		BluetoothDevice remoteDevice = socket.getRemoteDevice();
        // Cancel any thread currently running a connection
        if (mReadWriteThread != null) {mReadWriteThread.cancel(); mReadWriteThread = null;}
        
        // Start the thread to manage the connection and perform transmissions
        mReadWriteThread = new ReadWriteThread(socket);
        mReadWriteThread.start();
        
        mHandler.obtainMessage(MSG_TYPE_CONNECTED_TO_BEACON, remoteDevice.getAddress()).sendToTarget();
    }    
    
	
    public void connect(BluetoothDevice device) throws IOException 
    {   	
        // Cancel any thread currently running a connection
        if (mReadWriteThread != null) {mReadWriteThread.cancel(); mReadWriteThread = null;}

        // Cancel discovery because it will slow down the connection
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        
        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
    }
    

	public void sendMessage(String msg) throws IOException
    {
		if(mReadWriteThread != null)
    	{
			mReadWriteThread.write(msg.getBytes());
    	}
    }
    
	
	private class ConnectThread extends Thread 
	{
		private final BluetoothSocket mmSocket;
	    private boolean running = true;
	    
	    public ConnectThread(BluetoothDevice device) throws IOException 
	    {
	        // Use a temporary object that is later assigned to mmSocket, because mmSocket is final
	        BluetoothSocket tmp = null;
	        
	        tmp = device.createInsecureRfcommSocketToServiceRecord(BeaconDefaults.MY_UUID);
	        mmSocket = tmp;
	    }
	 
	    public void run() 
	    {
	    	if(!running) return;
	 
	        try 
	        {
	            // Connect the device through the socket. This will block until it succeeds or throws an exception
	        	/**
	        	 * Upon this call, the system will perform an SDP lookup on the remote device in order to match the UUID. 
	        	 * If the lookup is successful and the remote device accepts the connection, it will share the RFCOMM channel
	        	 *  to use during the connection and connect() will return. This method is a blocking call. If, for any reason, 
	        	 *  the connection fails or the connect() method times out (after about 12 seconds), then it will throw an exception.
	        	 *  Because connect() is a blocking call, this connection procedure should always be performed in a thread separate 
	        	 *  from the main activity thread.
	        	 */
	            mmSocket.connect();
	        } 
	        catch (IOException connectException) 
	        {
	            // Unable to connect; close the socket and get out
	            try 
	            {
	                mmSocket.close();
	                mHandler.obtainMessage(MSG_TYPE_CONNECT_EXCEPTION, connectException).sendToTarget();
	            } 
	            catch (IOException closeException) 
	            { 
	            	mHandler.obtainMessage(MSG_TYPE_EXCEPTION, closeException).sendToTarget();
	            }
	            return;
	        }
	 
	        try
	        {
	        	// Do work to manage the connection (in a separate thread)
	        	startTransmission(mmSocket);
	        }
	        catch(IOException e)
	        {
	        	mHandler.obtainMessage(MSG_TYPE_EXCEPTION, e).sendToTarget();
	        }
	    }
	    
	    public synchronized void cancel()
	    {
	        this.running = false;
	    }
	}
	
	
	
	private class ReadWriteThread extends Thread 
	{
		private final BluetoothSocket mmSocket;
	    private final InputStream mmInStream;
	    private final OutputStream mmOutStream;
	    private volatile boolean running = true;
	 
	    public ReadWriteThread(BluetoothSocket socket) 
	    {
	        mmSocket = socket;
	        InputStream tmpIn = null;
	        OutputStream tmpOut = null;
	 
	        // Get the input and output streams, using temp objects because member streams are final
	        try 
	        {
	            tmpIn = socket.getInputStream();
	            tmpOut = socket.getOutputStream();
	        } 
	        catch (IOException e) 
	        { 
	        	mHandler.obtainMessage(MSG_TYPE_EXCEPTION, e).sendToTarget();
	        }
	 
	        mmInStream = tmpIn;
	        mmOutStream = tmpOut;
	    }
	 
	    public void run() 
	    {
	    	// buffer store for the stream
	        byte[] buffer = new byte[2048];  

	        // bytes returned from read()
	        int bytes; 
	 
	        // Keep listening to the InputStream until an exception occurs
	        while (this.running) 
	        {
	            try 
	            {
	                // Read from the InputStream
	                bytes = mmInStream.read(buffer);
	                
	                if(bytes > 0)
	                {
	                	// Send the obtained bytes to the UI activity
	                	mHandler.obtainMessage(MSG_TYPE_MESSAGE_READ, bytes, -1, buffer).sendToTarget();	                	
	                }
	            } 
	            catch (IOException e) 
	            {
	            	mHandler.obtainMessage(MSG_TYPE_CONNECTION_CLOSED, null).sendToTarget();
	            	
	            	try 
	            	{
	            		shutDown();
					} 
	            	catch (IOException e1) 
					{
						e1.printStackTrace();
					}
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
	    public synchronized void cancel() throws IOException
	    {
	    	if(! running) return;
	    	
	        try 
	        { 
	        	if(mmInStream!= null)
	        	{
	        		mmInStream.close(); 
	        	}
	        }
	        catch (IOException e) 
	        {
	        	mHandler.obtainMessage(MSG_TYPE_CONNECT_EXCEPTION, e).sendToTarget();
	        }

	        try 
	        { 
	        	if(mmOutStream != null)
	        	{
	        		mmOutStream.close();
	        	}
	        }
	        catch (IOException e) 
	        {
	        	mHandler.obtainMessage(MSG_TYPE_CONNECT_EXCEPTION, e).sendToTarget();
	        }

	        try 
	        { 
        		mmSocket.close();
	        }
	        catch (IOException e) 
	        {
	        	mHandler.obtainMessage(MSG_TYPE_CONNECT_EXCEPTION, e).sendToTarget();
	        }
	        finally
	        { 
	        	this.running = false;
	        }
	    }
	}

}
