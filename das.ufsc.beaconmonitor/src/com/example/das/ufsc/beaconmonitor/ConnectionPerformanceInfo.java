package com.example.das.ufsc.beaconmonitor;

import java.util.Date;

public class ConnectionPerformanceInfo 
{
	private Date startDiscoveryTS;
	private Date beaconFoundTS;
	private Date firstConnectionTS;	
	private boolean firstConnection;
	private Date lastAcceptedConnection;
	
	public ConnectionPerformanceInfo()
	{
		super();
		this.firstConnection = true;
	}
	
	public Date getStartDiscoveryTS() {
		return startDiscoveryTS;
	}
	public void setStartDiscoveryTS(Date startDiscoveryTS) {
		this.startDiscoveryTS = startDiscoveryTS;
	}
	public Date getBeaconFoundTS() {
		return beaconFoundTS;
	}
	public void setBeaconFoundTS(Date beaconFoundTS) {
		this.beaconFoundTS = beaconFoundTS;
	}
	
	public Date getFirstConnectionTS() 
	{
		return firstConnectionTS;
	}
	
	public void setFirstConnectionTS(Date connDate) 
	{
		this.firstConnectionTS = connDate;
		this.lastAcceptedConnection = connDate;
		setFirstConnection(false);
	}

	public boolean isFirstConnection() 
	{
		return firstConnection;
	}

	private void setFirstConnection(boolean firstConnection) 
	{
		this.firstConnection = firstConnection;
	}

	public void setLastAcceptedConnectionTS(Date date) 
	{
		this.lastAcceptedConnection = date;
	} 
	
	public Date getLastAcceptedConnectionTS() 
	{
		return this.lastAcceptedConnection;
	} 
}
