package com.example.das.ufsc.beaconmonitor.utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.example.das.ufsc.beaconmonitor.ConnectionPerformanceInfo;

public class BeaconDefaults 
{
	public static final String NAME = "beacon";
	
	//UUID for beacon service
	public static final UUID MY_UUID = UUID.fromString("061d3751-78df-472c-8957-07df79497e71");
	
	//JSON KEYS
	public static final String MAC_KEY = "MAC";
	public static final String OPP_MODE_KEY = "OPP_MODE";
	public static final String ACK_KEY = "ACK";
	public static final String TIC_KEY = "TIC";
	
	public static final String DISCOVERYDATE_KEY = "DISCTS";
	public static final String BEACON_FOUND_TS_KEY = "BEACONFOUNDTS";
	public static final String FIRSTCONNTS_KEY = "FIRSTCONNTS";
	public static final String LASTCONNTS_KEY = "LASTCONNTS";
	
	//OPP MODES
	public static final int OPP_MODE_AUTHENTIC = 0;
	public static final int OPP_MODE_DUBIOUS = 1;

	

	private static final List<String> beaconList;
	
	static
	{
		//setting up the known device list:
		beaconList = new ArrayList<String>();
		beaconList.add("9C:AD:97:FD:DB:52");
	}
	
	public static boolean checkBeacon(String mac)
	{
		return BeaconDefaults.beaconList.contains(mac);
	}
	
	public static String getStrDate(Date dt)
	{
		SimpleDateFormat formater = new SimpleDateFormat("yyyyMMdd.HHmmss.SSS");
		return formater.format(dt);
	}

	public static String formatJson(String address, int oppMode, ConnectionPerformanceInfo connPerformanceInfo) 
	{
		Date startDiscoveryTs = connPerformanceInfo.getStartDiscoveryTS();
		Date beaconFoundTs = connPerformanceInfo.getBeaconFoundTS();
		Date firstConnectionTs = connPerformanceInfo.getFirstConnectionTS();
		Date lastAcceptedConnectionTs = connPerformanceInfo.getLastAcceptedConnectionTS();

		String jsonString = "{";
		jsonString = jsonString + BeaconDefaults.MAC_KEY + ":'" + address;
		jsonString = jsonString + "'," + BeaconDefaults.OPP_MODE_KEY + ":" + oppMode;
		jsonString = jsonString + "," + BeaconDefaults.ACK_KEY + ":'true'"; 
		jsonString = jsonString + "," + BeaconDefaults.DISCOVERYDATE_KEY + ":" + getStrDate(startDiscoveryTs); 
		jsonString = jsonString + "," + BeaconDefaults.BEACON_FOUND_TS_KEY + ":" + getStrDate(beaconFoundTs);
		jsonString = jsonString + "," + BeaconDefaults.FIRSTCONNTS_KEY + ":" + getStrDate(firstConnectionTs);
		jsonString = jsonString + "," + BeaconDefaults.LASTCONNTS_KEY + ":" + getStrDate(lastAcceptedConnectionTs);
		jsonString = jsonString + "}";

		return jsonString;

	}
	
}
