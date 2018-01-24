package com.astpos.ASTPinpad;

import java.util.Map;

/**
 * Created by astpos on 1/10/18.
 */

abstract public class PinpadTransaction {

    abstract public void setToSale(String amount);

    abstract public void setToSaleCredit(String amount);

    abstract public void setToSaleDebit(String amount);

    abstract public void setToPreAuth(String amount);

    abstract public void setDebit();

    abstract public void setCredit();

    abstract public void setReboot();


    abstract public String getPostData();

    abstract public String getUrlString();

    abstract public Map<String, String> getResponseMap();


    abstract public void processResponse(String responseString);

    abstract public void printMap(Map<String, String> parsMap);


    abstract public boolean isSuccessful();

    abstract public boolean isCanceled();

    abstract public boolean isChip();

    // enclosing all pinpad classes
}
