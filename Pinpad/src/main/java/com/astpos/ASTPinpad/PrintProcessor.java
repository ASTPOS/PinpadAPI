package com.astpos.ASTPinpad;

import android.os.AsyncTask;
import android.util.Log;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Iskren Iliev on 11/16/17.
 */

public class PrintProcessor {

    private static Map<String, String> responsePars = new HashMap<String, String>();
    private String timeStampReceipt = new SimpleDateFormat("MM/d/yy  hh:mm aaa").format(Calendar.getInstance().getTime());
    private static Map<String, String> responseExtPars = new HashMap<String, String>();

    private PrintWriter printWriter = null;
    private Boolean socketSuccess = false;

    /**
     * TAG for printing logs
     */
    private final static String TAG = "ASTPOS";

    PrintProcessor(String printer_ip){
        new ConnectToSocket().execute(printer_ip);

        printReceiptMain(printer_ip);
        if (!socketSuccess) {
            return;
        }
    }


    private void printReceiptMain(String printer_ip) {

        // if(CreatePrintWriter()) // print Merchant copy
        // {
        // 	InitPrinterThermal();
        // 	printReceiptMain();
        // 	printReceiptMerchant();
        // }

        //wait up to 5 sec to make connection
        long time= System.currentTimeMillis();
        long endTime = time + 5000;
        while(!socketSuccess) {
            if (System.currentTimeMillis() > endTime) {
                break;
            }
        }
//        if(CreatePrintWriter(printer_ip)) {  // print customer copy

        if (socketSuccess) {
            InitPrinterThermal();
            PrintLine("========================================");
            JustifyCenterThermal();
            DoubleStrikeOnThermal();
            BoldOnThermal();
            PrintLine("AST-PAX RECEIPT");
            InitPrinterThermal();
            PrintLine(" ");

            PrintLine(timeStampReceipt);

            InitPrinterThermal();
            if (!"000000".equals( responsePars.get("responseCode") )) {
                printReceiptError();
            } else if ("A01".equals( responsePars.get("command") )) {
                printReceiptInit();
            } else if ("B01".equals( responsePars.get("command") )) {
                printReceiptBatch();
            } else if ("R09".equals( responsePars.get("command") )) {
                printReceiptReport();
            } else {
                printReceiptMain();
                printReceiptCustomer();
            }
        } else {
            return;
        }
        // DoubleStrikeOnThermal();
        FormFeedThermal();
        InitPrinterThermal();
        CutPaperThermal();
        ClosePrintWriter();
    }


    public void printReceiptCustomer() {
        InitPrinterThermal();
        JustifyCenterThermal();
        PrintLine("CARDHOLDER WILL PAY CARD ISSUER ABOVE ");
        PrintLine("AMOUNT PURSUANT TO CARDHOLDER AGREEMENT ");
        PrintLine(" IMPORTANT: RETAIN FOR YOUR RECORDS! \n ");
        PrintLine("*** Customer Copy ***");
        PrintLine("=========================================");
    }

    public void printReceiptMerchant() {
        InitPrinterThermal();
        JustifyCenterThermal();
        PrintLine("CARDHOLDER WILL PAY CARD ISSUER ABOVE ");
        PrintLine("AMOUNT PURSUANT TO CARDHOLDER AGREEMENT ");
        PrintLine(" IMPORTANT: RETAIN FOR YOUR RECORDS! \n ");
        PrintLine("*** Merchant Copy ***");
        PrintLine("=========================================");
    }


    public void printReceiptError() {
        if (responsePars.containsKey("responseCode")) {
            PrintLine("Response Code: " + responsePars.get("responseCode") );}
        if (responsePars.containsKey("responseCode")) {
            PrintLine("Response Message: " + responsePars.get("responseMessage") );}

        PrintLine(" ");
        PrintLine("=========================================");
    }

    public void printReceiptInit() {
        PrintLine("TYPE: INITIALIZE");
        if (responsePars.containsKey("hostInformation")) {
            PrintLine("Device SN: " + responsePars.get("hostInformation") );}

        PrintLine(" ");
        PrintLine("=========================================");
    }

    public void printReceiptBatch() {
        PrintLine("TYPE: BATCH CLOSE");
        PrintLine(" ");
        if (responsePars.containsKey("batchNumber")) {
            PrintLine("Batch No: "+responsePars.get("batchNumber") );
        }
        String amounts[] = {};
        String transactions[] = {};
        String labels[] = {"CREDIT", "DEBIT", "EBT", "GIFT", "LOYALTY", "CASH", "CHECK"};
        if (responsePars.containsKey("amountInformation")) {
            amounts = responsePars.get("amountInformation").split("[=]");
        }
        if (responsePars.containsKey("transactionType")) {
            transactions = responsePars.get("transactionType").split("[=]");
        }
        for (int i=0; i< amounts.length; i++) {
            PrintLine(labels[i]+"===============");
            PrintLine("Trans# "+transactions[i]+" Total: $"+amounts[i] + "\n");
        }
        PrintLine(" ");
        PrintLine("=========================================");
    }

    public void printReceiptReport() {
        PrintLine("TYPE: HISTORY REPORT");
        PrintLine(" ");
        // batch number == account information
        if (responsePars.containsKey("accountInformation:")) {
            PrintLine("Batch No: "+responsePars.get("accountInformation:") );
        }

        String amounts[] = {};
        String transactions[] = {};
        String labels[] = {"CREDIT", "DEBIT", "EBT", "GIFT", "LOYALTY", "CASH", "CHECK"};
        // total amount == transactionType
        if (responsePars.containsKey("transactionType")) {
            amounts = responsePars.get("transactionType").split("[=]");
        }
        // total count == host info
        if (responsePars.containsKey("hostInformation")) {
            transactions = responsePars.get("hostInformation").split("[=]");
        }
        for (int i=0; i< amounts.length; i++) {
            PrintLine(labels[i]+"===============");
            PrintLine("Trans# "+transactions[i]+" Total: $"+amounts[i] + "\n");
        }
        PrintLine(" ");
        PrintLine("=========================================");
    }

    public void printReceiptMain() {
        // Trans Type not printed if null:  // or Force Post = indep refund
        String transType = responsePars.get("transactionType");
        if (transType == null) transType = " ";
        else if (transType.equals("01")) transType = "SALE";		// Purchase
        else if (transType.equals("02")) transType = "INDEPENDENT RETERN";// Refund OR Independent Refund
        else if (transType.equals("03")) transType = "AUTH";		// Authorization
        else if (transType.equals("04")) transType = "POSTAUTH";	// Completion or Capture
        else if ( Arrays.asList("16", "17", "18", "19", "20", "21", "22").contains(transType)) {
            transType = "VOID";		// Sale Void
        }
        else if (transType.equals("06")) transType = "ADJUST";		// Adjust
        else transType = "OTHER";
        PrintLine("TYPE: " + transType);
        PrintLine(" ");

        // prints the last 4 digit of the account number
        if (responseExtPars.get("account") == null) PrintLine(" ");
        else PrintLine("ACCT: ************ " + responseExtPars.get("account") );

        // Card Type definition - Chapter 4.5 page 46
        String cardType = responseExtPars.get("cardType");
        if (cardType == null) cardType = " ";
        else if (cardType.equals("01")) cardType = "VISA";
        else if (cardType.equals("02")) cardType = "MASTERCARD";
        else if (cardType.equals("03")) cardType = "AMEX";
        else if (cardType.equals("04")) cardType = "DISCOVER";
        else cardType = "OTHER";
        PrintLine("CARD TYPE: " +  cardType);

        // 0:Manual 1:Swipe 2:Contactless 3:Scanner 4:Chip 5:Chip Fall Back Swipe
        String entryMode = responseExtPars.get("entryMode");
        if (entryMode == null) entryMode = " ";
        else if (entryMode.equals("0")) entryMode = "MANUAL";		// Manual or no card required
        else if (entryMode.equals("1")) entryMode = "SWIPED";		// Swipe or Magnetic
        else if (entryMode.equals("2")) entryMode = "CONTACTLESS";	// Tab or contactless
        else if (entryMode.equals("3")) entryMode = "SCANNER";		// Scanner
        else if (entryMode.equals("4")) entryMode = "CHIP";			// Chip
        else if (entryMode.equals("5")) entryMode = "CHIP/SWIPED";	// Chip failed to Swipe

        if (responseExtPars.containsKey("cardHolder")) {
            PrintLine("NAME: " + responseExtPars.get("cardHolder") );}
        if (responseExtPars.containsKey("AID")) { //Application Dedicated File (ADF) Name
            PrintLine("AID: " + responseExtPars.get("AID") );}
        if (responseExtPars.containsKey("TC")) { //Transaction Certificate
            PrintLine("ARQC: " + responseExtPars.get("TC") );}

        PrintLine("ENTRY: " +  entryMode);

        String toPrint = "";

        // prints approval CVD code if any
        if (responseExtPars.containsKey("authCode")){
            toPrint = responseExtPars.get("authCode");
            if (toPrint == null) PrintLine(" ");
            else PrintLine("APPROVAL: " + toPrint);
        }

        // prints HostRef code if any ( added 12/22/2015)
        // Host reference number or (Transaction UID).
        // This field is host dependent; it can be used to run Void/Return transactions.
        if (responseExtPars.containsKey("hostReferenceNumber")) {
            toPrint = responseExtPars.get("hostReferenceNumber");
            if (toPrint == null) PrintLine(" ");
            else PrintLine("HREF: " + toPrint);
        } else PrintLine("HREF: " + responseExtPars.get("HREF"));

        // prints approved amount
        String totalAmount = responseExtPars.get("approveAmount");
        if ( (totalAmount == null)||(totalAmount.isEmpty()) ) totalAmount = " ";
        else totalAmount = String.valueOf(new BigDecimal(totalAmount).movePointLeft(2));
        PrintLine("\nAMOUNT: $ " + totalAmount );

        String amountDue = (responseExtPars.containsKey("amountDue")) ? responseExtPars.get("amountDue") : "";
        if (amountDue != null && !amountDue.equals("0") && !amountDue.isEmpty() ) {
            amountDue = String.valueOf(new BigDecimal(amountDue).movePointLeft(2));
            PrintLine("\nAMOUNT DUE: $ " + amountDue );
        }

        PrintLine("\nTIP: _____________");
        PrintLine("\nTOTAL: ___________");

        PrintLine("");
        PrintLine("\nCustomer Signature:_____________________\n");
        PrintLine("");
        PrintLine("========================================");
    }




    /**
     * process Socket as an Async and return the respond onBackground
     */
    class ConnectToSocket extends AsyncTask<String, Void, Boolean> {

        private Exception exception;

        protected Boolean doInBackground(String... urls) {
            try {
                boolean success = CreatePrintWriter(urls[0]);

                return success;
            } catch (Exception e) {
                this.exception = e;

                return false;
            }
        }

        protected void onPostExecute(Boolean result) {
            Log.d(TAG, "Result: " + result);
            socketSuccess = result;
        }
    }
    /** ******************************************************** **/


    private Socket printSocket = null;
    public boolean CreatePrintWriter(String printer_ip) {
        try {
            // Socket printSocket = new Socket("192.168.1.49",9100);
            printSocket = new Socket(printer_ip,9100);
            printWriter = new PrintWriter(printSocket.getOutputStream(), true);
            return true;
        } catch(Exception ioe) {
            Log.d(TAG, "error: " + ioe);
            return false;
        }
    }

    public void PrintLine(String dataLine) {
        if(dataLine != null) {
            try	{
                printWriter.println(dataLine);
            } catch(Exception ex) {
                Log.d(TAG, "ERROR (ReceiptPrinter.PrintLine()) -- " + ex);
            }
        } else
            Log.d(TAG, "ERROR (ReceiptPrinter.PrintLine()) -- dataLine is null");
    }

    public void FormFeedThermal() {
        printWriter.print("\n\n\n\n\n\n\n ");
    }

    /**
     * Method to initialize the printer
     */
    public void InitPrinterThermal() {
        printWriter.print('');
        printWriter.print('@');
    }
    /**
     * Method to use center alignment
     */
    public void JustifyCenterThermal() {
        printWriter.print('');
        printWriter.print('a');
        printWriter.print('1');
    }

    public void BoldOnThermal() {
        printWriter.print('');
        printWriter.print('!');
        printWriter.print('8');
    }

    public void DoubleStrikeOnThermal() {
        printWriter.print('');
        printWriter.print('G');
        printWriter.print('1');
    }

    public void CutPaperThermal() {
        printWriter.print('');
        printWriter.print('m');
    }

    public void ClosePrintWriter() {
        if(printWriter != null) {
            printWriter.flush();
            printWriter.close();
            printWriter = null;
        }
    }

}
