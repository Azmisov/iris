/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2021-2022  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.server.comm.ss125;

import java.io.IOException;
import java.util.Date;
import us.mn.state.dot.sched.TimeSteward;
import us.mn.state.dot.tms.EventType;
import us.mn.state.dot.tms.server.ControllerImpl;
import us.mn.state.dot.tms.server.comm.CommMessage;
import us.mn.state.dot.tms.server.comm.PriorityLevel;

/**
 * Operation to get event data from a SS125 device
 *
 * @author Douglas Lau
 * @author Michael Darter
 */
public class OpQueryEvents extends OpSS125 {

	/** Time stamp when operation started */
	private final long start_time;

	/** Create a new "query events" operation */
	public OpQueryEvents(ControllerImpl c) {
		super(PriorityLevel.IDLE, c);
		setSuccess(false);
		start_time = TimeSteward.currentTimeMillis();
		log("OpQueryEvents: start_time=" + new Date(start_time));
	}

	/** Handle a communication error */
	@Override
	public void handleCommError(EventType et, String msg) {
		setSuccess(false);
		super.handleCommError(et, msg);
	}

	/** Create the first phase of the operation */
	@Override
	protected Phase phaseOne() {
		return new GetActiveEvent();
	}

	/** Phase to get the active event */
	private class GetActiveEvent extends Phase {

		/* Number of vehicles detected */
		private int num_vehs = 0;

		/** Get the active event data */
		public Phase poll(
			CommMessage<SS125Property> mess) throws IOException
		{
			log("-------OpQueryEvents.GetActiveEvent.poll");
			ActiveEventProperty ev =
				new ActiveEventProperty(controller.getName());
			mess.add(ev);
			mess.queryProps();
			if (ev.isValidEvent()) {
				if (ev.isValidStamp(start_time)) {
					ev.logVehicle(controller);
					setSuccess(true);
					++num_vehs;
				} else {
					controller.logGap();
					setSuccess(false);
					mess.logError("BAD TIMESTAMP: " + new Date(ev.getTime()));
					return new SendDateTime();
				}
			}
			if (controller.hasActiveDetector())
				return this;
			long elapsed = TimeSteward.currentTimeMillis() - start_time;
			log("read %d vehicles in %ld ms.".formatted(num_vehs, elapsed));
			return null;
		}
	}

	/** Phase to send the date and time */
	private class SendDateTime extends Phase {

		/** Send the date and time */
		public Phase poll(
			CommMessage<SS125Property> mess) throws IOException
		{
			DateTimeProperty date_time = new DateTimeProperty();
			mess.add(date_time);
			mess.storeProps();
			return new ClearEvents();
		}
	}

	/** Phase to clear the event FIFO */
	private class ClearEvents extends Phase {

		/** Clear the event FIFO */
		public Phase poll(
			CommMessage<SS125Property> mess) throws IOException
		{
			ClearEventsProperty clear = new ClearEventsProperty();
			mess.add(clear);
			mess.storeProps();
			return phaseOne();
		}
	}

	/** Get the error retry threshold */
	@Override
	public int getRetryThreshold() {
		return Integer.MAX_VALUE;
	}

	/** Update the controller operation counters */
	public void updateCounters(int p) {
		boolean s = isSuccess();
		if (!s)
			controller.logGap();
		controller.binEventData(p, s);
		controller.completeOperation(id, s);
	}
}
