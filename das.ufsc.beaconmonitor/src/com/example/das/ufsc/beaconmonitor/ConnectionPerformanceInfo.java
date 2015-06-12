package com.example.das.ufsc.beaconmonitor;

import java.util.Date;

public class ConnectionPerformanceInfo 
{
	private Date startDiscoveryTS;
	private Date beaconFoundTS;
	private Date firstConnAcceptanceTS;	
	private Date lastConnRequestTs;
	private Date lastConnAcceptanceTs;
	private Date lastTicReceivedTs;
	private Date lastAckSentTs;
	

	private boolean firstConnection;
	
	public ConnectionPerformanceInfo()
	{
		super();
		this.firstConnection = true;
	}
	
	public Date getLastConnRequestTs() {
		return lastConnRequestTs;
	}

	public void setLastConnRequestTs(Date lastConnRequestTs) {
		this.lastConnRequestTs = lastConnRequestTs;
	}

	public Date getLastTicReceivedTs() {
		return lastTicReceivedTs;
	}

	public void setLastTicReceivedTs(Date lastTicReceivedTs) {
		this.lastTicReceivedTs = lastTicReceivedTs;
	}

	public Date getLastAckSentTs() {
		return lastAckSentTs;
	}

	public void setLastAckSentTs(Date lastAckSentTs) {
		this.lastAckSentTs = lastAckSentTs;
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
	public void setBeaconFoundTS(Date beaconFoundTS) 
	{
		this.beaconFoundTS = beaconFoundTS;
		this.lastConnRequestTs = beaconFoundTS;
	}
	
	public Date getFirstConnAcceptanceTS() 
	{
		return firstConnAcceptanceTS;
	}
	
	public void setFirstConnAcceptanceTS(Date connDate) 
	{
		this.firstConnAcceptanceTS = connDate;
		this.lastConnAcceptanceTs = connDate;
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

	public void setLastConnAcceptanceTs(Date date) 
	{
		this.lastConnAcceptanceTs = date;
	} 
	
	public Date getLastConnAcceptanceTs() 
	{
		return this.lastConnAcceptanceTs;
	}

	public void setLastConnRequestTS(Date date) 
	{
		this.lastConnRequestTs = date;
	} 
	
	public Date getLastConnRequestTS() 
	{
		return this.lastConnRequestTs;
	} 

}
