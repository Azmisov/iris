/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2017  Iteris Inc.
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
import java.util.Date;
import us.mn.state.dot.tms.CommProtocol;
import us.mn.state.dot.tms.utils.XmlBuilder;

/**
 * A vehicle
 *
 * @author Michael Darter
 */
public class Vehicle {

	/** Data source */
	public final CommProtocol data_source;

	/** Lane number */
	public final int lane_num;

	/** Detector name */
	public final String det_name;

	/** Event time from controller's clock */
	public final long ctrl_time;

	/** Range */
	public final float range;

	/** Duration in ms */
	public final int dura_ms;

	/* Speed in MPH or KPH */
	public final float speed;

	/** Vehicle class */
	public final int vclass;

	/** Vehicle length in ft or meters */
	public final float vlength;

	/** Constructor */
	public Vehicle(CommProtocol ds, long ts, float ra, int du, float sp, 
		int vc, float le, String dname, int lnum)
	{
		data_source = ds;
		ctrl_time = ts;
		range = ra;
		dura_ms = du;
		speed = sp;
		vclass = vc;
		vlength = le;
		det_name = dname;
		lane_num = lnum;
	}

	/** Get controller time */
	private String getTime() {
		if (ctrl_time <= 0)
			return "?";
		return new Date(ctrl_time).toString();
	}

	/** To string */
	public String toString() {
		StringBuilder sb = new StringBuilder("SS125_event:");
		sb.append(" data_source=").append(data_source);
		sb.append(" ctrl_time=").append(getTime());
		sb.append(" range=").append(range);
		sb.append(" dura_ms=").append(dura_ms);
		sb.append(" speed=").append(speed);
		sb.append(" class=").append(vclass);
		sb.append(" length=").append(vlength);
		sb.append(" det_name=").append(det_name);
		sb.append(" lane_num=").append(lane_num);
		return sb.toString();
	}

	/** Write vehicle as XML */
	public void writeXml(Writer w) throws IOException {
		w.write('\t');
		var xb = new XmlBuilder(w);
		xb.tag("vehicle")
			.attr("ctrl_time", getTime())
			.attr("source", data_source)
			.attr("speed", speed)
			.attr("length", vlength)
			.attr("duration_ms", dura_ms)
			.attr("range", range)
			.attr("det_name", det_name)
			.attr("lane_num", lane_num)
			.attr("class", vclass)
			.ancestor(0);
		w.write('\n');
	}
}