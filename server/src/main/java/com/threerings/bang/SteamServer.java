package com.threerings.bang;

import com.codedisaster.steamworks.*;

/**
 * Created by Brandon Fairing on 7/20/2017.
 */
public class SteamServer {

    public static SteamUserCallback userCallback; // Abusing statics is not good

    public static void init()
    {
        try {
            if (!SteamGameServerAPI.init("/home/steam/linux64",(127 << 24) + 1, (short) 27015, (short) 27016, (short) 27017,
                    SteamGameServerAPI.ServerMode.NoAuthentication, "0.0.1")) {
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
            }
        } catch (SteamException e) {
            e.printStackTrace();
            System.out.println("Steam Exception occurred.");
        }
    }
}
