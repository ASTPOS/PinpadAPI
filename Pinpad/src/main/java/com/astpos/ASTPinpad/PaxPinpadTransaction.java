package com.astpos.ASTPinpad;

import android.util.Base64;
import android.util.Log;

import com.astpos.ASTPinpad.util.Constants;

import java.io.Console;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


/**
 * Created by Iskren Iliev on 11/15/17.
 */

public class PaxPinpadTransaction extends PinpadTransaction{

    /**
     * TAG for printing logs
     */
    private final static String TAG = "ASTPOS";

    // variables for Pax string build
    private final String USER_AGENT = "Mozilla/5.0";
    private final static Console console = System.console();
    private final static Scanner in = new Scanner(System.in);
    private static Boolean useScanner = false;


    private char STX = (char)Integer.parseInt("0002", 16);
    private char FS = (char)Integer.parseInt("001C", 16);
    private char ETX = (char)Integer.parseInt("0003", 16);
    private char US = (char)Integer.parseInt("001F", 16);

    private String ver = "1.28";
    private String initialize = "A00";
    private String doCredit = "T00"; // Do Credit
    //private String doCredit = "T02"; // Do Debit
    private String transNo = "";
    private String authCode = "";
    private String transType = "";
    private String amount = "";
    private String traceInfo = "";
    private String addInfo = "";
    private String hostRef = "";

    // vars for PAX ///

    private String strToEncode = "";
    private String pinpad_ip = "";

    // Capture the Date and time - build time stamp YYYYMMDDHHMMSS
//    private String timeStamp = new SimpleDateFormat("YYYYMMDDhhmmss").format(Calendar.getInstance().getTime());
//    private String timeStampReceipt = new SimpleDateFormat("MM/d/yy  hh:mm aaa").format(Calendar.getInstance().getTime());
    // System.out.println("Time Stamp " + timeStamp);
    private Map<String, String> responsePars = new HashMap<String, String>();
    private Map<String, String> responseExtPars = new HashMap<String, String>();
    private static Boolean exitLoop= false;


    /**
     * constructor
     * @param ipAddress
     */
    PaxPinpadTransaction(String ipAddress) {
        // String url = "http://192.168.1.205:10009?AkEwMBwxLjI4A0s=";
        // String url = "http://192.168.1.189:10009?" + encodedStr;
//        this.pinpad_ip = "http://192.168.1.213:10009?";
//        Log.d(TAG, "Pinpad IP: " + this.pinpad_ip);
        String ipString = "http://".concat(ipAddress).concat(":10009?");
        Log.d(TAG, "Pinpad IP: " + ipString);
        this.pinpad_ip = ipString;
    }


    /**
     * Methods to handle when user chooses:
     * Debit/Credit/Reboot based on CheckBox
     */

    public void setDebit(){
//        PaxPinpadActivity.showToast("Processing Debit...");
        this.doCredit= "T02";
        this.strToEncode= doCredit+FS+ver+FS+transType+FS+amount+FS+FS+traceInfo+FS+FS+addInfo+ETX;
    }

    public void setCredit(){
//        PaxPinpadActivity.showToast("Processing Credit...");
        this.doCredit= "T00";
        this.strToEncode= doCredit+FS+ver+FS+transType+FS+amount+FS+FS+traceInfo+FS+ FS+FS+FS+ FS+addInfo+ETX;
//        Log.d(TAG, "STRTOENCODE: "+strToEncode);
    }

    public void setReboot(){
//        PaxPinpadActivity.showToast("Rebooting...");
        this.doCredit = "A26";
        this.strToEncode= doCredit+FS+ver+ETX;
    }

    public void setToInit(){
        //to do initialize pinpad
        this.strToEncode = initialize + FS + ver + ETX;
    }


    public void setToSale(String saleAmount) {
        this.transType = "01";
        this.amount = saleAmount;
        this.traceInfo = "1";
        this.addInfo = ""; //"TOKENREQUEST=1"+US;
    }

    @Override
    public void setToSaleCredit(String amount) {
        //not needed for PAX
    }

    @Override
    public void setToSaleDebit(String amount) {
        //not needed for PAX
    }

    public void setToPreAuth(String saleAmount) {
        this.transType = "03";
        this.amount = saleAmount;
        this.traceInfo = "1";
        this.addInfo = "";
    }

    @Override
    public String getPostData() {
        return null;
    }

    public void setToPostAuth() {
        //TODO
    }

    public void setToVoid() {
        //TODO
    }

    public void setToIndepReturn() {
        //TODO
    }

    public void setToAdjustTip(String tipAmount, String transactionId) {
        amount = tipAmount;
        hostRef = "";
        transNo = transactionId;

        transType = "06"; // Adjust for existing trans
        traceInfo = "1"+US+US+US+transNo+US;
        addInfo = "HREF="+hostRef+US;
    }

    public void  setToAdjustByRef(String newTotal, String HREF) {
        amount = newTotal;
        hostRef = HREF;
        transNo = "";

        transType = "06"; // Adjust for existing trans
        traceInfo = "1"+US+US+US+transNo+US;
        addInfo = "HREF="+hostRef+US;
    }

    public void setToBatchClose() {
        //TODO
    }

    public void setToDoSignature() {
        transType = "05"; //Allows signature directly on the POS
    }



    public String getUrlString() {
        // Calculate LRC and build final string to encode
        char LRC = (char)calculateLRC(strToEncode.getBytes());
        System.out.println("LRC:  "  + LRC);
        String fullStrToEncode = STX + strToEncode + LRC;
        // System.out.println(strToEncode);

        String completedUrlString = "";
        try {
            byte[] data = fullStrToEncode.getBytes("UTF-8");
            String encodedStr = Base64.encodeToString(data, 8);
            completedUrlString = pinpad_ip + encodedStr;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return completedUrlString;
    }



   /*  *************************************************
   Calculating LRC using provided pseudocode by PAX:
   Set LRC = 0
   For each character c in the string
   Do
   Set LRC = LRC XOR c
   End Do
   ***************************************************** */
    public static byte calculateLRC(byte[] bytes) {
        int LRC = 0;
        for (int i = 0; i < bytes.length; i++) {
            LRC ^= bytes[i];
        }
        return (byte)LRC;
    }



    public void printMap( Map<String, String> parsMap ) {
        // System.out.println("\nlines len: " + parsMap.size());
        for ( Map.Entry<String, String> entry : parsMap.entrySet() ) {
            String key = entry.getKey();
            Object value = entry.getValue();
//            System.out.println(key+": "+value);
            Log.d(TAG, key+": "+value);

        }
    } // print


    /* *********************************************************
    It process the DoCredit response based ont he following break down:

        String urlText = "<DETAIL><TRAN_TYPE>CCR1</TRAN_TYPE><AMOUNT>1.00</AMOUNT></DETAIL>";
        http://192.168.2.100:10009?
        [02]T00 <- This is Command Type
        [1c]1.28 <- This is the protocol version
        [1c]01 <- Transaction Type (Transaction Type definition Section 4.4 on the guide)
        [1c]100 <- Amount Information (Request Amount Info is on page 163)
        [1c] <- Account Information
        [1c]1 <- Trace Information (ECR Reference Number or transaction number.  See page 166)
        [1c] <- AVS Information
        [1c] <- Cashier Information
        [1c] <- Commercial Information
        [1c] <- MoTo/EComm Information
        [1c] <- Additional Information
        [03]C <- close command.

    ************************************************************ */
    public void processResponse(String inputString) {
        if(inputString == null){
            return;
        }

        // separate lines
        // Map<String, String> responsePars = new HashMap<String, String>();  // <--  created a public map so we can use it for the receipt print
        String[] lines = inputString.split("["+STX+FS+ETX+"]"); // temp store lines
        String str = ""; //temp string
        //trim and print the response
        String[] responseLabels = {"blank", "status", "command", "version", "responseCode", "responseMessage",
                "hostInformation", "transactionType", "amountInformation", "accountInformation",
                "traceInformation", "avsInfo", "commercialInfo", "moto", "additionalInfo", "lrc"};
        // load all the param
        for (int i=0; i < lines.length; i++) {
//            Log.d(TAG, "Line: " + lines[i]);
            Log.d(TAG, "Key: " + responseLabels[i] + "  Val: " +lines[i]);
            if (i >= responseLabels.length) {
                responsePars.put("ExtraInfo", lines[i]);
            } else responsePars.put(responseLabels[i], lines[i]);
        }
        // Prints clustered lines of sub vars as well for Testing
        // printMap(responsePars); // print all the param

        if (!"000000".equals( responsePars.get("responseCode") )) {
            Log.i(TAG, "Bad Response code: " + responsePars.get("responseCode"));
            responseExtPars = responsePars;
            return; //process error message
        }

        //trim and print the Host Information
        str = responsePars.get("hostInformation"); //.toString();
        if (!str.isEmpty() && str!=null) {
            String[] hostRespLabels = {"hostResponseCode", "hostResponseMesg", "authCode",
                    "hostReferenceNumber", "traceNumber", "batchNumber"};
            lines = str.split("["+US+STX+FS+ETX+"]"); // or use [<>]
            // load host param
            for (int i=0; i < lines.length; i++) {
                if (i >= hostRespLabels.length) {
                    responseExtPars.put("ExtraInfo", lines[i]);
                } else responseExtPars.put(hostRespLabels[i], lines[i]);
            }
        }

        //trim and print the Acc Information
        str = responsePars.get("accountInformation"); //.toString();
        if (str!=null && !str.isEmpty()) {
            String[] accInfoLabels = {"account", "entryMode", "EXPD", "EBTtype", "VoucherNumber", "newAccountNo",
                    "cardType", "cardHolder", "CVDApprovalCode", "CVDMessage", "cardPresentIndicator"};
            lines = str.split("["+US+STX+FS+ETX+"]"); // or use [<>]
            // load all the param
            for (int i=0; i < lines.length; i++) {
                if (i >= accInfoLabels.length) {
                    responseExtPars.put("extraInfo", lines[i]);
                } else responseExtPars.put(accInfoLabels[i], lines[i]);
            }
        }

        //trim and print the Amount Information
        str = responsePars.get("amountInformation"); //.toString();
        if (str!=null && !str.isEmpty()) {
            String[] amountInfoLabels = {"approveAmount", "amountDue", "tipAmount", "cashBackAmount",
                    "merchantFee", "taxAmount", "Balance1", "Balance2"};
            lines = str.split("["+US+STX+FS+ETX+"]"); // or use [<>]
            // load all the param
            for (int i=0; i < lines.length; i++) {
                if (i >= amountInfoLabels.length) {
                    responseExtPars.put("extraInfo", lines[i]);
                } else responseExtPars.put(amountInfoLabels[i], lines[i]);
            }
        }

        //trim and print the Trace Information
        str = responsePars.get("traceInformation"); //.toString();
        if (str!=null && !str.isEmpty()) {
            String[] traceInfoLabels = {"transactionNumber", "referenceNumber", "timeStamp" };
            lines = str.split("["+US+STX+FS+ETX+"]"); // or use [<>]
            // load all the param
            for (int i=0; i < lines.length; i++) {
                if (i >= traceInfoLabels.length) {
                    responseExtPars.put("extraInfo", lines[i]);
                } else responseExtPars.put(traceInfoLabels[i], lines[i]);
            }
        }

        //trim and print the Additional Information
        str = responsePars.get("additionalInfo"); //.toString();
        if (str!=null && !str.isEmpty()) {
            // String[] accInfoLabels = {"TABLE", "GUEST", "EXPD", "TICKET", "DISAMT", "CHGAMT", "SIGNSTATUS",
            // 					"FPS", "FPSSIGN", "FPSRECEIPT", "ORIGTIP", "EDCTYPE", "TOKEN", "HREF",
            // 					"ADDLRSPDATA", "CARDBIN", "NEWCARDBIN", "TC", "TVR", "AID", "TSI", "ATC",
            // 					"APPLAB", "APPPN", "CVM", "TXNRESULT", "TXNPATH", };

            System.out.println("AdditionalInfo: " + str);
            lines = str.split("["+US+STX+FS+ETX+"]"); // or use [<>]
            // load all the param
            for (int i=0; i < lines.length; i++) {
                // System.out.println(lines[i]);
                String[] parts = lines[i].split( "=" );
                // System.out.println(parts);
                if (parts.length <2) responseExtPars.put("extraInfo", parts[0]);
                responseExtPars.put(parts[0], parts[1]);
            }
        }

        // System.out.println("\nResponse Ext Info\n");
        printMap(responseExtPars); // print all the param

    } // processDoCreditResponse


    public boolean isChip() {
        Log.i(TAG, Constants.ENTRY_MOD_PAX +": "+ responseExtPars.get(Constants.ENTRY_MOD_PAX));
        return "4".equals(responseExtPars.get(Constants.ENTRY_MOD_PAX));
    }


    public boolean isSuccessful() {
        Log.i(TAG, "ResponseCode: " + responsePars.get("responseCode"));
        return ("000000".equals( responsePars.get("responseCode")));
    }


    public boolean isCanceled() {
        Log.i(TAG, "Auth Response Code: " + responsePars.get("responseCode"));
        return ("100002".equals( responsePars.get("responseCode")));
    }



    public Map<String, String> getResponseMap(){
        return responseExtPars;
    }

}


