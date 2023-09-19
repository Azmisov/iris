package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import static us.mn.state.dot.tms.server.comm.ntcip.mib1204.MIB1204.*;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.StationMobility;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.StationType;
import us.mn.state.dot.tms.utils.JsonBuilder;

/**
 * General metadata regarding the station, instrumentation, or device 
 *
 * @author Isaac Nygaard
 * @copyright 2023 Iteris Inc.
 * @license GPL-2.0
 */
public class InstrumentValues implements JsonBuilder.Buildable{
	/** Plaintext station description */
	public final EssString description = 
		new EssString("description", essNtcipSiteDescription);

	/** How data is collected at this station */
	public final EssEnum<StationType> data_collection = 
		EssEnum.make(StationType.class, "data_collection", essTypeofStation)
			.setMissing(StationType.missingValue.ordinal());

	/** What the mobility of the station is */
	public final EssEnum<StationMobility> mobility = 
		EssEnum.make(StationMobility.class, "mobility", essNtcipCategory);

	/** Whether any door is open */
	public final EssBoolean door_open =
		new EssBoolean("door_open", essDoorStatus);

	/** Battery charge as percent (0-100) */
	public final EssNumber battery =
		EssNumber.Percent("battery", essBatteryStatus);

	/** Incoming controller power as volts root mean squared (vmrs) */
	public final EssNumber line_volts =
		new EssNumber("line_volts", essLineVolts)
			.setScale(2)
			.setRange(0, EssNumber.MAX_BYTE);

	/** Get JSON representation */
	public void toJson(JsonBuilder jb){
		jb.extend(new EssConvertible[]{
			description,
			data_collection,
			mobility,
			door_open,
			battery,
			line_volts
		});
	}    
}
