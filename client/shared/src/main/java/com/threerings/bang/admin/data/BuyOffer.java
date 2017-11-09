package com.threerings.bang.admin.data;

import com.threerings.presents.dobj.DObject;

import java.io.Serializable;

public class BuyOffer extends DObject implements Serializable {

    public int storedoffer = 0;

    public BuyOffer(int offer)
    {
        storedoffer = offer;
    }


}
