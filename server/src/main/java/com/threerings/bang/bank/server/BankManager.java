package com.threerings.bang.bank.server;

import com.threerings.bang.server.ShopManager;

public class BankManager extends ShopManager implements BankProvider{
    @Override
    protected String getIdent() {
        return "bank";
    }


}
