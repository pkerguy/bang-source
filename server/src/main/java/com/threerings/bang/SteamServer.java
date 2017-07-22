package com.threerings.bang;

import com.codedisaster.steamworks.*;

/**
 * Created by Brandon Fairing on 7/20/2017.
 */
public class SteamServer {

    public static SteamUserCallback userCallback; // Abusing statics is not good
    public static SteamUser user;

    public static void init()
    {
        try {
            if (!SteamAPI.init()) {
                System.out.println("Unable to init Steam");
            } else {
                userCallback = new SteamUserCallback() {
                    @Override
                    public void onValidateAuthTicket(SteamID steamID, SteamAuth.AuthSessionResponse authSessionResponse, SteamID ownerSteamID) {

                    }

                    @Override
                    public void onMicroTxnAuthorization(int appID, long orderID, boolean authorized) {

                    }
                };
                user = new SteamUser(userCallback);
            }
        } catch (SteamException e) {
            e.printStackTrace();
            System.out.println("Steam Exception occurred.");
        }
    }
}
