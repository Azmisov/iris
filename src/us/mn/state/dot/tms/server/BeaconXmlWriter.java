/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2009-2016  Minnesota Department of Transportation
 * Copyright (C) 2012-2022  Iteris Inc.
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
import java.io.Writer;
import java.util.Iterator;
import us.mn.state.dot.sched.TimeSteward;
import us.mn.state.dot.tms.Beacon;
import us.mn.state.dot.tms.BeaconHelper;

/**
 * This class writes out the current beacons to an XML file.
 *
 * @author Tim Johnson
 * @author Douglas Lau
 * @author Michael Darter
 * @author Arin Kase
 */
public class BeaconXmlWriter extends XmlWriter {

	/** XML file */
	static private final String BEACON_XML = "beacon.xml";

	/** Create a new beacon XML writer */
	public BeaconXmlWriter() {
		super(BEACON_XML, true);
	}

	/** Write the beacon XML file */
	@Override protected void write(Writer w) throws IOException {
		writeHead(w);
		writeBody(w);
		writeTail(w);
        }

	/** Write the head of the beacon XML file */
	private void writeHead(Writer w) throws IOException {
		w.write(XML_DECLARATION);
		writeDtd(w);
		w.write("<beacons time_stamp='" +
			TimeSteward.getDateInstance() + "'>\n");
	}

	/** Write the DTD */
	private void writeDtd(Writer w) throws IOException {
		w.write("<!DOCTYPE beacons [\n");
		w.write("<!ELEMENT beacons (beacon)*>\n");
		w.write("<!ATTLIST beacons time_stamp CDATA #REQUIRED>\n");
		w.write("<!ELEMENT beacons EMPTY>\n");
		w.write("<!ATTLIST beacons name CDATA #REQUIRED>\n");
		w.write("<!ATTLIST beacons pin CDATA #IMPLIED>\n");
		w.write("<!ATTLIST beacons notes CDATA #IMPLIED>\n");
		w.write("<!ATTLIST beacons message CDATA #REQUIRED>\n");
		w.write("<!ATTLIST beacons flashing CDATA #IMPLIED>\n");
		w.write("]>\n");
	}

	/** Write the body of the beacon XML file */
	private void writeBody(Writer w) throws IOException {
		Iterator<Beacon> it = BeaconHelper.iterator();
		while(it.hasNext()) {
			Beacon bea = it.next();
			if(bea instanceof BeaconImpl)
				((BeaconImpl)bea).writeXml(w);
		}
	}

	/** Write the tail of the beacon XML file */
	private void writeTail(Writer w) throws IOException {
		w.write("</beacons>\n");
	}
}
