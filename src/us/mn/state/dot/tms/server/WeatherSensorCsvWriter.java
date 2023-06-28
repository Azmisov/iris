package us.mn.state.dot.tms.server;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import us.mn.state.dot.tms.WeatherSensor;
import us.mn.state.dot.tms.WeatherSensorHelper;

/**
 * Write SSI ScanWeb CSV weather export files.
 *
 * @author Michael Darter
 * @copyright 2017-2021 Iteris Inc.
 * @license GPL-2.0
 */
public class WeatherSensorCsvWriter extends XmlWriter {

	/** Factory to create a new CSV file writer and write the file.
	 * @param ft File type
	 * @return Null on error or a new file writer */
	static public WeatherSensorCsvWriter create(WeatherSensorFileEnum ft) {
		return new WeatherSensorCsvWriter(ft);
	}

	/** Write a terminated line */
	static private void writeLine(Writer wr, String line) 
 		throws IOException
 	{
 		if (line != null)
 			wr.write(line + "\n");
 	}

	/** File type to generate */
	final private WeatherSensorFileEnum f_type;

	/** Constructor
	 * @arg ft File type to generate */
	private WeatherSensorCsvWriter(WeatherSensorFileEnum ft) {
		super(ft.file_name, true);
		f_type = ft;
	}

	/** Write the head of the CSV file */
	private void writeHead(Writer wr) throws IOException {
		writeLine(wr, f_type.header_row);
	}

	/** Write the weather sensor CSV file */
	@Override protected void write(Writer w) throws IOException {
		writeHead(w);
		writeBody(w);
	}

	/** Write the body of the XML file */
	private void writeBody(Writer wr) throws IOException {
		Iterator<WeatherSensor> it = WeatherSensorHelper.iterator();
		while(it.hasNext()) {
			WeatherSensor ws = it.next();
			if(ws instanceof WeatherSensorImpl) {
				WeatherSensorImpl wsi = (WeatherSensorImpl)ws;
				// Only write recs for enabled active rwis
				if (wsi.getActiveEnabled()) {
					String line = f_type.getRecs(wsi);
					if (!line.isEmpty())
						writeLine(wr, line);
				}
			}
		}
	}
}
