/**
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2021  Iteris Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package us.mn.state.dot.tms.server;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import javax.mail.MessagingException;
import us.mn.state.dot.sched.DebugLog;
import us.mn.state.dot.sched.Job;
import us.mn.state.dot.sched.TimeSteward;
import us.mn.state.dot.tms.SystemAttrEnum;
import us.mn.state.dot.tms.WeatherSensor;
import us.mn.state.dot.tms.WeatherSensorHelper;
import us.mn.state.dot.tms.utils.Emailer;
import us.mn.state.dot.tms.utils.SString;

/**
 * Job to verify weather sensor site id's and pikalert ids don't have 
 * 1) empty ids, 2) non-numeric ids, 3) bad length ids, 4) duplicates.
 * Email is generated and sent every 24h identifyfing issues.
 * 
 * @author Deb Behera
 * @author Michael Darter
 */
public class WeatherSensorVerifyJob extends Job {

	/** Max and min id lengths */
	static private final int SID_MIN_LEN = 6;
	static private final int SID_MAX_LEN = 8;
	static private final int PID_MIN_LEN = 4;
	static private final int PID_MAX_LEN = 6;

	/** Debug log */
	static private final DebugLog LOG = new DebugLog("rwis_verify");

	/** Log a message */
	static private void log(String msg) {
		if (LOG.isOpen()) {
			StringBuilder sb = new StringBuilder();
			sb.append(TimeSteward.currentDateTimeString(true));
			sb.append(" WeatherSensorVerify: ").append(msg);
			LOG.log(sb.toString());
		}
	}

	/** Log a message */
	static private void log(String severity, String msg) {
		log(severity + "! "  + msg);
	}

	/** Return a null-safe string */
	static private String safe(String str) {
		return (str != null ? str : "");
	}

	/** Create an error message */
	static private String createMsg(WeatherSensorImpl wsi, String msg) {
		String sid = safe(wsi.getSiteId());
		String pid = safe(wsi.getPikalertSiteId());
		return (sid + " - " + pid + ":" + msg);
	}

	/** Determine if a weather sensor is available */
	static private boolean isAvailable(WeatherSensorImpl wsi) {
		return wsi.isActive() && wsi.isConnected();
	}

	/** Site id hash of sid to sensor name */
	private HashMap<String,String> sid_hash = new HashMap<String,String>();

	/** Pikalert id hash of pid to sensor name */
	private HashMap<String,String> pid_hash = new HashMap<String,String>();

	/** Message errors list */
	private LinkedList<String> msg_list = new LinkedList<String>();

	/** Create a new job that executes daily at 5AM */
	public WeatherSensorVerifyJob() {
		super(Calendar.DATE, 1, Calendar.HOUR, 5);
	}

	/** Perform the job */
	public void perform() throws IOException {
		verify();
	}

	/** Verify weather sensor ids for all sensors. Get all the 
	 * siteid and psiteid and create a hashmap and check if 
	 * there are duplicates, nulls or invalid lengths. */
	private void verify() {
		log("-------Verifying RWIS");
		Iterator<WeatherSensor> it = WeatherSensorHelper.iterator();
		while (it.hasNext()) {
			WeatherSensor ws = it.next();
			if (ws instanceof WeatherSensorImpl)
				verify((WeatherSensorImpl)ws);
		}
		log("Number of errors=" + msg_list.size());
		if (msg_list.size() > 0)
			sendEmailAlert();
		clearContainers();
		log("Done verifying RWIS");
	}

	/** Verify weather sensor ids for a single weather sensor.
	 * Get all the siteid and psiteid and create a hashmap and 
	 * check if there are duplicates, nulls or invalid lengths. */
	private void verify(WeatherSensorImpl wsi) {
		boolean valid = isValidSyntax(wsi);

		// verify siteid not duplicate
		String sid = safe(wsi.getSiteId());
		if (!sid_hash.containsKey(sid)) {
			sid_hash.put(sid, wsi.getName());
		} else {
			valid = false;
			addErrorMsg(wsi, "Site id=" + sid + 
				" is a duplicate of " + sid_hash.get(sid));
		}

		// verify psiteid not duplicate
		String pid = safe(wsi.getPikalertSiteId());
		if (!pid_hash.containsKey(pid)) {
			pid_hash.put(pid, wsi.getName());
		} else {
			valid = false;
			addErrorMsg(wsi, " Pikalert id=" + pid + 
				" is a duplicate of " + pid_hash.get(pid));
		}

		log("Verify: " + wsi.getName() + " sid=" + sid + 
			" pid=" + pid + " avail=" + isAvailable(wsi) +
			" valid=" + valid);
	}

	/** Verify weather sensor id syntax is valid */
	private boolean isValidSyntax(WeatherSensorImpl wsi) {
		boolean valid = true;
		String sid = safe(wsi.getSiteId());
		String pid = safe(wsi.getPikalertSiteId());
		if (!SString.isNumeric(sid) || !SString.isNumeric(pid)) {
			valid = false;
			addErrorMsg(wsi, 
				"siteid=" + sid + " psiteid=" + pid +  
				" site id and psiteid must be numeric");
		}
		if (sid.length() < SID_MIN_LEN || sid.length() > SID_MAX_LEN ){
			valid = false;
			addErrorMsg(wsi, 
				"siteid=" + sid + 
				" bad site id length=" + sid.length() + 
				" should be " + SID_MIN_LEN + "-"+SID_MAX_LEN);
		} 
		if (pid.length() < PID_MIN_LEN || pid.length() > PID_MAX_LEN ){
			valid = false;
			addErrorMsg(wsi, 
				"psiteid=" + pid +  
				" bad Pikalert id length=" + pid.length() + 
				" should be " + PID_MIN_LEN + "-"+PID_MAX_LEN);
		}
		return valid;
	}

	/** Add an error message to the list */
	private void addErrorMsg(WeatherSensorImpl wsi, String msg) {
		if (isAvailable(wsi))
			msg_list.add(wsi.getName() + ": " + msg);
	}

	/** Send an email alert for all errors */
	private void sendEmailAlert() {
		String host = SystemAttrEnum.EMAIL_SMTP_HOST.getString();
		if (host.isEmpty()) {
			log("Error","Email Host FAILED");
			return;
		}
		String sender = SystemAttrEnum.EMAIL_SENDER_SERVER.getString();
		if (sender.isEmpty()) {
			log("Error","Email Sender FAILED");
			return;
		}
		String recip = SystemAttrEnum.
			EMAIL_RECIPIENT_ACTION_PLAN.getString();
		if (recip.isEmpty()) {
			log("Error","Email reciepient FAILED");
			return;
		}
		String sub = "IRIS RWIS site id issues";
		StringBuilder msg = new StringBuilder("\n");
		for (String msgdetails : msg_list) {
			msg.append(msgdetails).append("\n\n");
		}
		log("Sending email: " + sub + "/" + recip + "/" + msg);
		try {
			Emailer email = new Emailer(host, sender, recip);
			email.send(sub, msg.toString());
		} catch(MessagingException ex) {
			log("Error",msg + "ex=" + ex.getMessage());
		}
	}

	/** Clear containers */
	private void clearContainers() {
		sid_hash.clear();
		pid_hash.clear();
		msg_list.clear();
	}
}