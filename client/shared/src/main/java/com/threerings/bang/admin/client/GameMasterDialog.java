//
// $Id$

package com.threerings.bang.admin.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextField;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.PlayerService;
import com.threerings.bang.client.bui.OptionDialog;
import com.threerings.bang.client.bui.SteelWindow;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
import com.threerings.bang.util.BangContext;
import com.threerings.presents.client.InvocationService;
import com.threerings.util.MessageBundle;

/**
 * Admin dialog, for performing a variety of actions
 */
public class GameMasterDialog extends SteelWindow
    implements ActionListener
{
    public static final int
            WARN = 1,
            KICK = 2,
            TEMP_BAN = 3,
            PERMA_BAN = 4,
            WATCH_GAME = 5,
            SHOW_URL = 6;

    /** The width to hint when laying out this window. */
    public static final int WIDTH_HINT = 875;
    public GameMasterDialog instance;
    public Handle _handle;

    private BTextField _reasonField, _durationField;

    private int _action;

    public GameMasterDialog(BangContext ctx, Handle handle, String title, int action)
    {
        super(ctx, title);
        instance = this;
        _ctx = ctx;
        _handle = handle;
        _action = action;

        _msgs = _ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);
        setModal(true);
        _contents.setLayoutManager(new BorderLayout());
        showDialog();
    }

    public void showDialog ()
    {
        add(1, new BLabel("Reason:"));
        add(2, _reasonField = new BTextField("", BangUI.TEXT_FIELD_MAX_LENGTH), GroupLayout.FIXED);
        if (_action == TEMP_BAN) {
            add(3, new BLabel("Duration: ( X[s|m|h|d|w|M|y]... )"));
            add(4, _durationField = new BTextField("", BangUI.TEXT_FIELD_MAX_LENGTH), GroupLayout.FIXED);
        }

        _reasonField.setPreferredWidth(300);
        _reasonField.requestFocus();

        _buttons.add(new BButton("Execute", this, "execute"));
        _buttons.add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"));
    }

    private final long[] times = {
            1,
            20L,
            20L * 60,
            20L * 60 * 60,
            20L * 60 * 60 * 24,
            20L * 60 * 60 * 24 * 7,
            20L * 60 * 60 * 24 * 30,
            20L * 60 * 60 * 24 * 365
    };

    // from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        switch(action) {
            case "dismiss":
                _ctx.getBangClient().clearPopup(this, true);
                break;
            case "execute":
                String reason = _reasonField.getText().replaceAll(":", "");

                long duration = 0;
                if (_durationField != null) {
                    String str = _durationField.getText().replaceAll(":", "");

                    char c;
                    StringBuilder sb = new StringBuilder();

                    for (int i = 0, length = str.length(); i < length; ++i) {
                        switch (c = str.charAt(i)) {
                            case ' ': case '\t':
                                continue;
                            default:
                                int index = "tsmhdw".indexOf(c);
                                if (index >= 0) {
                                    try {
                                        duration += Long.parseLong(sb.toString()) * times[index];
                                        sb.setLength(0);
                                    } catch (NumberFormatException e) {
                                        OptionDialog.showConfirmDialog(
                                                _ctx, null, "Invalid duration!", new String[]{"m.ok"}, (button, result) -> {
                                                    // Automatically dismisses the dialog so nothing needed to be done here
                                                });
                                        return;
                                    }
                                } else {
                                    sb.append(c);
                                }
                        }
                    }
                    if (sb.length() > 0) {
                        try {
                            duration += Long.parseLong(sb.toString());
                        } catch (NumberFormatException ex) {
                            OptionDialog.showConfirmDialog(
                                    _ctx, null, "Invalid duration!", new String[]{"m.ok"}, (button, result) -> {
                                        // Automatically dismisses the dialog so nothing needed to be done here
                                    });
                            return;
                        }
                    }
                }

                _ctx.getClient().requireService(PlayerService.class).gameMasterAction(
                        _handle, _action, reason, duration,
                        new InvocationService.ConfirmListener() {
                            @Override
                            public void requestProcessed() {
                                _ctx.getBangClient().clearPopup(instance, true); // Now close the popup
                                OptionDialog.showConfirmDialog(
                                        _ctx, null, "Success!", new String[]{"m.ok"}, new OptionDialog.ResponseReceiver() {
                                            public void resultPosted(int button, Object result) {
                                                // Automatically dismisses the dialog so nothing needed to be done here
                                            }
                                        });
                            }

                            @Override
                            public void requestFailed(String cause) {
                                _ctx.getBangClient().clearPopup(instance, true); // Now close the popup
                                OptionDialog.showConfirmDialog(
                                        _ctx, null, "Failed!", new String[]{"m.ok"}, new OptionDialog.ResponseReceiver() {
                                            public void resultPosted(int button, Object result) {
                                                // Automatically dismisses the dialog so nothing needed to be done here
                                            }
                                        });
                            }
                });
                break;
        }
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
    }

    @Override // from BContainer
    protected void wasRemoved ()
    {
        super.wasRemoved();
    }

    protected BangContext _ctx;

    protected MessageBundle _msgs;
}
