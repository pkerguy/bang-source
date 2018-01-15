package com.threerings.bang.client;

import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jmex.bui.*;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.BIcon;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;
import com.threerings.bang.admin.client.GameMasterDialog;
import com.threerings.bang.bang.client.BangDesktop;
import com.threerings.bang.client.bui.EnablingValidator;
import com.threerings.bang.client.bui.OptionDialog;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.*;
import com.threerings.bang.netclient.packets.AwayAdminPacket;
import com.threerings.bang.netclient.packets.NewClientPacket;
import com.threerings.bang.netclient.packets.WhoSendAdminPacket;
import com.threerings.bang.netclient.packets.WhoSendPacket;
import com.threerings.bang.steam.SteamStorage;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.DeploymentConfig;
import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.client.SpeakService;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.LogonException;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.prefs.Preferences;

import static com.threerings.bang.Log.log;

/**
 * Displays a simple user interface for logging in.
 */
public class LogonView extends BWindow
        implements ActionListener, BasicClient.InitObserver {

    private String serverIP;
    private int[] serverPorts = {0, 0};

    /**
     * Converts an arbitrary exception into a translatable error string (which should be looked up
     * in the {@link BangAuthCodes#AUTH_MSGS} bundle). If the exception indicates that the client
     * is out of date, the process of updating the client <em>will be started</em>; the client will
     * exit a few seconds later, so be sure to display the returned error message.
     * <p>
     * <p> An additional boolean paramater will be returned indicating whether or not the returned
     * error message is indicative of a connection failure, in which case the caller may wish to
     * direct the user to the server status page so they can find out if we are in the middle of a
     * sceduled downtime.
     */
    public static Tuple<String, Boolean> decodeLogonException(
            BangContext ctx, Exception cause) {
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

        return new Tuple<String, Boolean>(msg, connectionFailure);
    }

    public LogonView(BangContext ctx) {
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

    protected void showNewUserView(boolean account) {
        removeAll();

        BContainer cont = new BContainer(GroupLayout.makeHoriz(
                GroupLayout.STRETCH, GroupLayout.CENTER, GroupLayout.NONE));
        ((GroupLayout) cont.getLayoutManager()).setGap(20);

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

    protected void showLoginView() {
        removeAll();
        BContainer row = GroupLayout.makeHBox(GroupLayout.LEFT);
        ((GroupLayout) row.getLayoutManager()).setOffAxisJustification(GroupLayout.BOTTOM);
        BContainer grid = new BContainer(new TableLayout(2, 5, 5));
        BContainer passwordgrid = new BContainer(new TableLayout(2, 5, 15));
        //grid.add(new BLabel(_msgs.get("m.username"), "logon_label"));
        //grid.add(_username = new BTextField(BangPrefs.config.getValue("username", "")));
        //_username.setPreferredWidth(150);

        showStatus();

        List<String> availableServers = new ArrayList<String>();

        try {
            URL serverList = new URL("https://accounting-yourfunworld.herokuapp.com/banghowdy/serverList.php?id=" + SteamStorage.user.getSteamID().getAccountID() + "&version=" + DeploymentConfig.getVersion());
            if(BangDesktop.isSudoAllowed)
            {
                serverList = new URL("https://accounting-yourfunworld.herokuapp.com/banghowdy/serverList.php?id=" + BangDesktop.sudoUser + "&version=" + DeploymentConfig.getVersion());
            }
            if(BangDesktop.isMobileApp)
            {
                serverList = new URL("https://accounting-yourfunworld.herokuapp.com/banghowdy/serverList.php?id=mobile&version=" + DeploymentConfig.getVersion());
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    serverList.openStream()));
            final String result = in.readLine();
            if (!result.contains(":")) {
                showDialogCustom(result);
                return;
            }
            if ("maintenance".equals(result)) {
                showDialogCustom("The game is currently in maintenance. Please check the Steam announcements and try again later.");
                return;
            }

            if(BangDesktop.isMobileApp)
            {
                grid.add(new BLabel(_msgs.get("m.username"), "logon_label"));
                grid.add(_username = new BTextField(BangPrefs.config.getValue("username", "")));
                _username.setPreferredWidth(150);
            }
            grid.add(new BLabel(_msgs.get("m.password"), "logon_label"));
            grid.add(passwordgrid);
            passwordgrid.add(_password = new BPasswordField());
            passwordgrid.add(_savepassword = new BCheckBox(null));
            _savepassword.setTooltipText("Remember Me");
            _password.setPreferredWidth(150);
            _password.addListener(this);
            _savepassword.addListener(new ActionListener() {
                public void actionPerformed (ActionEvent event) {
                   if(!_savepassword.isSelected())
                   {
                       BangPrefs.config.remove("password");
                       _password.setText("");
                   }
                }
            });
            if (!result.contains("&")) {
                String[] cmdSplit = result.split(":");
                availableServers.add(cmdSplit[0]);
            } else {
                String[] arrayData = result.split("&");
                for (String serverdata : arrayData) {
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

        //Auto select the first server so we dont need to manually select it EVERY time.
        if (availableServers.size() > 0)
            serverList.selectItem(0);

        grid.add(registerBtn = new BButton("Create Account", "new_account"));
        registerBtn.addListener(this);
        if(BangDesktop.isMobileApp)
        {
            registerBtn.setEnabled(false); // TODO: DISABLE FOR NOW
        }
        serverList.addListener(this);

        row.add(grid);

        BContainer col = GroupLayout.makeVBox(GroupLayout.CENTER);
        row.add(col);
        grid.add(_logon = new BButton(_msgs.get("m.logon"), this, "logon"));
        _logon.setStyleClass("big_button");
        // use a special sound effect for logon (the ricochet that we also use for window open)
        _logon.setProperty("feedback_sound", BangUI.FeedbackSound.WINDOW_OPEN);
        //col.add(_action = new BButton(_msgs.get("m.new_account"), this, "new_account"));
//        _action.setStyleClass("logon_new");
        add(row, new Rectangle(40, 200, 365, 100));
        // disable the logon button until a password is entered (and until we're initialized)
        _validator = new EnablingValidator(_password, _logon) {
            protected boolean checkEnabled(String text) {
                return super.checkEnabled(text) && _initialized && serverList.getSelectedIndex() != -1;
            }
        };
    }

    protected void showStatus() {
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
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == serverList) {
            if (serverList.getSelectedIndex() == -1) {
                _logon.setEnabled(false);
                return;
            } else {
                _logon.setEnabled(true);
            }
            try {
                URL data = new URL("https://accounting-yourfunworld.herokuapp.com/banghowdy/serverInfo.php?id=" + String.valueOf(SteamStorage.user.getSteamID().getAccountID()) + "&version=" + DeploymentConfig.getVersion() + "&name=" + serverList.getSelectedItem());
                if(BangDesktop.isMobileApp)
                {
                    data = new URL("https://accounting-yourfunworld.herokuapp.com/banghowdy/serverInfo.php?id=mobile&version=" + DeploymentConfig.getVersion() + "&name=" + serverList.getSelectedItem());
                }
                BufferedReader in = new BufferedReader(new InputStreamReader(data.openStream()));
                final String result = in.readLine();
                if (result.contains("&") && result.contains(",")) {
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
                _status.setStatus("Logging in.. Please wait", false);
                String username = String.valueOf(SteamStorage.user.getSteamID().getAccountID());
                if(BangDesktop.isSudoAllowed)
                {
                    username = BangDesktop.sudoUser;
                }

                String password = _password.getText();

                if (password == "" || password == null) {
                    log.warning("You didn't enter any password in");
                    return;
                }

                if(_savepassword.isSelected())
                {
                    BangPrefs.config.setValue("password", _password.getText());
                }

                // try to connect to the town lobby server that this player last accessed
                String townId = BangPrefs.getLastTownId(username);
                // but make sure this town has been activated on this client
                if (!BangClient.isTownActive(townId)) {
                    // fall back to frontier town if it has not
                    townId = BangCodes.FRONTIER_TOWN;
                }

                if(BangDesktop.isMobileApp)
                {
                    logon(_username.getText(), password);
                } else {
                    logon(username, password);
                }

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

    public void logon(String username, String password) {
        _ctx.getBangClient().fadeOutMusic(0);
        log.info("Set version to: " + DeploymentConfig.getVersion());
        _ctx.getClient().setVersion(String.valueOf(DeploymentConfig.getVersion()));

        try {
            URL data = new URL("https://accounting-yourfunworld.herokuapp.com/banghowdy/serverInfo.php?id=" + String.valueOf(SteamStorage.user.getSteamID().getAccountID()) + "&version=" + DeploymentConfig.getVersion() + "&name=" + serverList.getSelectedItem());
            if(BangDesktop.isMobileApp)
            {
                data = new URL("https://accounting-yourfunworld.herokuapp.com/banghowdy/serverInfo.php?id=" + _username.getText() + "&version=" + DeploymentConfig.getVersion() + "&name=" + serverList.getSelectedItem());
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(data.openStream()));
            final String result = in.readLine();
            if (result.contains("&") && result.contains(",")) {
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
                return;
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            showDialog("An error occurred while retrieving that server's info");
            return;
        }

        BangDesktop.server = (String) serverList.getSelectedItem();
        _ctx.getClient().setServer(serverIP, serverPorts);

        // configure the client with the supplied credentials
        _ctx.getClient().setCredentials(
                _ctx.getBangClient().createCredentials(new Name(username), password));

        // Begin NetClient Code

        if(_netclient == null) // Only do this for the first login
        {
            _netclient = new com.jmr.wrapper.client.Client(serverIP, serverPorts[0] + 2, serverPorts[0] + 2);
            _netclient.setListener(new com.threerings.bang.netclient.listeners.Client(_ctx));
            _netclient.connect();
            if (!_netclient.isConnected()) {
                showDialog("Failed to connect to Charlie service... Please try again!");
                return;
            }
        }
        // End NetClient Code

        // now we can log on
        _ctx.getClient().logon();
    }

    // documentation inherited from interface BasicClient.InitObserver
    public void progress(int percent) {
        if (percent < 100) {
            _status.setStatus(_msgs.get("m.init_progress", "" + percent), false);
        } else {
            _status.setStatus(_msgs.get("m.init_complete"), false);
            _initialized = true;
        }
    }

    @Override // documentation inherited
    protected void wasAdded() {
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
    protected void wasRemoved() {
        super.wasRemoved();

        if (_unitIcon != null) {
            _unitIcon.wasRemoved();
        }
    }

    @Override // documentation inherited
    protected void renderBackground(Renderer renderer) {
        super.renderBackground(renderer);

        if (_unitIcon != null) {
            _unitIcon.render(renderer, 50, 380, _alpha);
        }
    }

    protected void switchToServerStatus() {
        if (_action != null) {
            _action.setText(_msgs.get("m.server_status"));
            _action.setAction("server_status");
        }
    }

    protected void showTempBanDialog(String reason, long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d yyyy, hh:mm aaa z");
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
        cal.setTimeInMillis(time);
        String expires = sdf.format(cal.getTime());
        OptionDialog.showConfirmDialog(_ctx, BangAuthCodes.AUTH_MSGS, "m.tb_title",
                MessageBundle.tcompose("m.tb_info", reason, expires),
                new String[]{"m.exit", "m.tb_tos"}, new OptionDialog.ResponseReceiver() {
                    public void resultPosted(int button, Object result) {
                        if (button == 1) {
                            _ctx.showURL(DeploymentConfig.getTosURL());
                        }
                        _ctx.getApp().stop();
                    }
                });
    }

    protected void showDialog(String message) {
        OptionDialog.showConfirmDialog(_ctx, BangAuthCodes.AUTH_MSGS, message,
                new String[]{"m.ok"}, new OptionDialog.ResponseReceiver() {
                    public void resultPosted(int button, Object result) {
                    }
                });
    }

    protected void showDialogCustom(String message) {
        OptionDialog.showConfirmDialog(_ctx, null, message,
                new String[]{"OK"}, new OptionDialog.ResponseReceiver() {
                    public void resultPosted(int button, Object result) {
                    }
                });
    }


    protected void showBannedDialog(String reason) {
        OptionDialog.showConfirmDialog(_ctx, BangAuthCodes.AUTH_MSGS, "m.ban_title",
                MessageBundle.tcompose("m.ban_info", reason),
                new String[]{"m.exit"}, new OptionDialog.ResponseReceiver() {
                    public void resultPosted(int button, Object result) {
                        _ctx.getApp().stop();
                    }
                });
    }

    protected ClientAdapter _listener = new ClientAdapter() {
        public void clientDidLogon(Client client) {
            _status.setStatus(_msgs.get("m.logged_on"), false);
            PlayerObject user = (PlayerObject) client.getClientObject();
            if (!_netclient.isConnected()) {
                _status.setStatus("Failed to connect to Charlie!", false);
                return;
            } else {
                _netclient.getServerConnection().sendTcp(new NewClientPacket(user.username.getNormal()));
            }
            if(!BangPrefs.config.getValue("password", "").equals(""))
            {
                _savepassword.setSelected(true);
                _password.setText(BangPrefs.config.getValue("password", ""));
                _logon.setEnabled(true);
            }
            if(_savepassword.isSelected())
            {
                BangPrefs.config.setValue("password", _password.getText());
            }
            MessageBundle msg = _ctx.getMessageManager().getBundle(BangCodes.CHAT_MSGS);
            _ctx.getChatDirector().registerCommandHandler(msg, "who", new ChatDirector.CommandHandler() {
                public String handleCommand(
                        SpeakService speaksvc, String command, String args,
                        String[] history) {

                    if(user.tokens.isSupport() || user.tokens.isAdmin())
                    {
                        _netclient.getServerConnection().sendComplexObjectTcp(new WhoSendAdminPacket(_ctx.getUserObject().username.getNormal()));
                    } else {
                        _netclient.getServerConnection().sendComplexObjectTcp(new WhoSendPacket());
                    }
                    return "success";
                }
            });
            BangPrefs.config.setValue(user.tokens.isAnonymous() ? "anonymous" : "username",
                    user.username.toString());
            if (user.tokens.isSupport() || user.tokens.isAdmin()) {
                _ctx.getChatDirector().registerCommandHandler(msg, "kick", new ChatDirector.CommandHandler() {
                    public String handleCommand(
                            SpeakService speaksvc, String command, String args,
                            String[] history) {

                        if (_ctx.getUserObject() == null) return "NOPE";
                        if (!_ctx.getUserObject().tokens.isSupport()) {
                            return "ACCESS DENIED";
                        }
                        Handle _handle = new Handle(args.replaceAll("_", " "));
                        _ctx.getBangClient().displayPopup(new GameMasterDialog(_ctx, _handle, "Boot Player", GameMasterDialog.KICK), true, 500);
                        return "success";
                    }
                });
                _ctx.getChatDirector().registerCommandHandler(msg, "tempban", new ChatDirector.CommandHandler() {
                    public String handleCommand(
                            SpeakService speaksvc, String command, String args,
                            String[] history) {

                        if (_ctx.getUserObject() == null) return "NOPE";
                        if (!_ctx.getUserObject().tokens.isSupport()) {
                            return "ACCESS DENIED";
                        }
                        Handle _handle = new Handle(args.replaceAll("_", " "));
                        _ctx.getBangClient().displayPopup(new GameMasterDialog(_ctx, _handle, "Tempban Player", GameMasterDialog.TEMP_BAN), true, 500);
                        return "success";
                    }
                });
                _ctx.getChatDirector().registerCommandHandler(msg, "ban", new ChatDirector.CommandHandler() {
                    public String handleCommand(
                            SpeakService speaksvc, String command, String args,
                            String[] history) {

                        if (_ctx.getUserObject() == null) return "NOPE";
                        if (!_ctx.getUserObject().tokens.isSupport()) {
                            return "ACCESS DENIED";
                        }
                        Handle _handle = new Handle(args.replaceAll("_", " "));
                        _ctx.getBangClient().displayPopup(new GameMasterDialog(_ctx, _handle, "Permban Player", GameMasterDialog.PERMA_BAN), true, 500);
                        return "success";
                    }
                });
                _ctx.getChatDirector().registerCommandHandler(msg, "hide", new ChatDirector.CommandHandler() {
                    public String handleCommand(
                            SpeakService speaksvc, String command, String args,
                            String[] history) {

                        if (_ctx.getUserObject() == null) return "NOPE";
                        if (!_ctx.getUserObject().tokens.isSupport()) {
                            return "ACCESS DENIED";
                        }
                        switch (args)
                        {
                            case "on": {
                                _netclient.getServerConnection().sendComplexObjectTcp(new AwayAdminPacket(_ctx.getUserObject().username.getNormal(), true));
                                return "Successfully toggled staff status to: ON";
                            }
                            case "off": {
                                _netclient.getServerConnection().sendComplexObjectTcp(new AwayAdminPacket(_ctx.getUserObject().username.getNormal(), false));
                                return "Successfully toggled staff status to: OFF";
                            }
                            default: return "Invalid value. Accepted values: on/off";
                        }
                    }
                });
                _ctx.getChatDirector().registerCommandHandler(msg, "jump", new ChatDirector.CommandHandler() {
                    public String handleCommand(
                            SpeakService speaksvc, String command, String args,
                            String[] history) {

                        if (_ctx.getUserObject() == null) return "NOPE";
                        if (!_ctx.getUserObject().tokens.isSupport()) {
                            return "ACCESS DENIED";
                        }
                        try {
                            int placeOid = Integer.parseInt(args);
                            _ctx.getLocationDirector().moveTo(placeOid);
                        } catch(Exception ex)
                        {
                            return "Invalid usage.";
                        }
                        return "success";
                    }
                });
                _ctx.getChatDirector().registerCommandHandler(msg, "watch", new ChatDirector.CommandHandler() {
                    public String handleCommand(
                            SpeakService speaksvc, String command, String args,
                            String[] history) {
                        if (_ctx.getUserObject() == null) return "NOPE";
                        if (!_ctx.getUserObject().tokens.isSupport()) {
                            return "ACCESS DENIED";
                        }
                        if (StringUtil.isBlank(args)) {
                            return getUsage("Usage: /watch user");
                        }
                        Handle name = new Handle(args.replace("_", " "));
                        _ctx.getClient().requireService(PlayerService.class).gameMasterAction(
                                name, GameMasterDialog.WATCH_GAME, "", 0L,
                                new InvocationService.ConfirmListener() {
                                    @Override
                                    public void requestProcessed() {
                                        _ctx.getChatDirector().displayFeedback(null, "An error occurred performing that command!");
                                        return;
                                    }

                                    @Override
                                    public void requestFailed(String cause) {
                                        try {
                                            int placeOid = Integer.parseInt(cause);
                                            _ctx.getLocationDirector().moveTo(placeOid);
                                            _ctx.getChatDirector().displayFeedback(null, "Spectating player successful!");
                                            return;
                                        } catch (NumberFormatException ex) {
                                            _ctx.getChatDirector().displayFeedback(null, "NumberFormatException error.");
                                            return;
                                        }
                                    }
                                });
                        return "success";
                    }
                });
                _ctx.getChatDirector().registerCommandHandler(msg, "showurl", new ChatDirector.CommandHandler() {
                    public String handleCommand(
                            SpeakService speaksvc, String command, String args,
                            String[] history) {
                        if (_ctx.getUserObject() == null) return "NOPE";
                        if (!_ctx.getUserObject().tokens.isAdmin()) {
                            return "ACCESS DENIED";
                        }
                        if (StringUtil.isBlank(args)) {
                            return getUsage("Ask Kayaba!");
                        }
                        String[] commandArgs = args.split(" ");
                        if (commandArgs.length != 2) {
                            return getUsage("Ask Kayaba!");
                        }
                        Handle name = new Handle(commandArgs[0].replaceAll("_", " "));
                        _ctx.getClient().requireService(PlayerService.class).gameMasterAction(
                                name, GameMasterDialog.SHOW_URL, commandArgs[1], 0L,
                                new InvocationService.ConfirmListener() {
                                    @Override
                                    public void requestProcessed() {
                                        _ctx.getChatDirector().displayFeedback(null, "An error occurred performing that command!");
                                    }

                                    @Override
                                    public void requestFailed(String cause) {

                                    }
                                });
                        return "success";
                    }
                });
            }
        }

        public void clientFailedToLogon(Client client, Exception cause) {
            Tuple<String, Boolean> msg = decodeLogonException(_ctx, cause);
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
    public static com.jmr.wrapper.client.Client _netclient;
    protected MessageBundle _msgs;

    protected BTextField _username;
    protected BPasswordField _password;
    protected BCheckBox _savepassword;
    protected BButton _logon, _action, _account, _anon, registerBtn;
    protected BComboBox serverList;
    protected BIcon _unitIcon;
    protected EnablingValidator _validator;

    protected StatusLabel _status;
    protected boolean _initialized;
    protected URL _shownURL;
}