package us.mn.state.dot.tms.client;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.function.BiConsumer;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Extends JTextField with placeholder text, change listeners
 * 
 * @author Isaac Nygaard
 * @copyright Iteris Inc.
 * @license GPL-2.0
 */
public class ITextField extends JTextField {	
	/** Placeholder text */
	private final String ph_text;
	/** Placeholder font */
	private final Font ph_font;
	/** Placeholder color */
	private final Color ph_color = Color.GRAY;
	
	public ITextField(String placeholder, int cols){
		super("", cols);
		ph_text = placeholder;
		ph_font = getFont().deriveFont(Font.ITALIC);
	}

	@Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
		// render placeholder text on top
		// https://stackoverflow.com/a/16229082/379572
        if (getText().isEmpty()){
			final Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(
				RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON
			);
			var insets = getInsets();
			g2.setFont(ph_font);
			g2.setColor(ph_color);
			g2.drawString(
				ph_text,
				insets.left,
				g.getFontMetrics().getMaxAscent() + insets.top
			);
		}
    }

	/** Call a listener when the text changes; listener is passed the (new, old)
	 * text as arguments
	 */
	public void addChangeListener(BiConsumer<String, String> cbk){
		// loosely based on https://stackoverflow.com/a/27190162/379572
		getDocument().addDocumentListener(new DocumentListener(){
			String cur_text = getText();
			private void check(){
				// double check text changed
				var prev_text = cur_text;
				cur_text = getText();
				if (!cur_text.equals(prev_text))
					cbk.accept(cur_text, prev_text);
			}
			@Override
			public void insertUpdate(DocumentEvent e){ check(); }
			@Override
			public void removeUpdate(DocumentEvent e){ check(); }
			@Override
			public void changedUpdate(DocumentEvent e){ check(); }
		});
	}
}
