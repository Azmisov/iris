/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2007-2017  Minnesota Department of Transportation
 * Copyright (C) 2014  AHMCT, University of California
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
package us.mn.state.dot.tms.server.comm.pelcod;

import java.io.IOException;
import us.mn.state.dot.tms.server.CameraImpl;
import us.mn.state.dot.tms.server.comm.CommMessage;

/**
 * Pelco operation to move a camera.
 *
 * @author Douglas Lau
 * @author Travis Swanston
 */
public class OpMoveCamera extends OpPelcoD {

	/** Range of pan values (includes "turbo" values of 64) */
	static private final int PAN_RANGE = 65;

	/** Range of Tilt/Zoom values */
	static private final int TZ_RANGE = 64;

	/** Clamp a float value to the range of (-1, 1) */
	static private float clamp_float(float value) {
		return Math.max(-1, Math.min(value, 1));
	}

	/** Map a float value to an integer range */
	static private int map_float(float value, int range) {
		return Math.round(clamp_float(value) * (range - 1));
	}

	/** Command property */
	private final CommandProperty prop;

	/** Create a new operation to move a camera */
	public OpMoveCamera(CameraImpl c, float p, float t, float z) {
		super(c);
		int pan = map_float(p, PAN_RANGE);
		int tilt = map_float(t, TZ_RANGE);
		int zoom = map_float(z, TZ_RANGE);
		prop = new CommandProperty(pan, tilt, zoom, 0, 0);
	}

	/** Create the second phase of the operation */
	@Override
	protected Phase phaseTwo() {
		return new Move();
	}

	/** Phase to move the camera */
	protected class Move extends Phase {

		/** Number of times this request was sent */
		private int n_sent = 0;

		/** Command controller to move the camera */
		public Phase poll(
			CommMessage<PelcoDProperty> mess) throws IOException
		{
			mess.add(prop);
			mess.storeProps();
			n_sent++;
			return shouldResend() ? this : null;
		}

		/** Should we resend the property? */
		private boolean shouldResend() {
			return prop.isStop() && (n_sent < 2);
		}
	}
}
