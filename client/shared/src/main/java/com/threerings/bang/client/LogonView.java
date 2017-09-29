package com.threerings.bang.client;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.jme.renderer.*;
import com.jme.renderer.Renderer;
import com.jmex.bui.*;
import com.jmex.bui.event.*;
import com.jmex.bui.icon.*;
import com.jmex.bui.layout.*;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.*;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.*;
import com.threerings.bang.bang.client.BangDesktop;
import com.threerings.bang.client.bui.*;
import com.threerings.bang.data.*;
import com.threerings.bang.steam.*;
import com.threerings.bang.util.*;
import com.threerings.presents.client.*;
import com.threerings.util.*;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

import static com.threerings.bang.Log.*;

/**
 * Displays a simple user interface for logging in.
 */
public class LogonView extends BWindow
        implements ActionListener, BasicClient.InitObserver
{

    private String serverIP;
    private int[] serverPorts = {0,0};

    /**
     * Converts an arbitrary exception into a translatable error string (which should be looked up
     * in the {@link BangAuthCodes#AUTH_MSGS} bundle). If the exception indicates that the client
     * is out of date, the process of updating the client <em>will be started</em>; the client will
     * exit a few seconds later, so be sure to display the returned error message.
     *
     * <p> An additional boolean paramater will be returned indicating whether or not the returned
     * error message is indicative of a connection failure, in which case the caller may wish to
     * direct the user to the server status page so they can find out if we are in the middle of a
     * sceduled downtime.
     */
    public static Tuple<String,Boolean> decodeLogonException (
            BangContext ctx, Exception cause)
    {
        String msg = cause.getMessage();
        boolean connectionFailure = false;

        if (cause instanceof LogonException) {
            // if the failure is due to a need for a client update, check for that and take the
            // appropriate action
            if (BangClient.checkForUpgrade(ctx, msg)) {
                // mogrify the logon failed message to let the client know that we're going to
                // automatically restart
                msg = "m.version_mismatch_auto";
            }

            // change the new account button to server status for certain response codes
            if (msg.equals(BangAuthCodes.UNDER_MAINTENANCE)) {
                connectionFailure = true;
            } else if (msg.startsWith(BangAuthCodes.TEMP_BANNED)) {
                msg = BangAuthCodes.TEMP_BANNED;
            } else if (msg.startsWith(BangAuthCodes.BANNED)) {
                msg = BangAuthCodes.BANNED;
            }

        } else {
            if (cause instanceof ConnectException) {
                msg = "m.failed_to_connect";

            } else if (cause instanceof IOException) {
                String cmsg = cause.getMessage();
                // detect a problem where Windows Connection Sharing will allow a connection to
                // complete and then disconnect it after the first normal packet is sent
                if (cmsg != null && cmsg.indexOf("forcibly closed") != -1) {
                    msg = "m.failed_to_connect";
                } else {
                    msg = "m.network_error";
                }

            } else {
                msg = "m.network_error";
            }
            log.info("Unexpected logon failure", "error", cause);

            // change the new account button to server status
            connectionFailure = true;
        }

        return new Tuple<String,Boolean>(msg, connectionFailure);
    }

    public LogonView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), new AbsoluteLayout());
        setStyleClass("logon_view");

        _ctx = ctx;
        _ctx.getBangClient().queueMusic("theme", true, 0, 25);
        _ctx.getRenderer().setBackgroundColor(ColorRGBA.black);

        _msgs = ctx.getMessageManager().getBundle(BangAuthCodes.AUTH_MSGS);
        String username = BangPrefs.config.getValue("username", "");
        showLoginView();


        // pick a unit from the town they most recently logged into
        UnitConfig[] units = UnitConfig.getTownUnits(BangPrefs.getLastTownId(username),
                EnumSet.of(UnitConfig.Rank.BIGSHOT, UnitConfig.Rank.NORMAL));
        if (units.length > 0) {
            _unitIcon = BangUI.getUnitIcon(RandomUtil.pickRandom(units));
        }

        // add our logon listener
        _ctx.getClient().addClientObserver(_listener);
    }

    protected void showNewUserView (boolean account)
    {
        removeAll();

        BContainer cont = new BContainer(GroupLayout.makeHoriz(
                GroupLayout.STRETCH, GroupLayout.CENTER, GroupLayout.NONE));
        ((GroupLayout)cont.getLayoutManager()).setGap(20);

        String btn1 = "m.continue_player", act1 = "anonymous";
        String btn2 = "m.my_account", act2 = "my_account";
        if (StringUtil.isBlank(BangPrefs.config.getValue("anonymous", ""))) {
            btn1 = "m.new_player";
            btn2 = "m.have_account";
            act2 = "have_account";
        } else if (account) {
            btn1 = "m.have_account";
            act1 = "have_account";
            btn2 = "m.continue_player";
            act2 = "anon_account";
        }
        _anon = new BButton(_msgs.get(btn1), this, act1);
        _anon.setStyleClass("big_button");
        cont.add(_anon);
        _account = new BButton(_msgs.get(btn2), this, act2);
        _account.setStyleClass("big_button");
        cont.add(_account);
        add(cont, new Rectangle(40, 200, 365, 80));
        _anon.setEnabled(_initialized);
        _account.setEnabled(_initialized);
        _action = new BButton("", this, "");
        _action.setStyleClass("logon_new");
        add(_action, new Point(325, 200));

        showStatus();
    }

    protected void showLoginView ()
    {
        removeAll();

        BContainer row = GroupLayout.makeHBox(GroupLayout.LEFT);
        ((GroupLayout)row.getLayoutManager()).setOffAxisJustification(GroupLayout.BOTTOM);
        BContainer grid = new BContainer(new TableLayout(2, 5, 5));
        //grid.add(new BLabel(_msgs.get("m.username"), "logon_label"));
        //grid.add(_username = new BTextField(BangPrefs.config.getValue("username", "")));
        //_username.setPreferredWidth(150);

        showStatus();

        List<String> availableServers = new ArrayList<String>();

        try {
            URL serverList = new URL("https://banghowdy.com/serverList.php?id=" + SteamStorage.user.getSteamID().toString() + "&version=" + DeploymentConfig.getVersion());
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    serverList.openStream()));
            final String result = in.readLine();
            if(!result.contains(":"))
            {
                showDialogCustom(result);
                return;
            }
            if("maintenance".equals(result)) {
                showDialogCustom("The game is currently in maintenance. Please check the Steam announcements and try again later.");
                return;
            }

            grid.add(new BLabel(_msgs.get("m.password"), "logon_label"));
            grid.add(_password = new BPasswordField());
            _password.setPreferredWidth(150);
            _password.addListener(this);
            if(!result.contains("&"))
            {
                String[] cmdSplit = result.split(":");
                availableServers.add(cmdSplit[0]);
            } else {
                String[] arrayData = result.split("&");
                for(String serverdata : arrayData)
                {
                    String[] cmdSplit = serverdata.split(":");
                    availableServers.add(cmdSplit[0]);
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            showDialogCustom("An error occurred while attempting to get available servers. Please restart the game.");
            return;
        } catch (IOException e) {
            e.printStackTrace();
            showDialogCustom("An error occurred while attempting to get available servers. Please restart the game.");
            return;
        }

        grid.add(new BLabel("Server Selection", "logon_label"));
        grid.add(serverList = new BComboBox(availableServers.toArray()));
        grid.add(registerBtn = new BButton("Create Account", "new_account"));
        registerBtn.addListener(this);
        serverList.addListener(this);

        row.add(grid);

        BContainer col = GroupLayout.makeVBox(GroupLayout.CENTER);
        row.add(col);
        col.add(_logon = new BButton(_msgs.get("m.logon"), this, "logon"));
        _logon.setStyleClass("big_button");
        // use a special sound effect for logon (the ricochet that we also use for window open)
        _logon.setProperty("feedback_sound", BangUI.FeedbackSound.WINDOW_OPEN);
        //col.add(_action = new BButton(_msgs.get("m.new_account"), this, "new_account"));
//        _action.setStyleClass("logon_new");
        add(row, new Rectangle(40, 200, 365, 100));

        // disable the logon button until a password is entered (and until we're initialized)
        _validator = new EnablingValidator(_password, _logon) {
            protected boolean checkEnabled (String text) {
                return super.checkEnabled(text) && _initialized && serverList.getSelectedIndex() != -1;
            }
        };
    }

    protected void showStatus ()
    {
        if (_status == null) {
            _status = new StatusLabel(_ctx);
            _status.setStyleClass("logon_status");
            _status.setPreferredSize(new Dimension(360, 40));
        }
        add(_status, new Rectangle(40, 140, 375, 60));

        BContainer row = GroupLayout.makeHBox(GroupLayout.LEFT);
        row.add(new BButton(_msgs.get("m.options"), this, "options"));
        row.add(new BButton(_msgs.get("m.exit"), this, "exit"));
        add(row, new Rectangle(232, 115, 165, 30));
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if(event.getSource() == serverList)
        {
            if(serverList.getSelectedIndex() == -1)
            {
                _logon.setEnabled(false);
                return;
            } else {
                _logon.setEnabled(true);
            }
            try {
                URL data = new URL("https://banghowdy.com/serverInfo.php?id=" + String.valueOf(SteamStorage.user.getSteamID().getAccountID()) + "&version=" + DeploymentConfig.getVersion() + "&name=" + serverList.getSelectedItem());
                BufferedReader in = new BufferedReader(new InputStreamReader(data.openStream()));
                final String result = in.readLine();
                if(result.contains("&") && result.contains(","))
                {
                    String[] info = result.split("&");
                    serverIP = info[0];
                    String[] portStr = info[1].split(",");
                    serverPorts = new int[portStr.length];
                    for (int i = 0, len = portStr.length; i < len; ) {
                        serverPorts[i] = Integer.parseInt(portStr[i++]);
                    }

                } else {
                    showDialog(result);
                    serverList.selectItem(-1); // Un-select any item that was selected.
                    return;
                }
            } catch (IOException | NumberFormatException e) {
                e.printStackTrace();
                showDialog("An error occurred while retrieving that server's info");
                return;
            }
            _validator.invalidate();
            return;
        }
        switch (event.getSource() == _password ? "logon" : event.getAction()) {
            case "logon":
                if (!_initialized) {
                    log.warning("Not finished initializing. Hang tight.");
                    return;
                }

                String username = String.valueOf(SteamStorage.user.getSteamID().getAccountID());
                String password = _password.getText();

                if(password == "" || password == null)
                {
                    log.warning("You didn't enter any password in");
                    return;
                }

                // try to connect to the town lobby server that this player last accessed
                String townId = BangPrefs.getLastTownId(username);
                // but make sure this town has been activated on this client
                if (!BangClient.isTownActive(townId)) {
                    // fall back to frontier town if it has not
                    townId = BangCodes.FRONTIER_TOWN;
                }

                logon(username, password);

                break;
            case "options":
                _ctx.getBangClient().displayPopup(new OptionsView(_ctx, this), true);

                break;
            case "server_status":
                _ctx.showURL(DeploymentConfig.getServerStatusURL());
                _status.setStatus(_msgs.get("m.server_status_launched"), false);

                break;
            case "new_account":
                _ctx.getBangClient().displayPopup(new CreateAccountView(_ctx), true);
                break;
            case "anon_account":
                break;
            case "my_account":
                showNewUserView(true);

                break;
            case "have_account":
                showLoginView();

                break;
            case "anonymous":
                break;
            case "exit":
                _ctx.getApp().stop();
                break;
        }
    }

    public void logon (String username, String password)
    {

        log.info("Set version to: " + DeploymentConfig.getVersion());
        _ctx.getClient().setVersion(String.valueOf(DeploymentConfig.getVersion()));

        try {
            URL data = new URL("https://banghowdy.com/serverInfo.php?id=" + String.valueOf(SteamStorage.user.getSteamID().getAccountID()) + "&version=" + DeploymentConfig.getVersion() + "&name=" + serverList.getSelectedItem());
            BufferedReader in = new BufferedReader(new InputStreamReader(data.openStream()));
            final String result = in.readLine();
            if(result.contains("&") && result.contains(","))
            {
                String[] info = result.split("&"),
                        portStr = info[1].split(",");
                serverIP = info[0];
                serverPorts = new int[portStr.length];
                for (int i = 0, len = portStr.length; i < len; ) {
                    serverPorts[i] = Integer.parseInt(portStr[i++]);
                }

            } else {
                showDialog(result);
                serverList.selectItem(-1); // Un-select any item that was selected.
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            showDialog("An error occurred while retrieving that server's info");
        }

        BangDesktop.server = (String)serverList.getSelectedItem();
        _ctx.getClient().setServer(serverIP, serverPorts);

        // configure the client with the supplied credentials
        _ctx.getClient().setCredentials(
                _ctx.getBangClient().createCredentials(new Name(username), password));

        // now we can log on
        _ctx.getClient().logon();
    }

    // documentation inherited from interface BasicClient.InitObserver
    public void progress (int percent)
    {
        if (percent < 100) {
            _status.setStatus(_msgs.get("m.init_progress", ""+percent), false);
        } else {
            _status.setStatus(_msgs.get("m.init_complete"), false);
            _initialized = true;
        }
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // focus the appropriate textfield
        if (_username != null) {
            if (StringUtil.isBlank(_username.getText())) {
                _username.requestFocus();
            } else {
                _password.requestFocus();
            }
        }

        if (_unitIcon != null) {
            _unitIcon.wasAdded();
        }
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        if (_unitIcon != null) {
            _unitIcon.wasRemoved();
        }
    }

    @Override // documentation inherited
    protected void renderBackground (Renderer renderer)
    {
        super.renderBackground(renderer);

        if (_unitIcon != null) {
            _unitIcon.render(renderer, 50, 380, _alpha);
        }
    }

    protected void switchToServerStatus ()
    {
        if (_action != null) {
            _action.setText(_msgs.get("m.server_status"));
            _action.setAction("server_status");
        }
    }

    protected void showTempBanDialog (String reason, long time)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d yyyy, hh:mm aaa z");
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
        cal.setTimeInMillis(time);
        String expires = sdf.format(cal.getTime());
        OptionDialog.showConfirmDialog(_ctx, BangAuthCodes.AUTH_MSGS, "m.tb_title",
                MessageBundle.tcompose("m.tb_info", reason, expires),
                new String[] { "m.exit", "m.tb_tos" }, new OptionDialog.ResponseReceiver() {
                    public void resultPosted (int button, Object result) {
                        if (button == 1) {
                            _ctx.showURL(DeploymentConfig.getTosURL());
                        }
                        _ctx.getApp().stop();
                    }
                });
    }

    protected void showDialog(String message)
    {
        OptionDialog.showConfirmDialog(_ctx, BangAuthCodes.AUTH_MSGS, message,
                new String[] {"m.ok"}, new OptionDialog.ResponseReceiver() {
                    public void resultPosted (int button, Object result) {
                    }
                });
    }

    protected void showDialogCustom(String message)
    {
        OptionDialog.showConfirmDialog(_ctx, null, message,
                new String[] {"OK"}, new OptionDialog.ResponseReceiver() {
                    public void resultPosted (int button, Object result) {
                    }
                });
    }


    protected void showBannedDialog (String reason)
    {
        OptionDialog.showConfirmDialog(_ctx, BangAuthCodes.AUTH_MSGS, "m.ban_title",
                MessageBundle.tcompose("m.ban_info", reason),
                new String[] { "m.exit" }, new OptionDialog.ResponseReceiver() {
                    public void resultPosted (int button, Object result) {
                        _ctx.getApp().stop();
                    }
                });
    }

    protected ClientAdapter _listener = new ClientAdapter() {
        public void clientDidLogon (Client client) {
            _ctx.getBangClient().fadeOutMusic(0); // Kill the music
            _status.setStatus(_msgs.get("m.logged_on"), false);
            PlayerObject user = (PlayerObject)client.getClientObject();
            BangPrefs.config.setValue(user.tokens.isAnonymous() ? "anonymous" : "username",
                    user.username.toString());
        }

        public void clientFailedToLogon (Client client, Exception cause) {
            Tuple<String,Boolean> msg = decodeLogonException(_ctx, cause);
            if (msg.right) {
                switchToServerStatus();
            }

            _status.setStatus(_msgs.xlate(msg.left), true);

            // if we got a NO_TICKET message, reset our last town id just to be sure
            String cmsg = cause.getMessage();
            if (cmsg == null) {
                return;
            }

            if (cmsg.indexOf(BangAuthCodes.NO_TICKET) != -1) {
                BangPrefs.setLastTownId(_username.getText(), BangCodes.FRONTIER_TOWN);

                // if we got a no such user message and we're anonymous, clear it out
            } else if (cmsg.indexOf(BangAuthCodes.NO_SUCH_USER) != -1 &&
                    StringUtil.isBlank(BangPrefs.config.getValue("username", ""))) {
                BangPrefs.config.setValue("anonymous", "");
                showNewUserView(false);

                // if we got a no anonymous user message, change to user login mode
            } else if (cmsg.indexOf(BangAuthCodes.NO_ANONYMOUS_ACCESS) != -1) {
                showLoginView();

                // see if we are temp banned
            } else if (cmsg.startsWith(BangAuthCodes.TEMP_BANNED)) {
                String params = cmsg.substring(BangAuthCodes.TEMP_BANNED.length());
                int idx = params.indexOf("|");
                if (idx != -1) {
                    try {
                        long time = Long.parseLong(params.substring(0, idx));
                        String reason = params.substring(idx + 1);
                        showTempBanDialog(reason, time);
                    } catch (NumberFormatException nfe) {
                        log.warning("Unable to read time from temp banned message",
                                "cmsg", cmsg + ".");
                    }
                }

            } else if (cmsg.startsWith(BangAuthCodes.BANNED) &&
                    cmsg.length() > BangAuthCodes.BANNED.length()) {
                String reason = cmsg.substring(BangAuthCodes.BANNED.length());
                showBannedDialog(reason);
            }
        }
    };

    protected BangContext _ctx;
    protected MessageBundle _msgs;

    protected BTextField _username;
    protected BPasswordField _password;
    protected BButton _logon, _action, _account, _anon, registerBtn;
    protected BComboBox serverList;
    protected BIcon _unitIcon;
    protected EnablingValidator _validator;

    protected StatusLabel _status;
    protected boolean _initialized;
    protected URL _shownURL;
}