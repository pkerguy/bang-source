//
// $Id$

package com.threerings.bang.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BComboBox;
import com.jmex.bui.BLabel;
import com.jmex.bui.BPasswordField;
import com.jmex.bui.BTextField;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.TextEvent;
import com.jmex.bui.event.TextListener;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.samskivert.net.MailUtil;
import com.samskivert.util.StringUtil;
import com.samskivert.servlet.user.InvalidUsernameException;
import com.samskivert.servlet.user.Password;
import com.samskivert.servlet.user.Username;

import com.threerings.bang.bang.client.BangDesktop;
import com.threerings.bang.steam.SteamStorage;
import com.threerings.util.MessageBundle;

import com.threerings.bang.client.bui.OptionDialog;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.client.bui.SteelWindow;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;
import static com.threerings.presents.data.AuthCodes.INVALID_PASSWORD;

/**
 * Allows a player to create an account.
 */
public class CreateAccountView extends SteelWindow
        implements ActionListener
{

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String cmd = event.getAction();
        if (cmd.equals("create")) {
            createAccount();
        } else if (cmd.equals("cancel")) {
            if (_onExit) {
                _ctx.getApp().stop();
            } else {
                _ctx.getBangClient().clearPopup(this, true);
            }
        }
    }

    protected CreateAccountView (BangContext ctx)
    {
        super(ctx, ctx.xlate(BangCodes.BANG_MSGS, "m.account_title"));
        setModal(true);
        setLayer(BangCodes.NEVER_CLEAR_LAYER);
        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);
        _contents.setLayoutManager(GroupLayout.makeVert(GroupLayout.CENTER).setGap(15));
        _contents.setStyleClass("padded");

        BContainer grid = new BContainer(new TableLayout(2, 5, 5));
        grid.add(new BLabel(_msgs.get("m.password"), "right_label"));
        grid.add(_password = new BPasswordField(32));
        _password.setPreferredWidth(150);
        grid.add(new BLabel(_msgs.get("m.repassword"), "right_label"));
        grid.add(_repassword = new BPasswordField(32));
        _repassword.setPreferredWidth(150);
        grid.add(new BLabel(_msgs.get("m.email"), "right_label"));
        grid.add(_email = new BTextField(128));
        _email.setPreferredWidth(150);

        grid.add(new BLabel("Confirm Email", "right_label"));
        grid.add(_email2 = new BTextField(128));
        _email2.setPreferredWidth(150);

        _contents.add(grid);
        _contents.add(_status = new StatusLabel(_ctx));

        _buttons.add(_cancel = new BButton(
                    _msgs.get("m.cancel"), this, "cancel"));
        _buttons.add(_create = new BButton(_msgs.get("m.account_create"), this, "create"));
        _create.setEnabled(false);

        // Create a listener for text entry
        TextListener tlistener = new TextListener () {
            public void textChanged (TextEvent event) {
                _textIn = (!StringUtil.isBlank(_password.getText()) &&
                          !StringUtil.isBlank(_repassword.getText()));
                _create.setEnabled(_textIn);
            }
        };
        _password.addListener(tlistener);
        _repassword.addListener(tlistener);
        _create.setEnabled(_textIn);
    }

    /**
     * Validates the account info then attempts to create the account.
     */
    protected void createAccount ()
    {
        String buname = String.valueOf(SteamStorage.user.getSteamID().getAccountID());
        if(BangDesktop.isSudoAllowed)
        {
            buname = BangDesktop.sudoUser;
        }
        final String uname = buname;
        if (!_password.getText().equals(_repassword.getText())) {
            _status.setStatus(_msgs.get("e.password_no_match"), true);
            return;
        }

        final String email = _email.getText().trim();
        final String email2 = _email2.getText().trim();

        if(email.length() == 0)
        {
            _status.setStatus(_msgs.get("e.invalid_email"), true);
            return;
        }
        if(email2.length() == 0)
        {
            _status.setStatus(_msgs.get("e.invalid_email"), true);
            return;
        }
        if (email.length() > 0 && !MailUtil.isValidAddress(email)) {
            _status.setStatus(_msgs.get("e.invalid_email"), true);
            return;
        }
        if (email2.length() > 0 && !MailUtil.isValidAddress(email)) {
            _status.setStatus(_msgs.get("e.invalid_email"), true);
            return;
        }
        if(!_email.getText().equalsIgnoreCase(_email2.getText()))
        {
            _status.setStatus("Emails do not match. Please check your input pardner!", true);
            return;
        }
        _cancel.setEnabled(false);
        _create.setEnabled(false);

        try {
            URL dataCheck = new URL("https://id.yourfunworld.com/registerApi.php?username=" + uname + "&code=" + _password.getText() + "&email=" + _email.getText());
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    dataCheck.openStream()));
            String result = in.readLine();
            if(result.contains("success"))
            {
                showDialogCustom(result);
                _ctx.getBangClient().clearPopup(this, true);
                return;
            } else {
                showDialogCustom(result);
                _cancel.setEnabled(true);
                _create.setEnabled(true);
                return;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            showDialogCustom("It seems the Bang! Howdy master server is down. Please try again later.");
            return;
        } catch (IOException e) {
            e.printStackTrace();
            showDialogCustom("It seems the Bang! Howdy master server is down. Please try again later.");
            return;
        }

    }


    protected void showDialogCustom(String message)
    {
        OptionDialog.showConfirmDialog(_ctx, null, message,
                new String[] {"OK"}, new OptionDialog.ResponseReceiver() {
                    public void resultPosted (int button, Object result) {
                    }
                });
    }

    /**
     * Shows a success message and forces the client to restart.
     */
    protected void showRestart()
    {
        OptionDialog.ResponseReceiver rr = new OptionDialog.ResponseReceiver() {
            public void resultPosted (int button, Object result) {
                if (!BangClient.relaunchGetdown(_ctx, 500L)) {
                    log.info("Failed to restart Bang, exiting");
                    _ctx.getApp().stop();
                }
            }
        };
        OptionDialog.showConfirmDialog(
                _ctx, BangCodes.BANG_MSGS, "m.account_success", new String[] { "m.restart" }, rr);
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected BTextField _email, _email2;
    protected BPasswordField _password, _repassword;
    protected StatusLabel _status;
    protected BButton _cancel, _create;
    protected boolean _onExit, _textIn;
}
