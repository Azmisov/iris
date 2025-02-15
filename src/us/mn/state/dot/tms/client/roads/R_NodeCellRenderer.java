/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2000-2018  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.client.roads;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.Set;
import javax.swing.DefaultListCellRenderer;
import us.mn.state.dot.tms.Detector;
import javax.swing.JLabel;
import javax.swing.JList;
import us.mn.state.dot.tms.GeoLoc;
import us.mn.state.dot.tms.GeoLocHelper;
import us.mn.state.dot.tms.R_Node;
import us.mn.state.dot.tms.R_NodeTransition;
import us.mn.state.dot.tms.R_NodeType;
import static us.mn.state.dot.tms.client.widget.Widgets.UI;

/**
 * Renderer for roadway node cells in a list.
 *
 * @author Douglas Lau
 * @author Michael Darter
 */
public class R_NodeCellRenderer extends DefaultListCellRenderer {

	/** Background color for nodes with bad locations */
	static private final Color COLOR_NO_LOC = Color.RED;

	/** Background color for inactive nodes */
	static private final Color COLOR_INACTIVE = Color.LIGHT_GRAY;

	/** Color of lanes */
	static private final Color LANE_COLOR = Color.GRAY;

	/** Width of one lane */
	static private final int LANE_WIDTH = UI.scaled(20);

	/** Height of one lane */
	static private final int LANE_HEIGHT = UI.scaled(18);

	/** Total width of roadway node renderers */
	static private final int WIDTH = LANE_WIDTH * 22;

	/** Solid stroke line */
	static private final BasicStroke LINE_SOLID = new BasicStroke(8,
		BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	/** Dashed stroke line */
	static private final BasicStroke LINE_DASHED = new BasicStroke(4,
		BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1,
		new float[] { LANE_HEIGHT / 3, 2 * LANE_HEIGHT / 3 },
		2 * LANE_HEIGHT / 3
	);

	/** Basic stroke line */
	static private final BasicStroke LINE_BASIC = new BasicStroke(1);
	static private final BasicStroke LINE_BASIC2 = new BasicStroke(2);

	/** Font for cross-street labels */
	static private final Font FONT_XSTREET = new JLabel().getFont();

	/** Get the X-coordinate for the given shift.
	 * @param shift Shift index (0-12).
	 * @return X-coordinate to draw line. */
	static private int getShiftX(int shift) {
		return LANE_WIDTH * (2 + shift);
	}

	/** R_node model */
	private R_NodeModel model;

	/** R_Node */
	private R_Node r_node;

	/** R_Node type */
	private R_NodeType node_type;

	/** Roadway node manager */
	private final R_NodeManager manager;

	/** Constructor */
	public R_NodeCellRenderer(R_NodeManager rnm) {
		super();
		manager = rnm;
	}

	/** Set the r_node model */
	private void setModel(R_NodeModel m) {
		model = m;
		r_node = (m != null) ? model.r_node : null;
		node_type = (r_node != null)
		          ? R_NodeType.fromOrdinal(r_node.getNodeType())
		          : null;
	}

	/** Configure the renderer component */
	@Override
	public Component getListCellRendererComponent(JList<?> list,
		Object value, int index, boolean isSelected,
		boolean cellHasFocus)
	{
		setBackground(null);
		Component c = super.getListCellRendererComponent(list, value,
			index, isSelected, cellHasFocus);
		if (value instanceof R_NodeModel)
			setModel((R_NodeModel) value);
		setSelected(isSelected);
		return c;
	}

	/** Selected status */
	private boolean selected;

	/** Set the selected status of the component */
	private void setSelected(boolean sel) {
		selected = sel;
		if (!sel) {
			GeoLoc loc = r_node.getGeoLoc();
			if (GeoLocHelper.isNull(loc))
				setBackground(COLOR_NO_LOC);
			else if (!r_node.getActive())
				setBackground(COLOR_INACTIVE);
		}
	}

	/** Get the upstream line on the given side of the road */
	private int getUpstreamLine(boolean side) {
		return getShiftX(model.getUpstreamLane(side));
	}

	/** Get the downstream line on the given side of the road */
	private int getDownstreamLine(boolean side) {
		return getShiftX(model.getDownstreamLane(side));
	}

	/** Allow for subclasses to modify cross-street label */
	private String streetString(String street) {
		return street;
	}

	/** Paint the renderer */
	@Override
	public void paintComponent(Graphics g) {
		Dimension d = getSize();
		int width =  (int) d.getWidth();
		int height = (int) d.getHeight();
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
		                    RenderingHints.VALUE_ANTIALIAS_ON);
		fillBackground(g2, width, height);
		g2.setStroke(LINE_SOLID);
		drawYellowLines(g2, height);
		drawWhiteLines(g2, height);
		fillRoadway(g2, height);
		drawSkipStripes(g2, height);
		drawDetectors(g2, height);
		final Integer sp = getSpeed(manager.lookupDets(r_node));
		drawSpeedSign(g2, height, 0, r_node.getSpeedLimit(), true);
		drawSpeedSign(g2, height, 25, sp, false);
		String xl = GeoLocHelper.getCrossLandmark(r_node.getGeoLoc());
		if (xl.length() > 0)
			drawCrossLandmark(g2, xl, width, height);
		if (selected) {
			drawMarker(g2, height);
			drawShiftHandle(g2, height);
		}
	}

	/** Calculate station speed per lane
	 * @param ds A set of detectors in the station
	 * @return Null on error else speed per lane */
	public Integer getSpeed(Set<Detector> ds) {
		int total = 0;
		int count = 0;
		for(Detector d : ds) {
			if(d == null)
				continue;
			String did = d.getName();
			SensorSample ss = manager.getSample(did);
			if(ss ==  null)
				continue;
			Integer sp = ss.getSpeed();
			if(sp != null) {
				total += sp;
				count++;
			}
		}
		if(count > 0)
			return total / count;
		else
			return null;
	}

	/** Fill the background */
	private void fillBackground(Graphics2D g, int width, int height) {
		g.setColor(getBackground());
		g.fillRect(0, 0, width, height);
		g.setColor(Color.LIGHT_GRAY);
		g.drawLine(0, 0, width, 0);
		g.drawLine(0, height, width, height);
	}

	/** Draw the yellow lines */
	private void drawYellowLines(Graphics2D g, int height) {
		g.setColor(Color.YELLOW);
		if (model.hasMainline())
			g.draw(createYellowMainLine(height));
		switch (node_type) {
		case ENTRANCE:
			g.draw(createEntranceYellow());
			break;
		case EXIT:
			g.draw(createExitYellow());
			break;
		}
	}

	/** Create the yellow main line */
	private Shape createYellowMainLine(int height) {
		int y0 = getDownstreamLine(true);
		int y1 = getUpstreamLine(true);
		return new Line2D.Double(y0, 0, y1, height);
	}

	/** Draw the white lines */
	private void drawWhiteLines(Graphics2D g, int height) {
		g.setColor(Color.WHITE);
		if (model.hasMainline())
			g.draw(createWhiteMainLine(height));
		switch (node_type) {
		case ENTRANCE:
			g.draw(createEntranceWhite());
			break;
		case EXIT:
			g.draw(createExitWhite());
			break;
		}
	}

	/** Create the white main line */
	private Shape createWhiteMainLine(int height) {
		int w0 = getDownstreamLine(false);
		int w1 = getUpstreamLine(false);
		return new Line2D.Double(w1, height, w0, 0);
	}

	/** Fill the roadway area */
	private void fillRoadway(Graphics2D g, int height) {
		g.setColor(LANE_COLOR);
		if (model.hasMainline())
			g.fill(createMainRoadway(height));
		switch (node_type) {
		case ENTRANCE:
			g.fill(createEntranceRoadway());
			break;
		case EXIT:
			g.fill(createExitRoadway());
			break;
		}
	}

	/** Create the mainline roadway area */
	private Shape createMainRoadway(int height) {
		GeneralPath path =new GeneralPath(createYellowMainLine(height));
		path.append(createWhiteMainLine(height), true);
		path.closePath();
		return path;
	}

	/** Draw the skip stripes */
	private void drawSkipStripes(Graphics2D g, int height) {
		g.setColor(Color.WHITE);
		g.setStroke(LINE_DASHED);
		if (model.hasMainline())
			drawMainlineSkipStripes(g, height);
		switch (node_type) {
		case ENTRANCE:
			for (int lane = 1; lane < r_node.getLanes(); lane++)
				g.draw(createEntranceRamp(lane, true));
			break;
		case EXIT:
			for (int lane = 1; lane < r_node.getLanes(); lane++)
				g.draw(createExitRamp(lane, true));
			break;
		}
	}

	/** Draw the mainline skip stripes */
	private void drawMainlineSkipStripes(Graphics2D g, int height) {
		int left0 = getDownstreamLine(true);
		int left1 = getUpstreamLine(true);
		int right0 = getDownstreamLine(false);
		int right1 = getUpstreamLine(false);
		int left = Math.max(left0, left1);
		if (left0 == left1)
			left += LANE_WIDTH;
		int right = Math.min(right0, right1);
		if (right0 == right1)
			right -= LANE_WIDTH;
		for (int i = left; i <= right; i += LANE_WIDTH)
			g.draw(new Line2D.Double(i, 0, i, height));
	}

	/** Create a ramp curve for the specified lane
	 * @param lane Number of lanes from the outside lane */
	private Shape createRamp(int lane, boolean reverse, int x, int y0,
		int y1, int y2, int y3)
	{
		int x1, x2, x3;
		if (r_node.getAttachSide()) {
			x1 = x - LANE_WIDTH * 3;
			x2 = x - LANE_WIDTH;
			x3 = x + LANE_WIDTH * lane;
		} else {
			x1 = x + LANE_WIDTH * 3;
			x2 = x + LANE_WIDTH;
			x3 = x - LANE_WIDTH * lane;
		}
		R_NodeTransition nt = R_NodeTransition.fromOrdinal(
			r_node.getTransition());
		GeneralPath path = new GeneralPath();
		if (reverse) {
			if (nt == R_NodeTransition.LOOP) {
				path.moveTo(x3, y3);
				path.curveTo(x3, y0, x1, y1, x1, y2);
			} else {
				path.moveTo(x3, y3);
				path.curveTo(x3, y1, x2, y1, x1, y1);
			}
		} else {
			if (nt == R_NodeTransition.LOOP) {
				path.moveTo(x1, y2);
				path.curveTo(x1, y1, x3, y0, x3, y3);
			} else {
				path.moveTo(x1, y1);
				path.curveTo(x2, y1, x3, y1, x3, y3);
			}
		}
		return path;
	}

	/** Get the y-position of the specified ramp lane */
	private int getRampLaneY(int lane) {
		return LANE_HEIGHT * (r_node.getLanes() - lane)
		     + LANE_HEIGHT / 2;
	}

	/** Create a ramp curve for the specified lane
	 * @param lane Number of lanes from the outside lane */
	private Shape createEntranceRamp(int lane, boolean reverse) {
		int x = getDownstreamLine(r_node.getAttachSide());
		int y = getPreferredHeight() - getRampLaneY(lane);
		int y0 = y + LANE_HEIGHT;
		int y1 = y;
		int y2 = y - LANE_HEIGHT / 2;
		int y3 = 0;
		return createRamp(lane, reverse, x, y0, y1, y2, y3);
	}

	/** Create the yellow (left side) fog line for an entrance ramp */
	private Shape createEntranceYellow() {
		if (r_node.getAttachSide())
			return createEntranceRamp(0, false);
		else
			return createEntranceRamp(r_node.getLanes(), false);
	}

	/** Create the white (right side) fog line for an entrance ramp */
	private Shape createEntranceWhite() {
		if (r_node.getAttachSide())
			return createEntranceRamp(r_node.getLanes(), true);
		else
			return createEntranceRamp(0, true);
	}

	/** Create an entrance roadway area */
	private Shape createEntranceRoadway() {
		GeneralPath path = new GeneralPath(createEntranceYellow());
		path.append(createEntranceWhite(), true);
		path.closePath();
		return path;
	}

	/** Create a ramp curve for the specified lane
	 * @param lane Number of lanes from the outside lane */
	private Shape createExitRamp(int lane, boolean reverse) {
		int x = getUpstreamLine(r_node.getAttachSide());
		int y = getRampLaneY(lane);
		int y0 = y - LANE_HEIGHT;
		int y1 = y;
		int y2 = y + LANE_HEIGHT / 2;
		int y3 = getPreferredHeight();
		return createRamp(lane, reverse, x, y0, y1, y2, y3);
	}

	/** Create the yellow (left side) fog line for an exit ramp */
	private Shape createExitYellow() {
		if (r_node.getAttachSide())
			return createExitRamp(0, false);
		else
			return createExitRamp(r_node.getLanes(), false);
	}

	/** Create the white (right side) fog line for an exit ramp */
	private Shape createExitWhite() {
		if (r_node.getAttachSide())
			return createExitRamp(r_node.getLanes(), true);
		else
			return createExitRamp(0, true);
	}

	/** Create an exit roadway area */
	private Shape createExitRoadway() {
		GeneralPath path = new GeneralPath(createExitRamp(0, false));
		path.append(createExitRamp(r_node.getLanes(), true), true);
		path.closePath();
		return path;
	}

	/** Draw the detector locations */
	private void drawDetectors(Graphics2D g, int height) {
		g.setStroke(LINE_BASIC);
		switch (node_type) {
		case STATION:
			drawStationDetectors(g);
			break;
		case ENTRANCE:
			drawEntranceDetectors(g, height);
			break;
		}
	}

	/** Draw station detector locations */
	private void drawStationDetectors(Graphics2D g) {
		final int y = 2;
		int r = getDownstreamLine(false) - LANE_WIDTH + 4;
		for (int i = 0; i < r_node.getLanes(); i++) {
			int x = r - LANE_WIDTH * i;
			drawDetector(g, x, y, Integer.toString(i + 1));
		}
	}

	/** Get X position to draw an HOV diamond */
	private int getHovDiamondX() {
		boolean side = r_node.getAttachSide();
		int x = getDownstreamLine(side);
		if (side)
			return x - LANE_WIDTH * 2;
		else
			return x + LANE_WIDTH;
	}

	/** Draw entrance detectors stuff */
	private void drawEntranceDetectors(Graphics2D g, int height) {
		R_NodeTransition nt = R_NodeTransition.fromOrdinal(
			r_node.getTransition());
		if (nt == R_NodeTransition.HOV) {
			int x = getHovDiamondX();
			int y = height - LANE_HEIGHT - 1;
			GeneralPath path = new GeneralPath();
			path.moveTo(x, y);
			path.lineTo(x + LANE_WIDTH / 2, y + LANE_HEIGHT / 3);
			path.lineTo(x + LANE_WIDTH, y);
			path.lineTo(x + LANE_WIDTH / 2, y - LANE_HEIGHT / 3);
			path.closePath();
			g.setStroke(LINE_BASIC2);
			g.draw(path);
		}
	}

	/** Draw a detector */
	private void drawDetector(Graphics2D g, int x, int y, String label) {
		drawText(g, x, y, label, Color.DARK_GRAY, Color.WHITE);
	}

	/** Draw a text label */
	private void drawText(Graphics2D g, int x, int y, String label,
		Color bg, Color fg)
	{
		GlyphVector gv = FONT_XSTREET.createGlyphVector(
			g.getFontRenderContext(), label);
		Rectangle2D rect = gv.getVisualBounds();
		Rectangle2D face = new Rectangle2D.Double(x, y,
			rect.getWidth() + UI.hgap * 2,
			rect.getHeight() + UI.vgap * 2);
		g.setColor(bg);
		g.fill(face);
		g.setColor(Color.BLACK);
		g.setStroke(LINE_BASIC);
		g.draw(face);
		g.setColor(fg);
		int tx = UI.hgap - 1;
		int ty = UI.vgap + (int) rect.getHeight() + 1;
		g.drawGlyphVector(gv, x + tx, y + ty);
	}

	/** Draw a speed sign with a number in the middle.
	 * @param g Graphics handle
	 * @param height Height of box
	 * @param margin Margin to the left of the box
	 * @param sp Speed to draw in box
	 * @param white True for white background else SpeedBand colors
	 */
	protected void drawSpeedSign(Graphics2D g, int height, int margin, 
		Integer sp, boolean white) 
	{
		if (sp == null)
			return;
		switch (node_type) {
		case STATION:
			int x = getDownstreamLine(false) + LANE_WIDTH + margin;
			int y = 2;
			String slim = String.valueOf(sp);
			Color c = (white ? Color.WHITE : 
				SpeedBand.getBand(sp).color);
			drawText(g, x, y, slim, c, Color.BLACK);
			break;
		}
	}

	/** Draw the cross-street / landmark label */
	private void drawCrossLandmark(Graphics2D g, String xl, int width,
		int height)
	{
		GlyphVector gv = FONT_XSTREET.createGlyphVector(
			g.getFontRenderContext(), xl);
		Rectangle2D rect = gv.getVisualBounds();
		int x = width - (int) rect.getWidth() - UI.hgap * 2;
		int y = (height + (int) rect.getHeight()) / 2;
		g.setColor(Color.BLACK);
		g.drawGlyphVector(gv, x, y);
	}

	/** Draw the r_node marker */
	private void drawMarker(Graphics2D g, int height) {
		int x = getShiftX(r_node.getShift());
		AffineTransform t = g.getTransform();
		g.translate(x, height / 2);
		Shape marker = new R_NodeMarker();
		g.setColor(SegmentTheme.R_NODE_COLOR);
		g.fill(marker);
		g.setColor(SegmentTheme.OUTLINE.color);
		g.setStroke(SegmentTheme.OUTLINE.getStroke(1));
		g.draw(marker);
		g.setTransform(t);
	}

	/** Draw the shift handle */
	private void drawShiftHandle(Graphics2D g, int height) {
		g.setColor(Color.DARK_GRAY);
		Shape path = createShiftHandle(height);
		g.fill(path);
		g.setColor(Color.WHITE);
		g.setStroke(LINE_BASIC2);
		g.draw(path);
	}

	/** Create an exit roadway area */
	private Shape createShiftHandle(int height) {
		int x = getShiftX(r_node.getShift());
		int w = LANE_WIDTH / 3;
		int h = LANE_HEIGHT / 2;
		GeneralPath path = new GeneralPath();
		path.moveTo(x + w, 1);
		path.lineTo(x, h);
		path.lineTo(x - w, 1);
		path.closePath();
		return path;
	}

	/** Get the preferred height of a station node */
	private int getPreferredStationHeight() {
		int delta = Math.max(getFogLaneDelta(false),
		                     getFogLaneDelta(true));
		return LANE_HEIGHT * (delta + 1);
	}

	/** Get the absolute change in the fog line lane for the given side */
	private int getFogLaneDelta(boolean side) {
		int up   = model.getUpstreamLane(side);
		int down = model.getDownstreamLane(side);
		return Math.abs(up - down);
	}

	/** Get the preferred height */
	private int getPreferredHeight() {
		switch (node_type) {
		case ENTRANCE:
		case EXIT:
			return LANE_HEIGHT * (r_node.getLanes() + 2);
		case STATION:
			return getPreferredStationHeight();
		}
		return LANE_HEIGHT;
	}

	/** Get the preferred renderer size */
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(WIDTH, getPreferredHeight());
	}
}
