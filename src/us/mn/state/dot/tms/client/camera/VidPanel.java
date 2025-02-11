/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2019-2020  SRF Consulting Group
 * Copyright (C) 2018  Iteris Inc.
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
package us.mn.state.dot.tms.client.camera;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.net.URL;
import javax.imageio.ImageIO;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.BevelBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import us.mn.state.dot.sched.Job;
import us.mn.state.dot.sched.Scheduler;
import us.mn.state.dot.sched.TimeSteward;
import us.mn.state.dot.tms.Camera;
import us.mn.state.dot.tms.SystemAttrEnum;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.camera.VideoRequest.Size;

/** JPanel that shows video.
 *
 * This class handles:
 *   Switching between cameras.
 *   Switching between available streams for the current camera.
 *   Manages camera name label at top of panel.
 *   Manages status/error label at bottom of panel.
 *   Manages MousePTZ link for panel.
 *
 * @author John L. Stanley - SRF Consulting
 * @author Michael Darter
 */
@SuppressWarnings("serial")
public class VidPanel extends JPanel implements FocusListener {

	/** Current camera */
	private Camera camera;

	/** List of available StreamReq(s) for current camera.
	 * (Only includes those that "should" work in current context.) */
	private List<VidStreamReq> streamReqList = new ArrayList<VidStreamReq>();

	/** Current stream request number */
	private int streamReqNum = 0;

	/** Current stream manager */
	private VidStreamMgr streamMgr;

	/** placeholder gray panel used while stopped */
	protected final JComponent placeholderComponent;

	/** dimension of video */
	private Dimension videoDimension;

	/** Camera PTZ control */
	private CameraPTZ cam_ptz;

	/** Mouse PTZ control */
	private MousePTZ mouse_ptz;

	/** Label that holds the video component */
	private final JPanel videoHolder;

	/** Still image read from URL */
	private JLabel imageHolder = new JLabel();

	/** streaming control values */
	private boolean autostart = true;
	private boolean failover = true;
	private int     connectFailSec = 10;
	private int     lostTimeoutSec = 10;
	private boolean autoReconnect = true;
	private int     reconnectTimeoutSec = 10;

	static private final Color LIGHT_BLUE = new Color(128, 128, 255);

	// Panel status monitor

	static private enum PanelStatus {
		IDLE,      // idle state when first created
		SCANNING,  // scanning for a viable stream
		VIEWING,   // watching a stream
		FAILED,    // initial scan of streams all failed
		RECONNECT, // auto-reconnecting after a lost stream
	}
	// STOPPED and FAILED are similar, but they
	// put different messages in the status line.

	PanelStatus panelStatus = PanelStatus.IDLE;

	private boolean pausePanel = false;
	private boolean streamError = false;

	private int timeoutSec = 0;

	private boolean repeatStatusMonitor = false;

	/** Status monitor job called once per second */
	private final Job statusMonitor = new Job(Calendar.SECOND, 1) {
		public void perform2() {
			int frames = getReceivedFrameCount();
			switch (panelStatus) {
				case IDLE:
				case FAILED:
					// do nothing
					break;
				case SCANNING:
					// see if current stream starts in a reasonable time
					if (frames > 0) {
						timeoutSec = 0;
						panelStatus = PanelStatus.VIEWING;
					}
					else if (++timeoutSec >= connectFailSec) {
						if (failover) {
							timeoutSec = 0;
							if (startNextStream())
								return;
						}
						panelStatus = PanelStatus.FAILED;
						stopStream();
					}
					break;
				case VIEWING:
					// see if we're receiving frames regularly
					if (frames > 0) {
						timeoutSec = 0;
					}
					else if (++timeoutSec >= lostTimeoutSec) {
						if (autoReconnect) {
							panelStatus = PanelStatus.RECONNECT;
							timeoutSec = 0;
							startCurrentStream();
						}
						else {
							panelStatus = PanelStatus.FAILED;
							stopStream();
						}
					}
					break;
				case RECONNECT:
					// trying to reconnect
					if (frames > 0) {
						timeoutSec = 0;
						panelStatus = PanelStatus.VIEWING;
					}
					else if (++timeoutSec >= reconnectTimeoutSec) {
						timeoutSec = 0;
						startCurrentStream();
					}
			}
		}

		@Override
		public void perform() {
			if (repeatStatusMonitor)
				perform2();
			queueUpdatePanel();
		}

		/** Check if this is a repeating job */
		@Override
		public boolean isRepeating() {
			return repeatStatusMonitor;
		}
	};

	/** Start status monitor */
	private void startStatusMonitor() {
		repeatStatusMonitor = true;
		PANEL_UPDATE.addJob(statusMonitor);
	}

	public void stopStatusMonitor() {
		repeatStatusMonitor = false;
		PANEL_UPDATE.removeJob(statusMonitor);
		panelStatus = PanelStatus.IDLE;
		timeoutSec = 0;
	}

	/** Create fixed-size video panel */
	public VidPanel(Size sz) {
		this(sz.width, sz.height);
	}

	/** Create fixed-size video panel */
	public VidPanel(Dimension dim) {
		this(dim.width, dim.height);
	}

	/** Create fixed-size video panel with specified stream */
	public VidPanel(Dimension dim, int strm_num) {
		this(dim.width, dim.height);
		streamReqNum = strm_num;
	}

	/** Create fixed-size video panel */
	public VidPanel(int width, int height) {
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		addFocusListener(this);

		videoDimension = new Dimension(width, height);
		placeholderComponent = new JPanel(new BorderLayout());
		placeholderComponent.setPreferredSize(videoDimension);
		placeholderComponent.setMinimumSize(videoDimension);
		placeholderComponent.setBackground(Color.LIGHT_GRAY);

		videoHolder = new JPanel(new BorderLayout());
		videoHolder.setPreferredSize(videoDimension);
		videoHolder.setMinimumSize(videoDimension);
		videoHolder.setBackground(Color.LIGHT_GRAY);
		videoHolder.add(placeholderComponent, BorderLayout.CENTER);

		addTopLabel(" ");
		if (videoStillEnabled()) {
			imageHolder = new JLabel(readStillImage(videoDimension, camera));
			add(imageHolder, BorderLayout.CENTER);
		}
		add(videoHolder, BorderLayout.CENTER);
		addBottomLabel(" ");

		// Catch when panel using this is closed and
		// shut down the stream if it's running.
		addAncestorListener(new AncestorListener() {
			@Override
			public void ancestorAdded(AncestorEvent event) {
				startStatusMonitor();
				if (autostart)
					startCurrentStream();
			}
			@Override
			public void ancestorMoved(AncestorEvent event) {}
			@Override
			public void ancestorRemoved(AncestorEvent event) {
				stopStatusMonitor();
				releaseStream();
			}
		});

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent arg0) {
				if (mouse_ptz != null) {
					Dimension sz = videoHolder.getSize();
					mouse_ptz.resize(sz.width, sz.height);
				}
			}
			@Override
			public void componentShown(ComponentEvent e) {
			}
			@Override
			public void componentHidden(ComponentEvent e) {
			}
		});

		setFocusable(true);
		setupKeyBindings();
	}

	/** Setup key bindings on the panel */
	private void setupKeyBindings() {
		InputMap im = getInputMap(
				JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		ActionMap am = getActionMap();

		/* Ctrl + Right Arrow - Start next stream */
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,
				KeyEvent.ALT_DOWN_MASK), "startNextStream");
		am.put("startNextStream", startNextStreamAction);

		/* Ctrl + Left Arrow - Start previous stream */
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,
				KeyEvent.ALT_DOWN_MASK), "startPreviousStream");
		am.put("startPreviousStream", startPreviousStreamAction);

		/* Ctrl + F5 - Restart stream */
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5,
				KeyEvent.ALT_DOWN_MASK), "restartStream");
		am.put("restartStream", restartStream);

		/* Ctrl + Space - Pause stream */
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH,
				KeyEvent.ALT_DOWN_MASK), "pauseStream");
		am.put("pauseStream", pauseStream);
	}

	private Action startNextStreamAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			startNextStream();
		}
	};

	private Action startPreviousStreamAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			startPreviousStream();
		}
	};

	/** Restart the stream that is currently playing. */
	private Action restartStream = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			panelStatus = PanelStatus.SCANNING;
			timeoutSec = 0;
			startCurrentStream();
		}
	};

	/** Pause the video playing in the panel. */
	private Action pauseStream = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			pausePanel = !pausePanel;
			if (pausePanel)
				stopStream();
			else
				switch (panelStatus) {
					case FAILED:
					case IDLE:
						break; // do nothing
					case SCANNING:
						if (streamError)
							startNextStream();
						else
							startCurrentStream();
						break;
					case RECONNECT:
					case VIEWING:
						startCurrentStream();
				}
			queueUpdatePanel();
		}
	};

	public Dimension getVideoDimension() {
		return videoDimension;
	}

	VidStreamMgr getStreamMgr() {
		return streamMgr;
	}

	//-------------------------------------------
	// The updatePanel job is run 0.1 seconds
	// after queueUpdatePanel() is called.  If
	// it's called more than once, the 0.1 sec
	// delay is reset, allowing several things
	// to be changed with only 1 updatePanel.

	static private boolean isNothing(String str) {
		return ((str == null) || str.isEmpty());
	}

	/** Shared VideoPanel update thread */
	static protected final Scheduler
		PANEL_UPDATE = new Scheduler("VideoPanels");

	/** job to rebuild the panel */
	private final Job updatePanel = new Job(100) {
		public void perform() {
			Camera     cam;
			VidStreamMgr smgr;
			removeAll();
			synchronized (this) {
				cam  = camera;
				smgr = streamMgr;
			}
			if (cam == null) {
				// no camera selected
				addTopLabel("");
				addVideo(placeholderComponent);
				addBottomLabel("");
			}
			else {
				// camera selected
				addTopLabel(cam.getName());
				if (smgr == null) {
					// no stream manager
					addVideo(placeholderComponent);
					addBottomLabel("");
				}
				else {
					// stream manager available
					JComponent vc = smgr.getComponent();
					addVideo(vc);
					String lbl = smgr.getLabel();
					String msg = smgr.getErrorMsg();
					streamError = !isNothing(msg);
					if (streamError)
						addBottomLabel(lbl+": "+msg,
								Color.BLACK,
								Color.ORANGE);
					else {
						msg = smgr.getStatus();
						if (isNothing(msg))
							addBottomLabel(lbl);
						else
							addBottomLabel(lbl+": "+msg);
					}
				}
			}
			revalidate();
			repaint();
			queueFireChangeListeners();
		}
	};

	/** Add top label line */
	private void addTopLabel(String txt) {
		Color nameColorBG;
		Color nameColorFG = Color.BLACK;
		if (VidPanel.this.isFocusOwner())
			nameColorBG = pausePanel
					? Color.BLUE
					: Color.WHITE;
		else
			nameColorBG = pausePanel
					? LIGHT_BLUE
					: Color.LIGHT_GRAY;
		if (pausePanel)
			nameColorFG = Color.WHITE;
		add(createLabel(txt, nameColorFG, nameColorBG),
			BorderLayout.NORTH);
	}

	private void addVideo(JComponent vidcomp) {
		videoHolder.removeAll();
		videoHolder.add(vidcomp, BorderLayout.CENTER);
		add(videoHolder);
	}

	/** Add bottom label line */
	private void addBottomLabel(String txt) {
		addBottomLabel(txt, null, null);
	}

	/** Add bottom label line, with optional colors */
	private void addBottomLabel(String txt, Color fgColor, Color bgColor) {
		add(createLabel(txt, fgColor, bgColor), BorderLayout.SOUTH);
	}

	/** Create a label */
	private JLabel createLabel(String txt) {
		return createLabel(txt, null, null);
	}

	/** Create a label.
	 * (Adds a tool-tip if the text is wider than the label */
	private JLabel createLabel(String txt, Color fgColor, Color bgColor) {
		JLabel lbl = new JLabel(txt);
		lbl.setHorizontalAlignment(JLabel.CENTER);
		FontMetrics lblFontMetrics = lbl.getFontMetrics(lbl.getFont());
		if (fgColor != null)
			lbl.setForeground(fgColor);
		if (bgColor != null) {
			lbl.setOpaque(true);
			lbl.setBackground(bgColor);
		}
		lbl.setAlignmentX(CENTER_ALIGNMENT);
		int txtWidth = lblFontMetrics.stringWidth(txt) + 2;
		int lblWidth = lbl.getWidth();
		if (txtWidth > lblWidth)
			lbl.setToolTipText(txt);
		else
			lbl.setToolTipText(null);
		return lbl;
	}

	public void queueUpdatePanel() {
		PANEL_UPDATE.removeJob(updatePanel);
		PANEL_UPDATE.addJob(updatePanel);
	}

	public void focusGained(FocusEvent fe) {
		queueUpdatePanel();
	}

	public void focusLost(FocusEvent fe){
		queueUpdatePanel();
	}

	//-------------------------------------------
	// Methods to set camera and manage streaming

	/** Get Boolean value from cameraTemplate
	 *  or a boolean from SystemAttrEnum value
	 *  if the CameraTemplate Boolean is null.
	 */
	private boolean getCamTempBool(Boolean camBool, SystemAttrEnum deflt) {
		if (camBool != null)
			return camBool.booleanValue();
		return deflt.getBoolean();
	}

	/** Get Integer value from cameraTemplate
	 *  or an int from SystemAttrEnum value
	 *  if the CameraTemplate Integer is null.
	 */
	private int getCamTempInt(Integer camInt, SystemAttrEnum deflt) {
		if (camInt != null)
			return camInt;
		return deflt.getInt();
	}

	/** Set camera, initialize sreqList,
	 *  and start playing first stream.
	 *
	 * @param c Camera
	 * @return true if stream available, false if none available.
	 */
	public boolean setCamera(Camera cam) {
		releaseStream();
		camera = cam;

		autostart = getCamTempBool(
				null,
				SystemAttrEnum.VID_CONNECT_AUTOSTART);
		failover = getCamTempBool(
				null,
				SystemAttrEnum.VID_CONNECT_FAIL_NEXT_SOURCE);
		connectFailSec = getCamTempInt(
				null,
				SystemAttrEnum.VID_CONNECT_FAIL_SEC);
		lostTimeoutSec = getCamTempInt(
				null,
				SystemAttrEnum.VID_LOST_TIMEOUT_SEC);
		autoReconnect = getCamTempBool(
				null,
				SystemAttrEnum.VID_RECONNECT_AUTO);
		reconnectTimeoutSec = getCamTempInt(
				null,
				SystemAttrEnum.VID_RECONNECT_TIMEOUT_SEC);

		Session s = Session.getCurrent();
		streamReqList = VidStreamReq.getVidStreamReqs(camera);
		streamReqNum = 0;
		cam_ptz = new CameraPTZ(s);
		cam_ptz.setCamera(cam);
		if (mouse_ptz != null)
			mouse_ptz.dispose();
		videoDimension = videoHolder.getSize();
		mouse_ptz = createMousePTZ(cam_ptz, videoDimension, videoHolder);
		boolean ret = !streamReqList.isEmpty();
		panelStatus = PanelStatus.IDLE;
		if (autostart) {
			if (playStream(streamReqNum)) {
				panelStatus = PanelStatus.SCANNING;
				ret = true;
			}
		}
		startStatusMonitor();
		updateStillImage();
		return ret;
	}

	/** Resize image */
	static private Image resizeImage(Dimension sz, Image img) {
		if (sz == null || img == null)
			return img;
		System.err.println("resizeImage: resizing to: " +
			sz.getWidth() + "/" + sz.getHeight());
		Image img2 = img.getScaledInstance(
			(int)sz.getWidth(), 
			(int)sz.getHeight(), 
			Image.SCALE_DEFAULT);
		return img2;
	}

	/** Read the remote image
	 * @param sz Dimension of JPanel in which image will be displayed
	 * @param cam Camera to read still image for
	 * @return Still image or null on error */
	static private ImageIcon readStillImage(Dimension sz, Camera cam) {
		if (cam == null)
			return null;
		String addr = cam.getEncAddress();
		Integer port = cam.getEncPort();
		if (addr == null || port == null)
			return null;
		String uri = addr+":"+port;
		try {
			System.out.println("readStillImage: reading...");
			BufferedImage img = ImageIO.read(new URL(uri.trim()));
			System.out.println("readStillImage: ...read " + uri);
			System.out.println("readStillImage: x=" + 
				img.getWidth() + " y=" + img.getHeight() + 
				" type=" + img.getType() + " img=" + 
				img.toString());
			return new ImageIcon(resizeImage(sz, img));
		} catch (Exception ex) {
			System.out.println("readStillImage: ex=" + ex);
			return null;
		}
	}

	/** Update still camera image */
	public void updateStillImage() {
		if (!videoStillEnabled())
			return;
		System.out.println("----------still image timer: camera=" + 
			(camera == null ? "null" : camera.getName()) + " t=" + 
			TimeSteward.currentDateTimeString(true) );
		Dimension sz = imageHolder.getSize(null);
		ImageIcon newimg = readStillImage(sz, camera);
		if (newimg != null)
			imageHolder.setIcon(newimg);
	}

	/** Video still functionality enabled? */
	static private boolean videoStillEnabled() {
		return SystemAttrEnum.VIDEO_STILL_ENABLE.getBoolean();
	}

	/** Create a mouse PTZ */
	static private MousePTZ createMousePTZ(CameraPTZ cam_ptz,
			Dimension sz,
			JPanel video_pnl)
	{
		return (cam_ptz != null)
		      ? new MousePTZ(cam_ptz, sz, video_pnl)
		      : null;
	}

	/** Start/restart playing stream number n.
	 * Automatically wraps at both ends of
	 * request list.  Returns false if
	 * stream not available. */
	private boolean playStream(int snum) {
		releaseStream();
		List<VidStreamReq> srl = streamReqList;
		int len = (srl == null) ? 0 : srl.size();
		if (len == 0) {
			streamReqNum = 0;
			queueUpdatePanel();
			return false;
		}
		if (snum < 0)
			snum = len - 1;
		else if (snum >= len)
			snum = 0;
		streamReqNum = snum;
		streamMgr = createStreamMgr(srl.get(snum));
		streamMgr.queueStartStream();
		queueUpdatePanel();
		return true;
	}

	/** Start playing previous stream */
	public boolean startPreviousStream() {
		return playStream(streamReqNum - 1);
	}

	/** Start/restart playing current stream */
	public boolean startCurrentStream() {
		return playStream(streamReqNum);
	}

	/** Start playing next stream */
	public boolean startNextStream() {
		return playStream(streamReqNum + 1);
	}

	/** Stop playing current stream.
	 *  (Leaves last frame and status on screen.) */
	public void stopStream() {
		VidStreamMgr vmOld = streamMgr;
		if (vmOld != null) {
			vmOld.queueStopStream();
			queueUpdatePanel();
		}
	}

	/** Release the current stream manager
	 * (Blanks the video portion of the panel.) */
	public void releaseStream() {
		VidStreamMgr vmOld = streamMgr;
		if (vmOld != null) {
			vmOld.queueStopStream();
			streamMgr = null;
			queueUpdatePanel();
		}
	}

	//-------------------------------------------

	/** Create a StreamMgr from a StreamReq */
	private VidStreamMgr createStreamMgr(VidStreamReq sreq) {
		if (sreq == null)
			return null;
		if (sreq.isGst())
			return new VidStreamMgrGst(this, sreq);
		else if (sreq.isMJPEG())
			return new VidStreamMgrMJPEG(this, sreq);
		return null;
	}

	/**
	 * @return
	 */
	public boolean isStreaming() {
		VidStreamMgr sm = streamMgr;
		if (sm == null)
			return false;
		return sm.isStreaming();
	}

	/**
	 * Gets number of frames received since this
	 * was last called.
	 */
	private int getReceivedFrameCount() {
		VidStreamMgr sm = streamMgr;
		if (sm == null)
			return 0;
		return sm.getReceivedFrameCnt();
	}

	//-------------------------------------------
	// Include a ChangeListener interface

	public void addChangeListener(ChangeListener listener) {
		listenerList.add(ChangeListener.class, listener);
	}

	public void removeChangeListener(ChangeListener listener) {
		listenerList.remove(ChangeListener.class, listener);
	}

	public ChangeListener[] getChangeListeners() {
		return listenerList.getListeners(ChangeListener.class);
	}

	/** Job to call any ChangeListeners */
	private final Job fireChangeListenersJob = new Job(100) {
		public void perform() {
			ChangeEvent event = new ChangeEvent(this);
			for (ChangeListener listener : getChangeListeners()) {
				listener.stateChanged(event);
			}
		}
	};

	/** Queue job to fire any ChangeListeners */
	private void queueFireChangeListeners() {
		PANEL_UPDATE.removeJob(fireChangeListenersJob);
		PANEL_UPDATE.addJob(fireChangeListenersJob);
	}
}
