/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2017-2023 Iteris Inc.
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
package us.mn.state.dot.tms.server.comm.ntcip;

import java.io.IOException;
import us.mn.state.dot.tms.server.WeatherSensorImpl;
import us.mn.state.dot.tms.server.comm.PriorityLevel;
import us.mn.state.dot.tms.server.comm.CommMessage;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.EssConvertible;
import us.mn.state.dot.tms.server.comm.snmp.ASN1Object;
import us.mn.state.dot.tms.server.comm.snmp.NoSuchName;

/**
 * Operation performed on an ESS
 *
 * @author Michael Darter, Isaac Nygaard
 */
abstract public class OpEss extends OpNtcip {

	/** Field sensor */
	protected final WeatherSensorImpl w_sensor; 

	/** Constructor */
	public OpEss(PriorityLevel p, WeatherSensorImpl ws) {
		super(p, ws);
		w_sensor = ws;
	}

	/** Helper to query many {@link EssConvertible} types, with automatic
	 * handling of logs
	 * @param mess message to query through
	 * @param nodes list of nodes to be queried
	 * @param logOnError whether you want to log even if there was an exception
	 * @returns error if it occurred, else null
	 */
	public NoSuchName queryMany(
		CommMessage<ASN1Object> mess, EssConvertible[] nodes, boolean logOnError
	) throws IOException{
		for (var node : nodes)
			mess.add(node.getRaw());
		// Note, the original code for OpQueryEssXXX didn't catch for many
		//  queries; so historically, either job failed and was caught higher
		//  up, or errors were never thrown
		try {
			mess.queryProps();
		} catch (NoSuchName err){
			log("Caught NoSuchName in ESS query: ex=" + err);
			if (logOnError)
				logQueryMany(nodes);
			return err;
		}
		logQueryMany(nodes);
		return null;
	}
	/** Same as {@link #queryMany}, with `longOnError` defaulting to false */
	public NoSuchName queryMany(CommMessage<ASN1Object> m, EssConvertible[] n) throws IOException{
		return queryMany(m, n, false);
	}

	private void logQueryMany(EssConvertible[] nodes){
		for (var node : nodes)
			logQuery(node.getRaw());
	}
}
