package com.example.das.ufsc.beaconmonitor.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BeaconDefaults 
{
	public static final String NAME = "beacon";
	public static final UUID MY_UUID = UUID.fromString("061d3751-78df-472c-8957-07df79497e71");
	
	//JSON KEYS
	public static final String MAC_KEY = "MAC";
	public static final String OPP_MODE_KEY = "OPP_MODE";
	public static final String ACK_KEY = "ACK";
	public static final String TIC_KEY = "TIC";
	
	//OPP MODES
	public static final int OPP_MODE_AUTHENTIC = 0;
	public static final int OPP_MODE_DUBIOUS = 1;

	

	private static final List<String> beaconList;
	
	static
	{
		//setting up the known device list:
		beaconList = new ArrayList<String>();
		beaconList.add("");
	}
	
	public static boolean checkBeacon(String mac)
	{
		return BeaconDefaults.beaconList.contains(mac);
	}
	
}
