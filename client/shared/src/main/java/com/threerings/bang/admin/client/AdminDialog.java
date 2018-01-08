//
// $Id$

package com.threerings.bang.admin.client;

import com.jmex.bui.*;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.text.IntegerDocument;
import com.jmex.bui.util.Dimension;
import com.samskivert.util.StringUtil;
import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.PlayerService;
import com.threerings.bang.client.bui.OptionDialog;
import com.threerings.bang.client.bui.SteelWindow;
import com.threerings.bang.data.Badge;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
import com.threerings.bang.tourney.client.TourneyListView;
import com.threerings.bang.tourney.data.TourneyListingEntry;
import com.threerings.bang.util.BangContext;
import com.threerings.presents.client.InvocationService;
import com.threerings.util.MessageBundle;

/**
 * Admin dialog, for performing a variety of actions
 */
public class AdminDialog extends SteelWindow
        implements ActionListener
{
    public static final int
            GRANT_SCRIP = 0,
            REMOVE_SCRIP = 1,
            RESET_SCRIP = 2,
            GRANT_BADGE = 3,
            REMOVE_BADGE = 4,
            RESET_BADGE = 5,
            GET_CONFIG = 6;

    /** The width to hint when laying out this window. */
    public static final int WIDTH_HINT = 875;
    public AdminDialog instance;
    public Handle _handle;

    private BComponent _valueField;

    private String _numberLabel;
    private int _action;

    public AdminDialog(BangContext ctx, Handle handle, String title, String numberLabel, int action)
    {
        super(ctx, title);
        instance = this;
        _ctx = ctx;
        _handle = handle;
        _numberLabel = numberLabel;
        _action = action;

        _msgs = _ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);
        setModal(true);
        _contents.setLayoutManager(new BorderLayout());
        showDialog();
    }

    protected BadgeList _badgeList;

    public void showDialog ()
    {
        switch (_action) {
            case GRANT_SCRIP: case REMOVE_SCRIP:
                add(3, new BLabel(_numberLabel));
                BTextField field = new BTextField("0", BangUI.TEXT_FIELD_MAX_LENGTH);
                add(4, _valueField = field, GroupLayout.FIXED);

                field.setPreferredWidth(300);
                field.setDocument(new IntegerDocument(true));
                field.requestFocus();
                break;
            case GRANT_BADGE: case REMOVE_BADGE:

                BLabel label;

                add(3, new BLabel(_numberLabel));
                add(4, label = new BLabel("No Badge Selected"));
                add(5, _badgeList = new BadgeList(new Dimension(450, 325), label), GroupLayout.FIXED);
                break;
        }

        _buttons.add(new BButton("Execute", this, "execute"));
        _buttons.add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"));
    }

    // from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        switch(action) {
            case "dismiss":
                _ctx.getBangClient().clearPopup(this, true);
                break;
            case "execute":
                if(_badgeList == null || _badgeList.getSelected() == null)
                {
                    if(_action == GRANT_SCRIP)
                    {
                        BTextField field = (BTextField)_valueField;
                        _ctx.getClient().requireService(PlayerService.class).adminAction(
                                _handle, 0, field.getText(),
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
                        return;
                    }
                    if(_action == REMOVE_SCRIP)
                    {
                        BTextField field = (BTextField)_valueField;
                        _ctx.getClient().requireService(PlayerService.class).adminAction(
                                _handle, 1, field.getText(),
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
                        return;
                    }
                }
                if(_action == RESET_SCRIP)
                {
                    _ctx.getClient().requireService(PlayerService.class).adminAction(
                            _handle, 2, "",
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
                    return;
                }
                Badge valueText = _badgeList.getSelected();
                if(valueText == null)  {
                    System.out.println("No badge selected!");
                    ((BLabel)getComponent(3)).setText("Badge: (Required)");
                    return;
                }

                _ctx.getClient().requireService(PlayerService.class).adminAction(
                        _handle, _action, String.valueOf(valueText.getCode()),
                        new InvocationService.ConfirmListener() {
                            @Override
                            public void requestProcessed() {
                                _ctx.getBangClient().clearPopup(instance, true); // Now close the popup
                                OptionDialog.showConfirmDialog(
                                        _ctx, null, "Success!", new String[]{"m.ok"}, (button, result) -> {
                                            // Automatically dismisses the dialog so nothing needed to be done here
                                        });
                            }

                            @Override
                            public void requestFailed(String cause) {
                                _ctx.getBangClient().clearPopup(instance, true); // Now close the popup
                                OptionDialog.showConfirmDialog(
                                        _ctx, null, "Failed!", new String[]{"m.ok"}, (button, result) -> {
                                            // Automatically dismisses the dialog so nothing needed to be done here
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
