/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2009-2022  Minnesota Department of Transportation
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

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.TimeZone;
import us.mn.state.dot.tms.utils.HexString;
import us.mn.state.dot.tms.server.ControllerImpl;
import us.mn.state.dot.tms.server.comm.ChecksumException;
import us.mn.state.dot.tms.server.comm.ControllerException;
import us.mn.state.dot.tms.server.comm.ControllerProperty;
import us.mn.state.dot.tms.server.comm.CRC;
import us.mn.state.dot.tms.server.comm.InvalidAddressException;
import us.mn.state.dot.tms.server.comm.ParsingException;
import us.mn.state.dot.tms.server.comm.ProtocolException;

/**
 * SS125 property.
 *
 * @author Douglas Lau
 * @author Michael Darter
 */
abstract public class SS125Property extends ControllerProperty {

	/** Charset name for ASCII */
	static private final String ASCII = "US-ASCII";

	/** Byte offsets from beginning of header packet */
	static private final int OFF_SENTINEL = 0;
	static private final int OFF_PROTOCOL_VER = 1;
	static private final int OFF_DEST_SUB_ID = 2;
	static private final int OFF_DEST_ID = 3;
	static private final int OFF_SOURCE_SUB_ID = 5;
	static private final int OFF_SOURCE_ID = 6;
	static private final int OFF_SEQUENCE = 8;
	static private final int OFF_BODY_SIZE = 9;
	static private final int OFF_CRC = 10;

	/** Byte offsets from beginning of body packet */
	static private final int OFF_MSG_ID = 0;
	static protected final int OFF_MSG_SUB_ID = 1;
	static private final int OFF_MSG_TYPE = 2;
	static private final int OFF_RESULT = 3;

	/** Maximum number of octets in message body */
	static private final int MAX_BODY_OCTETS = 244;

	/** Sub ID must be configured to zero */
	static private final int SUB_ID = 0;

	/** Message sub ID "don't care" */
	static private final byte DONT_CARE_MSG_SUB_ID = 0;

	/** CRC calculator */
	static private final CRC crc = new CRC(8, 0x1C, 0x00, false);

	/** Controller name for logging */
	private final String ctl_name;

	/** Check if a drop address is valid */
	static private boolean isAddressValid(int drop) {
		return drop > 0 && drop < 65536;
	}

	/** Calculate the CRC for a buffer */
	static private int calculate(byte[] buf) {
		return crc.calculate(buf, buf.length - 1);
	}

	/** Format a boolean value.
	 * @param buf Buffer to store formatted value.
	 * @param pos Starting position in buffer.
	 * @param value Value to store in buffer. */
	static protected void formatBool(byte[] buf, int pos, boolean value) {
		buf[pos] = value ? (byte) 1 : (byte) 0;
	}

	/** Format a 16-bit fixed-point value.
	 * @param buf Buffer to store formatted value.
	 * @param pos Starting position in buffer.
	 * @param value Value to store in buffer. */
	static protected void format16Fixed(byte[] buf, int pos, float value) {
		int intg = (int) value;
		int frac = (int) (256 * (value - intg));
		buf[pos] = (byte) intg;
		buf[pos + 1] = (byte) frac;
	}

	/** Format a 24-bit value.
	 * @param buf Buffer to store formatted value.
	 * @param pos Starting position in buffer.
	 * @param value Value to store in buffer. */
	static protected void format24(byte[] buf, int pos, int value) {
		buf[pos] = (byte) ((value >> 16) & 0xFF);
		buf[pos + 1] = (byte) ((value >> 8) & 0xFF);
		buf[pos + 2] = (byte) (value & 0xFF);
	}

	/** Format a string to a byte array.
	 * @param buf Buffer to store formatted value.
	 * @param pos Starting position in buffer.
	 * @param len Length of field in buffer.
	 * @param value Value to store in buffer. */
	static protected void formatString(byte[] buf, int pos,
		int len, String value) throws IOException
	{
		byte[] src = value.getBytes(ASCII);
		int vlen = Math.min(len, src.length);
		System.arraycopy(src, 0, buf, pos, vlen);
		for (int i = vlen; i < len; i++)
			buf[pos + i] = 0;
	}

	/** Parse a boolean value */
	static protected boolean parseBool(byte[] body, int pos)
		throws ParsingException
	{
		int b = body[pos];
		if (b == 0)
			return false;
		else if (b == 1)
			return true;
		else
			throw new ParsingException("INVALID BOOLEAN");
	}

	/** Parse a 16-bit fixed-point value */
	static protected float parse16Fixed(byte[] body, int pos) {
		int intg = body[pos] & 0xFF;
		int frac = body[pos + 1] & 0xFF;
		return intg + frac / 256f;
	}

	/** Parse a 24-bit value */
	static protected int parse24(byte[] body, int pos) {
		int b2 = body[pos] & 0xFF;
		int b1 = body[pos + 1] & 0xFF;
		int b0 = body[pos + 2] & 0xFF;
		return (b2 << 16) | (b1 << 8) | b0;
	}

	/** Parse a 24-bit fixed-point value.
	 * @return Null if the controller indicates an error */
	static protected Float parse24Fixed(byte[] body, int pos) {
		int flag = (body[pos] >> 7) & 0x01;
		if (flag == 0)
			return null;
		int b1 = body[pos] & 0x7F;
		int b0 = body[pos + 1] & 0xFF;
		short sint = (short) ((b1 << 8) | b0);
		int intg = (short) (sint << 1) >> 1;	// extend sign
		int frac = body[pos + 2] & 0xFF;
		int fr = intg >= 0 ? frac : -frac;
		return intg + fr / 256f;
	}

	/** Parse a string value */
	static protected String parseString(byte[] body, int pos, int len)
		throws IOException
	{
		return new String(body, pos, len, ASCII).trim();
	}

	/** Parse a date / time stamp */
	static protected long parseDate(byte[] body, int pos) {
		int date = parse32(body, pos);
		int time = parse32(body, pos + 4);
		int year = (date >> 9) & 0x0FFF;
		int month = (date >> 5) & 0x0F;
		int day = date & 0x1F;
		int hour = (time >> 22) & 0x1F;
		int minute = (time >> 16) & 0x3F;
		int second = (time >> 10) & 0x3F;
		int ms = time & 0x3FF;
		TimeZone utc = TimeZone.getTimeZone("GMT");
		Calendar cal = Calendar.getInstance(utc);
		cal.set(year, month - 1, day, hour, minute, second);
		cal.set(Calendar.MILLISECOND, ms);
		return cal.getTimeInMillis();
	}

	/** Parse the result code.
	 * @param rbody Received response body.
	 * @throws ParsingException On any errors parsing result code.
	 * @throws ControllerException If result indicates an error. */
	static void parseResult(byte[] rbody) throws IOException {
		if (rbody.length != 6)
			throw new ParsingException("RESULT LENGTH");
		int result = parse16(rbody, OFF_RESULT);
		ResponseCode rc = ResponseCode.fromCode(result);
		if (rc != ResponseCode.NO_ERRORS)
			throw new ControllerException(rc.toString());
	}

	/** Source sub ID */
	private final int source_sub_id = SUB_ID;

	/** Source ID */
	private final int source_id = 0;

	/** Destination sub ID */
	private final int dest_sub_id = SUB_ID;

	/** Packet sequence number */
	private byte seq_num = 0;

	/** Message sub ID */
	protected int msg_sub_id = DONT_CARE_MSG_SUB_ID;

	/** Constructor */
	public SS125Property() {
		ctl_name = "";
	}

	/** Constructor */
	public SS125Property(String cn) {
		ctl_name = (cn == null ? "" : cn);
	}

	/** Log a message */
	protected void log(String msg) {
		if (SS125Poller.SS125_LOG.isOpen())
			SS125Poller.SS125_LOG.log(ctl_name + " " + msg);
	}

	/** Log a message */
	protected void logError(String msg) {
		if (SS125Poller.SS125_LOG.isOpen())
			SS125Poller.SS125_LOG.log(ctl_name + "! " + msg);
	}

	/** Format a request header.
	 * @param body Body of message to send.
	 * @param drop Destination ID (drop address).
	 * @return Header appropriate for polling message. */
	protected byte[] formatHeader(byte[] body, int drop) throws IOException{
		if (!isAddressValid(drop))
			throw new InvalidAddressException(drop);
		assert (body.length - 1) <= MAX_BODY_OCTETS;
		byte[] header = new byte[11];
		header[OFF_SENTINEL] = 'Z';
		header[OFF_PROTOCOL_VER] = '1';
		if (SS125Poller.BROADCAST) {
			// Broadcast to all devices on subnet, useful
			// for resolving connection issues.
			format8(header, OFF_DEST_SUB_ID, 0xFF);
			format16(header, OFF_DEST_ID, 0xFFFF);
		} else {
			// traditional values
			format8(header, OFF_DEST_SUB_ID, dest_sub_id);
			format16(header, OFF_DEST_ID, drop);
		}
		format8(header, OFF_SOURCE_SUB_ID, source_sub_id);
		format16(header, OFF_SOURCE_ID, source_id);
		format8(header, OFF_SEQUENCE, seq_num);
		format8(header, OFF_BODY_SIZE, body.length - 1);
		format8(header, OFF_CRC, calculate(header));
		log("SS125Property.formatHeader: dest_sub_id="+
			dest_sub_id);
		log("SS125Property.formatHeader: src_sub_id="+
			source_sub_id);
		log("SS125Property.formatHeader: source_id="+
			source_id);
		log("SS125Property.formatHeader: seq_num="+
			seq_num);
		log("SS125Property.formatHeader: body_len="+
			body.length);
		log("SS125Property.formatHeader: crc="+
			calculate(header));
		return header;
	}

	/** Test the if property has some data */
	public boolean hasData() {
		return false;
	}

	/** Decode a message header.
	 * @param is Input stream to decode from.
	 * @param drop Destination ID (drop address).
	 * @return Number of bytes in response body.
	 * @throws IOException on error. */
	private int decodeHead(InputStream is, int drop) throws IOException {
		log("SS125Property.decodeHead: called: drop=" + drop);
		byte[] rhead = recvResponse(is, 11);
		log("SS125Property.decodeHead: read head=" + 
			HexString.format(rhead));
		if (parse8(rhead, OFF_CRC) != calculate(rhead))
			throw new ChecksumException("HEADER");
		if (rhead[OFF_SENTINEL] != 'Z')
			throw new ParsingException("SENTINEL");
		if (rhead[OFF_PROTOCOL_VER] != '1')
			throw new ParsingException("VERSION");
		if (parse8(rhead, OFF_DEST_SUB_ID) != source_sub_id)
			throw new ParsingException("DEST SUB ID");
		if (parse16(rhead, OFF_DEST_ID) != source_id)
			throw new ParsingException("DEST ID");
		if (parse8(rhead, OFF_SOURCE_SUB_ID) != dest_sub_id)
			throw new ParsingException("SRC SUB ID");
		if(parse16(rhead, OFF_SOURCE_ID) != drop) {
			// WYDOT specific change due to their comm config
			logError("warning mismatch: off_source_id=" + 
				parse16(rhead, OFF_SOURCE_ID) + " drop=" + drop);
			//throw new ParsingException("SRC ID");
		}
		seq_num++;
		if (parse8(rhead, OFF_SEQUENCE) != seq_num)
			throw new ParsingException("SEQUENCE");
		int n_body = parse8(rhead, OFF_BODY_SIZE);
		if (n_body < 3 || n_body > MAX_BODY_OCTETS)
			throw new ParsingException("BODY SIZE");
		log("SS125Property.decodeHead: n_bytes_in_body=" + n_body);
		log("SS125Property.decodeHead: read head with no problems");
		return n_body;
	}

	/** Format a message body.
	 * @param body Body buffer.
	 * @param mt Message type. */
	protected void formatBody(byte[] body, MessageType mt) {
		format8(body, OFF_MSG_ID, msgId().id);
		format8(body, OFF_MSG_SUB_ID, msgSubId());
		format8(body, OFF_MSG_TYPE, mt.code);
	}

	/** Decode a message response body.
	 * @param is Input stream to decode from.
	 * @param n_body Number of bytes in response body.
	 * @param mt Message type of request.
	 * @throws IOException on error. */
	private byte[] decodeBody(InputStream is, int n_body, MessageType mt)
		throws IOException
	{
		log("SS125Property.decodeBody: expected body len=" + n_body);
		byte[] rbody = recvResponse(is, n_body + 1);
		if (rbody.length < 4)
			throw new ParsingException("BODY SIZE");
		if (parse8(rbody, rbody.length - 1) != calculate(rbody))
			throw new ChecksumException("BODY CRC");
		log("SS125Property.decodeBody: body crc is good");
		MessageID mid = MessageID.fromCode(parse8(rbody, OFF_MSG_ID));
		if (mid != msgId())
			throw new ParsingException("MESSAGE ID");
		log("SS125Property.decodeBody: msg id matches: " + 
			mid + "=" + msgId());
		if(!parseMsgSubId(rbody)) {
			log("SS125Property.decodeBody: EX");
			throw new ParsingException("MESSAGE SUB ID");
		}
		MessageType rmt = MessageType.fromCode(parse8(rbody,
			OFF_MSG_TYPE));
		log("SS125Property.decodeBody: remote msg_type=" + rmt);
		if (rmt == MessageType.RESULT) {
			if (rbody.length != 6)
				throw new ParsingException("RESULT SIZE");
		} else if (rmt != mt)
			throw new ParsingException("MESSAGE TYPE");	
		log("SS125Property.decodeBody: returning body=" + 
			HexString.format(rbody));
		return rbody;
	}

	/** Validate the message sub id in the message response body */
	protected boolean parseMsgSubId(byte[] rbody) {
		// previously checked that matched current msg_sub_id; now we just override
		msg_sub_id = parse8(rbody, OFF_MSG_SUB_ID);
		return true;
	}

	/** Get the message ID */
	abstract protected MessageID msgId();

	/** Get the message sub-ID */
	protected int msgSubId() {
		return msg_sub_id;
	}

	/** Encode a QUERY request */
	@Override
	public void encodeQuery(ControllerImpl c, OutputStream os)
		throws IOException
	{
		byte[] body = formatQuery();
		byte[] header = formatHeader(body, c.getDrop());
		format8(body, body.length - 1, calculate(body));
		os.write(header);
		os.write(body);
	}

	/** Format a QUERY request */
	protected byte[] formatQuery() throws IOException {
		throw new ProtocolException("QUERY not supported");
	}

	/** Decode a QUERY response */
	@Override
	public void decodeQuery(ControllerImpl c, InputStream is)
		throws IOException
	{
		int n_body = decodeHead(is, c.getDrop());
		byte[] body = decodeBody(is, n_body, MessageType.READ);
		parseQuery(body);
	}

	/** Parse a QUERY response */
	protected void parseQuery(byte[] body) throws IOException {
		throw new ProtocolException("QUERY not supported");
	}

	/** Encode a STORE request */
	@Override
	public void encodeStore(ControllerImpl c, OutputStream os)
		throws IOException
	{
		byte[] body = formatStore();
		byte[] header = formatHeader(body, c.getDrop());
		format8(body, body.length - 1, calculate(body));
		log("SS125Property.encodeQuery: hdr sending=" + 
			HexString.format(header));
		log("SS125Property.encodeQuery: body sending=" + 
			HexString.format(body));
		log("SS125Property.encodeQuery: writing header+body...");
		os.write(header);
		os.write(body);
		log("SS125Property.encodeQuery: wrote header+body.");
	}

	/** Format a STORE request */
	protected byte[] formatStore() throws IOException {
		throw new ProtocolException("STORE not supported");
	}

	/** Decode a STORE response */
	@Override
	public void decodeStore(ControllerImpl c, InputStream is)
		throws IOException
	{
		int n_body = decodeHead(is, c.getDrop());
		byte[] body = decodeBody(is, n_body, MessageType.WRITE);
		parseResult(body);
	}
}
