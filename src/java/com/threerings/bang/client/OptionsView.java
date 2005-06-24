//
// $Id$

package com.threerings.bang.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Level;

import com.jme.bui.BButton;
import com.jme.bui.BCheckBox;
import com.jme.bui.BComboBox;
import com.jme.bui.BContainer;
import com.jme.bui.BDecoratedWindow;
import com.jme.bui.BLabel;
import com.jme.bui.event.ActionEvent;
import com.jme.bui.event.ActionListener;
import com.jme.bui.layout.GroupLayout;

import com.samskivert.util.CollectionUtil;
import com.samskivert.util.RunAnywhere;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.util.EscapeListener;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * Allows options to be viewed and adjusted. Presently that's just video
 * mode and whether or not we're in full screen mode.
 */
public class OptionsView extends BDecoratedWindow
    implements ActionListener
{
    public OptionsView (BangContext ctx, LogonView parent)
    {
        super(ctx.getLookAndFeel(), null);
        setLayoutManager(GroupLayout.makeVStretch());
        _modal = true;

        _ctx = ctx;
        _parent = parent;
        _msgs = ctx.getMessageManager().getBundle("options");

        addListener(new EscapeListener() {
            public void escapePressed() {
                dismiss();
            }
        });

        add(new BLabel(_msgs.get("m.title")));

        BContainer cont = GroupLayout.makeButtonBox(GroupLayout.LEFT);
        cont.add(new BLabel(_msgs.get("m.video_mode")));
        cont.add(_modes = new BComboBox());
        _modes.addListener(_modelist);
        add(cont);

        cont = GroupLayout.makeButtonBox(GroupLayout.LEFT);
        cont.add(_fullscreen = new BCheckBox(_msgs.get("m.fullscreen_mode")));
        _fullscreen.setChecked(Display.isFullscreen());
        _fullscreen.addListener(_modelist);
        add(cont);

        cont = GroupLayout.makeButtonBox(GroupLayout.RIGHT);
        BButton btn;
        cont.add(btn = new BButton(_msgs.get("m.dismiss"), "dismiss"));
        btn.addListener(this);
        add(cont);

        _mode = Display.getDisplayMode();
        refreshDisplayModes();
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if ("dismiss".equals(event.getAction())) {
            dismiss();
        }
    }

    protected void refreshDisplayModes ()
    {
        try {
            ArrayList<DisplayMode> modes = new ArrayList<DisplayMode>();
            CollectionUtil.addAll(modes, Display.getAvailableDisplayModes());
            for (Iterator<DisplayMode> iter = modes.iterator();
                 iter.hasNext(); ) {
                DisplayMode mode = iter.next();
                if (mode.getWidth() < 800 || mode.getHeight() < 600) {
                    iter.remove();
                }
            }

            // if there is only one display mode, and we're on Linux, it's
            // probably because of Xinerama wackiness, so slip in a couple
            // of sensible modes for use in non-fullscreen mode
            if (RunAnywhere.isLinux() && modes.size() < 2) {
                modes.add(new DisplayMode(1024, 768));
                modes.add(new DisplayMode(1280, 1024));
            }

            ModeItem current = null;
            ModeItem[] items = new ModeItem[modes.size()];
            for (int ii = 0; ii < items.length; ii++) {
                DisplayMode mode = (DisplayMode)modes.get(ii);
                items[ii] = new ModeItem(mode);
                if (mode.equals(_mode)) {
                    current = items[ii];
                }
            }
            Arrays.sort(items);
            _modes.setItems(items);
            _modes.selectItem(current);

        } catch (LWJGLException e) {
            log.log(Level.WARNING, "Failed to obtain display modes", e);
        }
    }

    protected void updateDisplayMode (DisplayMode mode)
    {
        if (_mode != null && _mode.equals(mode) &&
            Display.isFullscreen() == _fullscreen.isChecked()) {
            return;
        }
        if (mode != null) {
            log.info("Switching to " + mode + " (from " + _mode + ")");
            _mode = mode;
        }
        _ctx.getDisplay().recreateWindow(
            _mode.getWidth(), _mode.getHeight(), _mode.getBitsPerPixel(),
            _mode.getFrequency(), _fullscreen.isChecked());
        _parent.center();
        center();
        BangPrefs.updateDisplayMode(_mode);
        BangPrefs.updateFullscreen(_fullscreen.isChecked());
    }

    protected static class ModeItem implements Comparable
    {
        public DisplayMode mode;

        public ModeItem (DisplayMode mode) {
            this.mode = mode;
        }

        public String toString () {
            return mode.getWidth() + "x" + mode.getHeight() + "x" +
                mode.getBitsPerPixel() + " " + mode.getFrequency() + "Hz";
        }

        public int compareTo (Object other) {
            DisplayMode omode = ((ModeItem)other).mode;
            if (mode.getWidth() != omode.getWidth()) {
                return mode.getWidth() - omode.getWidth();
            } else if (mode.getHeight() != omode.getHeight()) {
                return mode.getHeight() - omode.getHeight();
            } else if (mode.getBitsPerPixel() != omode.getBitsPerPixel()) {
                return mode.getBitsPerPixel() - omode.getBitsPerPixel();
            } else {
                return mode.getFrequency() - omode.getFrequency();
            }
        }
    }

    protected ActionListener _modelist = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            updateDisplayMode(((ModeItem)_modes.getSelectedItem()).mode);
        }
    };

    protected BangContext _ctx;
    protected LogonView _parent;
    protected MessageBundle _msgs;
    protected DisplayMode _mode;

    protected BComboBox _modes;
    protected BCheckBox _fullscreen;
}
