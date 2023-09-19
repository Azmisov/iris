/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2016  Iteris Inc.
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
import us.mn.state.dot.tms.Detector;
import us.mn.state.dot.tms.DetectorHelper;

/**
 * This class writes the periodic vehicle XML file.
 * @author Michael Darter
 */
class VehicleManager {

	/** Name of vehicle sample XML file */
	static private final String SAMPLE_XML = "vehicle_sample.xml";

	/** Write the vehicle sample data out as XML */
	public void writeSampleXml() throws IOException {
		XmlWriter w = new XmlWriter(SAMPLE_XML, true) {
			@Override protected void write(Writer w)
				throws IOException
			{
				writeSampleXmlHead(w);
				writeSampleXmlBody(w);
				writeSampleXmlTail(w);
			}
		};
		w.write();
	}

	/** Print the header sample XML file */
	private void writeSampleXmlHead(Writer w) throws IOException {
		w.write(XmlWriter.XML_DECLARATION);
		writeDtd(w);
		w.write("<vehicle_sample time_stamp='" +
			TimeSteward.getDateInstance() + "' period='30'>\n");
	}

	/** Print the DTD */
	private void writeDtd(Writer w) throws IOException {
		w.write("""
			<!DOCTYPE vehicle_sample [
				<!ELEMENT vehicle_sample (sample)*>
				<!ATTLIST vehicle_sample time_stamp CDATA #REQUIRED>
				<!ATTLIST vehicle period CDATA #REQUIRED>
				<!ELEMENT vehicle EMPTY>
				<!ATTLIST vehicle det_name CDATA #REQUIRED>
				<!ATTLIST vehicle speed CDATA #REQUIRED>
				<!ATTLIST vehicle source CDATA #REQUIRED>
				<!ATTLIST vehicle lane_num CDATA #REQUIRED>
				<!ATTLIST vehicle range CDATA #REQUIRED>
				<!ATTLIST vehicle length CDATA #REQUIRED>
				<!ATTLIST vehicle duration_ms CDATA #REQUIRED>
				<!ATTLIST vehicle ctrl_time CDATA #REQUIRED>
				<!ATTLIST vehicle class CDATA #REQUIRED>
			]>
			""");
	}

	/** Print the body of the sample XML file */
	private void writeSampleXmlBody(Writer w) throws IOException {
		Iterator<Detector> it = DetectorHelper.iterator();
		while (it.hasNext()) {
			Detector d = it.next();
			if (d instanceof DetectorImpl) {
				DetectorImpl det = (DetectorImpl)d;
				det.writeSampleVehicleXml(w);
				det.writeSampleVehicleXml(w);
			}
		}
	}

	/** Print the tail of the sample XML file */
	private void writeSampleXmlTail(Writer w) throws IOException {
		w.write("</vehicle_sample>\n");
	}
}