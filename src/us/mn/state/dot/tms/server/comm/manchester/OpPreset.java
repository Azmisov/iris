/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2014  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.server.comm.manchester;

import java.io.IOException;
import us.mn.state.dot.tms.server.CameraImpl;
import us.mn.state.dot.tms.server.comm.CommMessage;

/**
 * Manchester operation to recall or store a camera preset.
 *
 * @author Douglas Lau
 */
public class OpPreset extends OpManchester {

	/** Preset property */
	private final PresetProperty prop;

	/** Create a new operation to recall or store a camera preset */
	public OpPreset(CameraImpl c, boolean s, int p) {
		super(c);
		prop = new PresetProperty(s, p);
	}

	/** Create the second phase of the operation */
	@Override
	protected Phase phaseTwo() {
		return new CommandPreset();
	}

	/** Phase to recall or store a camera preset */
	protected class CommandPreset extends Phase {

		/** Command controller to recall or store a preset */
		protected Phase poll(
			CommMessage<ManchesterProperty> mess) throws IOException
		{
			mess.add(prop);
			mess.storeProps();
			return null;
		}
	}
}
