//
// $Id$

package com.threerings.bang.admin.client;

import com.jmex.bui.BLabel;
import com.jmex.bui.BTextField;
import com.jmex.bui.Spacer;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.text.LengthLimitedDocument;
import com.threerings.bang.client.PlayerService;
import com.threerings.bang.client.bui.EnablingValidator;
import com.threerings.bang.client.bui.RequestDialog;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
import com.threerings.bang.gang.client.GangService;
import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.NameFactory;

/**
 * Allows Support to warn a player
 */
public class WarnPlayerDialog extends RequestDialog
{
    public WarnPlayerDialog(BangContext ctx, StatusLabel status)
    {
        this(ctx, status, null);
    }

    public WarnPlayerDialog(
        BangContext ctx, StatusLabel status, Handle handle)
    {
        super(ctx, null, "Confirm Warning", "Ok", "Cancel",
            "Warning Successful!", status);
        _handle = handle;

        setRequiresString(300, "");

        // if the handle is not specified already, add the fields to enter it
        if (_handle == null) {
            add(0, new BLabel("Player Name"), GroupLayout.FIXED);
            add(1, _hfield = new BTextField(NameFactory.getValidator().getMaxHandleLength()),
                GroupLayout.FIXED);
            _hfield.setPreferredWidth(200);
            new EnablingValidator(_hfield, _buttons[0]);
            add(2, new Spacer(1, 15), GroupLayout.FIXED);
        }

        // limit the length of the invite message, over 255 will break
        _input.setDocument(new LengthLimitedDocument(200));

        // shove the title up on top
        add(0, new BLabel("Warning a Player", "window_title"));
    }

    // documentation inherited
    protected void fireRequest (Object result)
    {
        PlayerService psvc = _ctx.getClient().requireService(PlayerService.class);
        if (_hfield != null) {
            _handle = new Handle(_hfield.getText());
        }
        psvc.warnPlayer(_handle, _input.getText(), new PlayerService.ConfirmListener() {

            @Override
            public void requestFailed(String s) {
                _ctx.getChatDirector().displayFeedback(null, "Failed to warn player.");
            }

            @Override
            public void requestProcessed() {
                _ctx.getChatDirector().displayFeedback(null, "Warning has been sent.");
            }
        });
        psvc.bootPlayer(_handle, new PlayerService.ConfirmListener() {
            @Override
            public void requestFailed(String s) { }
            @Override
            public void requestProcessed() { }
        });

        }

    protected Handle _handle;
    protected BTextField _hfield;
}
