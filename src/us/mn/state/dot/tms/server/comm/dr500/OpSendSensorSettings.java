/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2015-2022  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.server.comm.dr500;

import java.io.IOException;
import us.mn.state.dot.tms.server.ControllerImpl;
import us.mn.state.dot.tms.server.comm.CommMessage;
import us.mn.state.dot.tms.server.comm.PriorityLevel;

/**
 * Controller operation to send settings to a DR-500.
 *
 * @author Douglas Lau
 */
public class OpSendSensorSettings extends OpDR500 {

	/** Sensitivity value */
	static private final int SENSITIVITY_VAL = 99;

	/** Low speed value */
	static private final int LO_SPEED_VAL = 1;

	/** Threshold speed value */
	static private final int THRESHOLD_SPEED_VAL = 121;

	/** High speed value */
	static private final int HI_SPEED_VAL = 120;

	/** Target value (0 = select strongest, 1 = select fastest) */
	static private final int TARGET_VAL = 0;

	/** Binning interval (minutes) */
	static private final int BIN_INTERVAL = 1;

	/** Requested mode flags */
	static private int MODE_FLAGS = ModeFlag.SLOW_FILTER.flag
	                              | ModeFlag.RAIN_FILTER.flag;

	/** Time average period (seconds) */
	private final int period;

	/** Create a new operation to send settings to a sensor */
	public OpSendSensorSettings(PriorityLevel p, ControllerImpl c) {
		super(p, c);
		period = c.getPollPeriodSec();
	}

	/** Create a new operation to send settings to a sensor */
	public OpSendSensorSettings(ControllerImpl c) {
		this(PriorityLevel.SETTINGS, c);
	}

	/** Create the first phase of the operation */
	@Override
	protected Phase phaseOne() {
		return new QuerySysInfo();
	}

	/** Phase to query the system information */
	protected class QuerySysInfo extends Phase {

		/** Query the system information */
		public Phase poll(
			CommMessage<DR500Property> mess) throws IOException
		{
			SysInfoProperty si = new SysInfoProperty();
			mess.add(si);
			mess.queryProps();
			controller.setVersionNotify(si.getVersion());
			return new StoreDateTime();
		}
	}

	/** Phase to store the date/time */
	protected class StoreDateTime extends Phase {

		/** Store the date/time */
		public Phase poll(
			CommMessage<DR500Property> mess) throws IOException
		{
			DateTimeProperty dt = new DateTimeProperty();
			mess.add(dt);
			mess.storeProps();
			return new QueryUnits();
		}
	}

	/** Phase to query the units */
	protected class QueryUnits extends Phase {

		/** Query the units */
		public Phase poll(
			CommMessage<DR500Property> mess) throws IOException
		{
			VarProperty un = new VarProperty(VarName.UNITS);
			mess.add(un);
			mess.queryProps();
			if (un.getValue() != UnitsVar.MPH.ordinal())
				return new StoreUnits();
			else
				return new QueryBinning();
		}
	}

	/** Phase to store the units */
	protected class StoreUnits extends Phase {

		/** Store the units */
		public Phase poll(
			CommMessage<DR500Property> mess) throws IOException
		{
			VarProperty un = new VarProperty(VarName.UNITS,
				UnitsVar.MPH.ordinal());
			mess.add(un);
			mess.storeProps();
			return new QueryBinning();
		}
	}

	/** Phase to query the binning interval */
	protected class QueryBinning extends Phase {

		/** Query the binning interval */
		public Phase poll(
			CommMessage<DR500Property> mess) throws IOException
		{
			VarProperty bn = new VarProperty(VarName.BIN_MINUTES);
			mess.add(bn);
			mess.queryProps();
			if (bn.getValue() != BIN_INTERVAL)
				return new StoreBinning();
			else
				return new QuerySensitivity();
		}
	}

	/** Phase to store the binning interval */
	protected class StoreBinning extends Phase {

		/** Store the binning interval */
		public Phase poll(
			CommMessage<DR500Property> mess) throws IOException
		{
			VarProperty bn = new VarProperty(VarName.BIN_MINUTES,
				BIN_INTERVAL);
			mess.add(bn);
			mess.storeProps();
			return new QuerySensitivity();
		}
	}

	/** Phase to query the sensitivity */
	protected class QuerySensitivity extends Phase {

		/** Query the sensitivity */
		public Phase poll(
			CommMessage<DR500Property> mess) throws IOException
		{
			VarProperty st = new VarProperty(VarName.SENSITIVITY);
			mess.add(st);
			mess.queryProps();
			if (st.getValue() != SENSITIVITY_VAL)
				return new StoreSensitivity();
			else
				return new QueryLowSpeed();
		}
	}

	/** Phase to store the sensitivity */
	protected class StoreSensitivity extends Phase {

		/** Store the sensitivity */
		public Phase poll(
			CommMessage<DR500Property> mess) throws IOException
		{
			VarProperty st = new VarProperty(VarName.SENSITIVITY,
				SENSITIVITY_VAL);
			mess.add(st);
			mess.storeProps();
			return new QueryLowSpeed();
		}
	}

	/** Phase to query the low speed */
	protected class QueryLowSpeed extends Phase {

		/** Query the low speed */
		public Phase poll(
			CommMessage<DR500Property> mess) throws IOException
		{
			VarProperty lo = new VarProperty(VarName.LO_SPEED);
			mess.add(lo);
			mess.queryProps();
			if (lo.getValue() != LO_SPEED_VAL)
				return new StoreLowSpeed();
			else
				return new QueryThresholdSpeed();
		}
	}

	/** Phase to store the low speed */
	protected class StoreLowSpeed extends Phase {

		/** Store the low speed */
		public Phase poll(
			CommMessage<DR500Property> mess) throws IOException
		{
			VarProperty lo = new VarProperty(VarName.LO_SPEED,
				LO_SPEED_VAL);
			mess.add(lo);
			mess.storeProps();
			return new QueryThresholdSpeed();
		}
	}

	/** Phase to query the threshold speed */
	protected class QueryThresholdSpeed extends Phase {

		/** Query the threshold speed */
		public Phase poll(
			CommMessage<DR500Property> mess) throws IOException
		{
			VarProperty sp = new VarProperty(
				VarName.THRESHOLD_SPEED);
			mess.add(sp);
			mess.queryProps();
			if (sp.getValue() != THRESHOLD_SPEED_VAL)
				return new StoreThresholdSpeed();
			else
				return new QueryHighSpeed();
		}
	}

	/** Phase to store the threshold speed */
	protected class StoreThresholdSpeed extends Phase {

		/** Store the threshold speed */
		public Phase poll(
			CommMessage<DR500Property> mess) throws IOException
		{
			VarProperty sp = new VarProperty(
				VarName.THRESHOLD_SPEED, THRESHOLD_SPEED_VAL);
			mess.add(sp);
			mess.storeProps();
			return new QueryHighSpeed();
		}
	}

	/** Phase to query the high speed */
	protected class QueryHighSpeed extends Phase {

		/** Query the high speed */
		public Phase poll(
			CommMessage<DR500Property> mess) throws IOException
		{
			VarProperty hi = new VarProperty(VarName.HI_SPEED);
			mess.add(hi);
			mess.queryProps();
			if (hi.getValue() != HI_SPEED_VAL)
				return new StoreHighSpeed();
			else
				return new QueryTarget();
		}
	}

	/** Phase to store the high speed */
	protected class StoreHighSpeed extends Phase {

		/** Store the high speed */
		public Phase poll(
			CommMessage<DR500Property> mess) throws IOException
		{
			VarProperty hi = new VarProperty(VarName.HI_SPEED,
				HI_SPEED_VAL);
			mess.add(hi);
			mess.storeProps();
			return new QueryTarget();
		}
	}

	/** Phase to query the target flag */
	protected class QueryTarget extends Phase {

		/** Query the target flag */
		public Phase poll(
			CommMessage<DR500Property> mess) throws IOException
		{
			VarProperty sf = new VarProperty(VarName.TARGET);
			mess.add(sf);
			mess.queryProps();
			if (sf.getValue() != TARGET_VAL)
				return new StoreTarget();
			else
				return new QueryTimeAvg();
		}
	}

	/** Phase to store the target flag */
	protected class StoreTarget extends Phase {

		/** Store the target flag */
		public Phase poll(
			CommMessage<DR500Property> mess) throws IOException
		{
			VarProperty sf = new VarProperty(VarName.TARGET,
				TARGET_VAL);
			mess.add(sf);
			mess.storeProps();
			return new QueryTimeAvg();
		}
	}

	/** Phase to query time average */
	protected class QueryTimeAvg extends Phase {

		/** Query time average */
		public Phase poll(
			CommMessage<DR500Property> mess) throws IOException
		{
			VarProperty ta = new VarProperty(VarName.TIME_AVG);
			mess.add(ta);
			mess.queryProps();
			if (ta.getValue() != period)
				return new StoreTimeAvg();
			else
				return new QueryMode();
		}
	}

	/** Phase to store time average */
	protected class StoreTimeAvg extends Phase {

		/** Store time average */
		public Phase poll(
			CommMessage<DR500Property> mess) throws IOException
		{
			VarProperty ta = new VarProperty(VarName.TIME_AVG,
				period);
			mess.add(ta);
			mess.storeProps();
			return new QueryMode();
		}
	}

	/** Phase to query mode */
	protected class QueryMode extends Phase {

		/** Query mode */
		public Phase poll(
			CommMessage<DR500Property> mess) throws IOException
		{
			VarProperty mode = new VarProperty(VarName.MODE);
			mess.add(mode);
			mess.queryProps();
			int f = mode.getValue() | MODE_FLAGS;
			if (f != mode.getValue())
				return new StoreMode(f);
			else
				return null;
		}
	}

	/** Phase to store mode */
	protected class StoreMode extends Phase {
		private final int flags;
		private StoreMode(int f) {
			flags = f;
		}

		/** Store mode */
		public Phase poll(
			CommMessage<DR500Property> mess) throws IOException
		{
			VarProperty mode = new VarProperty(VarName.MODE, flags);
			mess.add(mode);
			mess.storeProps();
			return null;
		}
	}
}
