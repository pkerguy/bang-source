package com.threerings.bang.steam;

import com.codedisaster.steamworks.*;
import com.threerings.bang.bang.client.BangDesktop;

import javax.swing.*;

/**
 * Created by Brandon Fairing on 7/20/2017.
 */
public class SteamStorage {

    public static SteamUserCallback userCallback; // Abusing statics is not good
    public static SteamUser user;

    public static void init()
    {
        try {
            if (!SteamAPI.init()) {
                JOptionPane.showMessageDialog(null,
                        "Please make sure to be running Steam before trying to run Bang! Howdy.",
                        "Steam is not running",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            } else {
                userCallback = new SteamUserCallback() {
                    @Override
                    public void onValidateAuthTicket(SteamID steamID, SteamAuth.AuthSessionResponse authSessionResponse, SteamID ownerSteamID) {
                        switch(authSessionResponse)
                        {
                            case UserNotConnectedToSteam:
                                JOptionPane.showMessageDialog(null,
                                        "You are not connected to Steam.",
                                        "Steam Connection Offline",
                                        JOptionPane.ERROR_MESSAGE);
                                System.exit(1); // This can be called if they fail somewhere else in the game past the initial user call
                                return; // In-case the previous didn't work, DO NOT CONTINUE.
                            case VACBanned:
                                JOptionPane.showMessageDialog(null,
                                        "You have been VAC banned and therefore cannot play Bang! Howdy",
                                        "VAC Banned",
                                        JOptionPane.ERROR_MESSAGE);
                                System.exit(1); // This can be called if they fail somewhere else in the game past the initial user call
                                return; // In-case the previous didn't work, DO NOT CONTINUE.
                            case VACCheckTimedOut:
                                JOptionPane.showMessageDialog(null,
                                        "The VAC check timed out and therefore you have been disconnected.",
                                        "VAC Banned",
                                        JOptionPane.ERROR_MESSAGE);
                                System.exit(1); // This can be called if they fail somewhere else in the game past the initial user call
                                return; // In-case the previous didn't work, DO NOT CONTINUE.
                            case PublisherIssuedBan: // This will only be issued to tainted PCs if it gets to the point we need to do that, hopefully we stay away from this
                                JOptionPane.showMessageDialog(null,
                                        "You have been permanently blacklisted from our games. You may not appeal this ban.",
                                        "Blacklisted",
                                        JOptionPane.ERROR_MESSAGE);
                                System.exit(1); // This can be called if they fail somewhere else in the game past the initial user call
                                return; // In-case the previous didn't work, DO NOT CONTINUE.
                            case NoLicenseOrExpired: // Won't be used because it's probally going to be F2P, but let's add it in case
                                JOptionPane.showMessageDialog(null,
                                        "You do not own Bang! Howdy. Please consider buying it.",
                                        "Expired/Pirated Copy",
                                        JOptionPane.ERROR_MESSAGE);
                                System.exit(1); // This can be called if they fail somewhere else in the game past the initial user call
                                return; // In-case the previous didn't work, DO NOT CONTINUE.
                            case LoggedInElseWhere:
                                JOptionPane.showMessageDialog(null,
                                        "You logged in from another location",
                                        "Location Error",
                                        JOptionPane.ERROR_MESSAGE);
                                System.exit(1); // This can be called if they fail somewhere else in the game past the initial user call
                                return; // In-case the previous didn't work, DO NOT CONTINUE.
                            case AuthTicketInvalidAlreadyUsed:
                                JOptionPane.showMessageDialog(null,
                                        "Your authentication session is already in use.",
                                        "Session in Use",
                                        JOptionPane.ERROR_MESSAGE);
                                System.exit(1); // This can be called if they fail somewhere else in the game past the initial user call
                                return; // In-case the previous didn't work, DO NOT CONTINUE.
                            case AuthTicketInvalid:
                                JOptionPane.showMessageDialog(null,
                                        "Your authentication session is invalid. Please try relaunching the game",
                                        "Authentication Error",
                                        JOptionPane.ERROR_MESSAGE);
                                System.exit(1); // This can be called if they fail somewhere else in the game past the initial user call
                                return; // In-case the previous didn't work, DO NOT CONTINUE.
                            case AuthTicketCanceled:
                                JOptionPane.showMessageDialog(null,
                                        "Your authentication session was cancelled. Please try relaunching the game",
                                        "Authentication Error",
                                        JOptionPane.ERROR_MESSAGE);
                                System.exit(1); // This can be called if they fail somewhere else in the game past the initial user call
                                return; // In-case the previous didn't work, DO NOT CONTINUE.
                        }
                    }

                    @Override
                    public void onMicroTxnAuthorization(int appID, long orderID, boolean authorized) {

                    }
                };
                user = new SteamUser(userCallback);
            }
        } catch (SteamException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "An exception occurred while trying to attach to Steam.",
                    "Steam Exception",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }
}
