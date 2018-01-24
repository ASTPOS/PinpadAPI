package com.astpos.ASTPinpad;


// package nobCard;
import android.util.Log;

import com.astpos.ASTPinpad.util.Constants;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import java.text.SimpleDateFormat; // for time stamp
import java.util.*; //for HashMap and Map
import java.io.Console; // for User interaction
import java.io.*; // for valueOf

import static com.astpos.ASTPinpad.PinpadActivity.TAG;

/**
 * Created by Iskren Iliev on 11/14/17.
 */

public class NabPinpadTransaction extends PinpadTransaction{

    private String strToEncode = "";
    private String pinpad_ip = "";

    private final String USER_AGENT = "Mozilla/5.0";

    private Map<String, String> responsePars = new HashMap<String, String>();  // response from Device
    private String timeStampReceipt = new SimpleDateFormat("MM/d/yy  hh:mm aaa").format(Calendar.getInstance().getTime());
    private static Map<String, String> properties = new HashMap<String, String>();

    //Enums for all operations
    public enum Transaction {
        REPORT(1), SALE(2), PREAUTH(3), POSTAUTH(4), ADJUST(5), VOID(6), INDEP_RET(7), DEB_SALE(8), EXIT(0);
        private int value;

        private Transaction(int value) {
            this.value = value;
        }
    }

    // account vars - default used is ASTPOS settings
    private static String merchantID = "";
    private static String workflowId = "";
    private static String host= "";
    private static String applicationProfileId= "";
    private static String identityToken= "";

    // transaction vars
    private static String amount = "";
    private static String guid = "";
    // private static int userInput = 1;
    private static String cardToken = "";
    private static String dateRangeTag = "";
    private static String guidTag = "";
    private static String batchIdTag = "";

    //temp
    private static String transNumber = "";


    // variables used to build Tokenization rerequests
    private static String transaction = "";
    private static String cardData = "";
    private static String paymentAccountDataToken = "";
    private static String industryType = "";
    private static String orderNumber = "";
    private static String dnsIpAddress = "";
    private static String customerPresent = "";
    private static String accountType = "";


    /**
     * ReadFile function is designed for specifically property files. It will wplit
     * each line using '=' and save the first two words as key and value in a dict.
     * Example of input file:
     * PORT=COM2
     * Input: fileName = provided path/name for the file
     * Output: dict = dictionary with key first word on the line and value - second
     **/
    public static Map<String, String> readFile(String fileName) {
        Map<String, String> dict = new HashMap<String, String>();
        try {
            //Create object of FileReader
            FileReader inputFile = new FileReader(fileName);

            //Instantiate the BufferedReader Class
            BufferedReader bufferReader = new BufferedReader(inputFile);

            String line; //Variable to hold the one line data
            while ((line = bufferReader.readLine()) != null) {
                String[] parts = line.split("=");
                String key = parts[0];
                String value = ( !parts[1].isEmpty() ) ? parts[1] : " ";
                dict.put( key, value );
            }
            //Close the buffer reader
            bufferReader.close();
        }
        catch(Exception e) {
            System.out.println("Error while reading file line by line:" + e.getMessage());
        }
        return dict;
    } //readFile



    /**
     * constructor
     * @param ipAddress
     */
    NabPinpadTransaction(String ipAddress) {
        String ipString = "http://".concat(ipAddress).concat(":6200");
//        Log.d(TAG, "Pinpad IP: " + ipString);
        this.pinpad_ip = ipString;
    }

    // Customer intercaction interface to choose type of transaction
    private final static Console console = System.console();

    /* Provides choice of treansaction for user to choose
     * Returns int value of the first char of the input
     * @return (int) input
    */
    public static int askUser() {
        // Capture users response which transaction type to use
        System.out.println("\n*** Enter Transaction Type:\n"
                + "(1) For Reports Menu\n" // close all/ batch close
                + "(2) For Sale\n"       // needs amount
                + "(3) For PreAuth\n"    // needs amount
                + "(4) For PostAuth\n"   // Needs trans num and amount
                + "(5) For Adjust\n"     // Needs trans num and amount
                + "(6) For Void\n"       // Needs trans num
                + "(7) For Indep Return\n"  // Ind Retrun needs the credit card
                + "(8) For Debit Sale\n" // needs amount
                + "(0) For Exit \n" // Report since the last batch
        );
        // if (console == null) {
        //     System.err.println("No console.");
        //     System.exit(1);
        // }
        // String input = console.readLine("Enter transaction type: ");

        char input;
        BufferedReader br = null;
        while(true) {
            try {
                br = new BufferedReader(new InputStreamReader(System.in));
                System.out.print("Enter transaction type: ");
                input = br.readLine().charAt(0);
                break;
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } catch (StringIndexOutOfBoundsException sobe) {
                sobe.printStackTrace();
            }
        }

        return Character.getNumericValue(input);
        // return Character.getNumericValue(input.charAt(0));
    }

    //TODO: make it static in the parent class
    public void printMap( Map<String, String> parsMap ) {
//         System.out.println("\nlines len: " + parsMap.size());
        Log.d(TAG, "===== Response Map Print ========= ");

        SortedSet<String> keys = new TreeSet<String>(parsMap.keySet());
        for (String key : keys) {
            if (!key.equals("SignOnWithTokenResult")) {
                Log.d(TAG, key+": "+parsMap.get(key));
            }
        }
        Log.d(TAG, "================================ ");
    } // print

    /**
     * This method separates each element fromt he response
     * uses the name as a keay in Dic and assigns a values to it
     * responseString: a string to be split
     * Stores all results in external hashmap: responsePars
     * */
    public void processResponse( String responseString ) {
//         Log.d(TAG, responseString);
        if (responseString == null || responseString.isEmpty()) return;

        String[] pars = responseString.split("[<,>]"); // or use [<>]

        for (int i=1; i < pars.length-1; i++) {
            // System.out.print(pars[i]+": "); //// comment out //////////////////////////////////////
            List<String> list = new ArrayList<String>(Arrays.asList(pars));
            list.removeAll(Arrays.asList(""));
            pars = list.toArray(pars);

            if (Arrays.asList("RESPONSE", "", null).contains(pars[i])) continue;
            if (pars[i].toLowerCase().contains("body")) {
                i ++; continue;
            }
            if (pars[i].startsWith("/")) continue;
            if (pars[i].endsWith("/")) continue;
            else {
                //System.out.println(pars[i]+" : "+pars[i+1]);
                if (pars[i+1].indexOf(':') == 1) continue;
                String[] key2 = pars[i].split("[ ]", 2);
                String[] key1 ={};
                if ( (pars[i-1] != null) && (pars[i-1].indexOf(':') == 1) ) {//  && !pars[i-1].endsWith("/")
                    key1 = pars[i-1].split("[ ]", 2);
                    responsePars.put(key1[0]+"_"+key2[0], pars[i+1]);
                    //System.out.println("*** Not Count: "+pars[i]);
                    //System.out.println("***  "+key1[0]+"_"+key2[0]+" : "+pars[i+1]);
                }
                else if ( pars[i].toLowerCase().contains("count") && (pars[i-4] != null) && (pars[i-4].indexOf(':') == 1) ) {
                    key1 = pars[i-4].split("[ ]", 2);
                    responsePars.put(key1[0]+"_"+key2[0], pars[i+1]);
                    //System.out.println("*** Count: "+pars[i]);
                    //System.out.println("***  "+key1[0]+"_"+key2[0]+" : "+pars[i+1]);
                }
                else responsePars.put(key2[0], pars[i+1]);
                i++;
                // System.out.println(pars[i]);
                // System.out.println(pars[i-1] + " : " + pars[i] + " : " + pars[i+1]);
                // System.out.println(pars[i].indexOf(':'));
                // System.out.println("***************************************************");
            } //else
        } //for
    } // processResponse


    public void processSummaryResponse( String responseString ) {
        // System.out.println(responseString);
        transNumber = "";
        String[] transactionPars = responseString.split("<SummaryDetail>"); // or use [<>]

        for (int j=1; j < transactionPars.length; j++) {
            // System.out.println(transactionPars[j]); //// comment out //////////////////////////////////////
            transNumber = Integer.toString(j);
            String[] pars = transactionPars[j].split("[<,>]"); // or use [<>]
            for (int i=1; i < pars.length-1; i++) {
                if (pars[i]== null || pars[i].isEmpty() || pars[i].startsWith("/") || pars[i].endsWith("/") ) {
                    // System.out.println("end: " + pars[i]);
                    continue;
                }
                else {
                    responsePars.put(transNumber+":"+pars[i], pars[i+1]);
                    // System.out.println(pars[i]);
                    // System.out.println(pars[i] + " : " + pars[i+1]);

                    i++;
                } //else
            } //for
            // printMap(responsePars);
            // console.readLine( "Hit enter to cont..."+ transNumber);
        } //for

    } //processSummaryResponse





    public static String getDateRangeTag(String tagType){
        String startDate = console.readLine( "Enter start date (2017-01-01): " );
        String endDate = console.readLine( "Enter end date (2017-01-01): " );
        // startDate = endDate = "2016-09-09";
        String dateRangeString=
                "    <" + tagType + ">"+
                        "      <ns1:EndDateTime xmlns:ns1=\"http://schemas.ipcommerce.com/CWS/v2.0/DataServices\">"+endDate+"T23:59:59-08:00</ns1:EndDateTime>"+
                        "      <ns2:StartDateTime xmlns:ns2=\"http://schemas.ipcommerce.com/CWS/v2.0/DataServices\">"+startDate+"T00:00:00-08:00</ns2:StartDateTime>"+
                        "    </" + tagType + ">"
                ;
        return dateRangeString;
    }



    /** ====================================================================================== **/
/** ****
//    public static void main(String[] args) throws Exception {
//        if ( args.length < 2 ) {
//            System.err.println("Usage: java NabCardHttpConnection <PINPAD IP> <PRINTER IP>");
//            System.err.println("\te.g. java NabCardHttpConnection 192.168.1.211 192.168.1.49");
//            System.exit(0);
//        }
//
//        properties = readFile("nab.properties");
//        System.out.println("\n============== Account Settings ==============");
//        printMap(properties);
//        System.out.println("==========================================");
//        merchantID = properties.get("MerchantProfileId");
//        workflowId = properties.get("ServiceID");
//        host= properties.get("Host");
//        applicationProfileId= properties.get("ApplicationProfileId");
//        identityToken= properties.get("IdentityToken");
//        // System.out.println("applicationProfileId: "+ applicationProfileId);
//
//        NabPinpadTransaction http = new NabPinpadTransaction();
//        String pinpad_ip = "http://"+args[0]+":6200";
//        String printer_ip = args[1];
//        System.out.println("\n============== IP Settings ==============");
//        System.out.println("PINPAD IP: " + pinpad_ip);
//        System.out.println("PRINTER IP: " + printer_ip);
//        System.out.println("==========================================");
//
//
//        int choice = 0; // used for additional choices
//        int userInput = 1;
//
//
//        while (true) {
//            userInput = askUser();
//            Transaction transType = Transaction.values()[userInput-1];
//            System.out.println("Transaction: " + transType);
//
//            if(userInput == 0) {break;}
//
//            // System.in.read(); // to pause the flow for debug ONLY/
//
//            try {
//                BufferedReader br = null;
//                br = new BufferedReader(new InputStreamReader(System.in));
//
//
//                switch(transType) {
//                    case REPORT: {     // 1
//                        //Used for bringing a submenu and pull different reports
//                        System.out.print( "(1) Transaction Details\n" +
//                                "(2) Uncaptured report\n" +
//                                "(3) Captured report\n" +
//                                "(8) Batch report\n" +
//                                "(9) Close Batch\n"
//                        );
//
//                        choice = Character.getNumericValue( br.readLine().charAt(0) );
//
//                        http.sendPostSignOn();  // SignOn
//                        if (choice == 9) {
//                            http.sendPostBatchClose(); // BatchClose
//                        }
//                        else if (choice == 8) { // Batch report
//                            String batchId = console.readLine( "Enter Batch ID or 0 for none: " );
//                            if (batchId.equals("0")) {
//                                dateRangeTag = getDateRangeTag("BatchDateRange");
//
//                                batchIdTag = "";
//                            }
//                            else {
//                                dateRangeTag = "";
//                                batchIdTag =
//                                        "    <BatchIds>"+
//                                                "      <ns3:string xmlns:ns3=\"http://schemas.microsoft.com/2003/10/Serialization/Arrays\">"+batchId+"</ns3:string>"+
//                                                "    </BatchIds>"
//                                ;
//                            }
//                            http.sendPostBatchReport(); //QueryBatch
//                        }
//                        else if (choice == 2) { //uncaptured report
//                            dateRangeTag = getDateRangeTag("TransactionDateRange");
//
//                            guid = console.readLine( "Enter Transaction ID or 0 for none: " );
//                            if (guid.equals("0")) {
//                                guidTag = "";
//                            } else {
//                                guidTag =
//                                        "   <TransactionIds>" +
//                                                "     <ns1:string xmlns:ns1=\"http://schemas.microsoft.com/2003/10/Serialization/Arrays\">"+guid+"</ns1:string>" +
//                                                "   </TransactionIds>"
//                                ;
//                            }
//                            http.sendPostUncapturedReport(); //QueryTransactionsSummary
//
//                            System.out.println("Total transactions: "+ transNumber);
//                        }
//                        else if (choice == 3) { //captured report
//                            dateRangeTag = getDateRangeTag("TransactionDateRange");
//
//                            guid = console.readLine( "Enter Transaction ID or 0 for none: " );
//                            if (guid.equals("0")) {
//                                guidTag = "";
//                            } else {
//                                guidTag =
//                                        "   <TransactionIds>" +
//                                                "     <ns1:string xmlns:ns1=\"http://schemas.microsoft.com/2003/10/Serialization/Arrays\">"+guid+"</ns1:string>" +
//                                                "   </TransactionIds>"
//                                ;
//                            }
//                            http.sendPostTransactionSummary(); //QueryTransactionsSummary
//
//                            System.out.println("Total transactions: "+ transNumber);
//                        }
//                        else if (choice == 1) { //uncaptured report
//                            // dateRangeTag = getDateRangeTag("TransactionDateRange");
//
//                            guid = console.readLine( "Enter Transaction ID or 0 for none: " );
//                            guidTag =
//                                    "   <TransactionIds>" +
//                                            "     <ns1:string xmlns:ns1=\"http://schemas.microsoft.com/2003/10/Serialization/Arrays\">"+guid+"</ns1:string>" +
//                                            "   </TransactionIds>"
//                            ;
//
//                            http.sendPostTransactionsDetail(); //QueryTransactionsDetail
//                            cardToken = responsePars.get("PaymentAccountDataToken"); // save the token in DB
//                            System.out.println("PmtAcctDataTkn: " + cardToken);
//                        }
//                        break;
//                    }
//                    case SALE: {        // 2
//                        // Use details
//                        // (1) one time sale: is when using the pinpad and credit card is swiped
//                        // (2) save token: sends the info to the server and captures Data Token related to the cc
//                        // (3) use token: send the captured token to the server and process transaction w/o using cc
//                        //
//                        amount = console.readLine("Enter amount(1.00 for $1.00): ");
//                        choice = Integer.parseInt(console.readLine( "(1) One time sale (use pinpad) \n" +
//                                "(2) save token (send preset CC info to server) \n" +
//                                "(3) use token (requires step (2) first) \n:" ));
//                        if (choice == 1) {      // using the pinpad
//                            http.sendPostToDevice("CCR1", pinpad_ip);
//                        }
//                        else
//                        {   // set all variables for the POST request
//                            transaction = "AuthorizeAndCapture";
//                            industryType = "Retail"; //"Ecommerce";
//                            orderNumber = "123";
//                            dnsIpAddress = "8.8.8.8";
//                            customerPresent = "Present"; //"Ecommerce";
//                            accountType = "CheckingAccount";
//
//                            if (choice == 2) { // send data to server and capture token
//                                cardData =
//                                "    <ns1:CardData>" +
//                                "     <ns1:CardType>Visa</ns1:CardType>" +
//                                "     <ns1:CardholderName>John Doe</ns1:CardholderName>" +
//                                "     <ns1:PAN>4111111111111111</ns1:PAN>" +
//                                "     <ns1:Expire>1210</ns1:Expire>" +
//                                "    </ns1:CardData>" +
//                                "     <ns1:CardSecurityData>" +
//                                "      <ns1:CVDataProvided>Provided</ns1:CVDataProvided>" +
//                                "      <ns1:CVData>111</ns1:CVData>" +
//                                "     </ns1:CardSecurityData>"
//                                ;
//                            }
//                            else if (choice == 3) { // use token to place transaction
//                                cardToken = responsePars.get("PaymentAccountDataToken"); // retrieve token
//                                paymentAccountDataToken =
//                                    "   <ns2:PaymentAccountDataToken " +
//                                    "     xmlns:ns2=\"http://schemas.ipcommerce.com/CWS/v2.0/Transactions\">" +
//                                    cardToken +
//                                    "   </ns2:PaymentAccountDataToken> "
//                                ;
//                            }
//
//                            System.out.println("PmtAcctDataTkn: " + cardToken);
//
//                            http.sendPostSignOn();       // SignOn
//                            http.sendPostTokenization(); // do AuthAndCapture without card
//                            cardToken = responsePars.get("PaymentAccountDataToken"); // save the token in DB
//                        }
//                        break;
//                    }
////                    case PREAUTH: {     // 3
////                        amount = console.readLine("Enter amount(1.00 for $1.00): ");
////                        choice = Integer.parseInt(console.readLine( "(1) One time auth \n" +
////                                "(2) save token \n:" ));
////                        if (choice == 1) {
////                            http.sendPostToDevice("CCR2", pinpad_ip);
////                        }
////                        else if (choice == 2) {
////                            transaction = "Authorize";
////                            industryType = "Retail"; //"Restaurant";
////                            orderNumber = "69465";
////                            dnsIpAddress = "8.8.8.8";
////                            customerPresent = "Present";
////                            accountType = "CheckingAccount";
////
////                            cardData =
////                                    "    <ns1:CardData>" +
////                                            "      <ns1:CardType>AmericanExpress</ns1:CardType> " +
////                                            "      <ns1:CardholderName>John Doe</ns1:CardholderName> " +
////                                            "      <ns1:PAN>4847350637169739</ns1:PAN> " +
////                                            "      <ns1:Expire>0221</ns1:Expire> " +
////                                            "      <ns1:Track2Data " +
////                                            "      xsi:nil=\"true\"/> " +
////                                            "    </ns1:CardData>" +
////                                            "    <ns1:CardSecurityData/> "
////
////                            // "    <ns1:CardData>" +
////                            // "      <ns1:CardType>AmericanExpress</ns1:CardType> " +
////                            // "      <ns1:CardholderName>Naveen Kumar</ns1:CardholderName> " +
////                            // "      <ns1:PAN>372723072572016</ns1:PAN> " +
////                            // "      <ns1:Expire>1022</ns1:Expire> " +
////                            // "      <ns1:Track2Data " +
////                            // "      xsi:nil=\"true\"/> " +
////                            // "    </ns1:CardData>" +
////                            // "     <ns1:CardSecurityData>" +
////                            // "      <ns1:CVDataProvided>Provided</ns1:CVDataProvided>" +
////                            // "      <ns1:CVData>4211</ns1:CVData>" +
////                            // "     </ns1:CardSecurityData>"
////                            ;
////
////                            http.sendPostSignOn();  // SignOn
////                            http.sendPostTokenization();    // do AuthAndCapture without card
////                            cardToken = responsePars.get("PaymentAccountDataToken"); // save the token in DB
////                        }
////                        else {
////                            System.out.println("Wrong choice! Try again!");
////                        }
////                        break;
////                    }
////                    case POSTAUTH: {    // 4
////                        http.sendPostSignOn();  // SignOn
////                        guid = console.readLine("Enter GUID(32 A/N chars): ");
////                        amount = console.readLine("Enter amount(1.00 for $1.00): ");
////                        http.sendPostCapture();
////                        break;
////                    }
////                    case ADJUST: {      // 5
////                        http.sendPostSignOn();  // SignOn
////
////                        choice = Integer.parseInt(console.readLine( "(1) Adjust (+)\n" +
////                                "(2) Adjust (-)\n:" ));
////                        if (choice == 1) { //Adjust (+)
////                            //capture DataToken using TransactionDetail
////                            //Undo/Void the original transaction
////                            //Make a new transaction with new higher amount
////                            transaction = "AuthorizeAndCapture";
////                            industryType = "Retail"; //"Ecommerce";
////                            orderNumber = "123";
////                            dnsIpAddress = "8.8.8.8";
////                            customerPresent = "Retail"; //"Ecommerce";
////                            accountType = "CheckingAccount";
////
////
////                            guid = console.readLine("Enter GUID(32 A/N chars): ");
////                            guidTag =
////                                    "   <TransactionIds>" +
////                                            "     <ns1:string xmlns:ns1=\"http://schemas.microsoft.com/2003/10/Serialization/Arrays\">"+guid+"</ns1:string>" +
////                                            "   </TransactionIds>"
////                            ;
////                            // amount = console.readLine("Enter new total amount(1.00 for $1.00): ");
////                            amount = console.readLine("Enter partial return amount(1.00 for $1.00): ");
////
////                            http.sendPostTransactionsDetail();
////                            cardToken = responsePars.get("PaymentAccountDataToken"); // save the token in DB
////                            paymentAccountDataToken =
////                                    "   <ns2:PaymentAccountDataToken " +
////                                            "     xmlns:ns2=\"http://schemas.ipcommerce.com/CWS/v2.0/Transactions\">" +
////                                            cardToken +
////                                            "   </ns2:PaymentAccountDataToken> "
////                            ;
////                            System.out.println("\n **** Did TransactionsDetail ****");
////                            System.out.println("\n **** PmtAcctDataTkn: " + cardToken + "****");
////
////                            // http.sendPostVoid();
////
////                            http.sendPostReturnById();
////
////                            System.out.println("\n **** Did Void ****");
////
////                            amount = console.readLine("Enter new total amount(1.00 for $1.00): ");
////
////                            http.sendPostTokenization();
////                            System.out.println("\n **** Did Tokenization ****");
////                        } else if(choice == 2) { // Adjust (-)
////                            // requirs the GUID of the original transaction; every time adjust
////                            // is performed on one transaction it will require its original GUID
////                            // Amount send with Adjust/ReturnById is the amount being deducted from original amt
////                            guid = console.readLine("Enter GUID(32 A/N chars): ");
////                            amount = console.readLine("Enter partial return amount(1.00 for $1.00): ");
////                            http.sendPostReturnById();
////                        } else {
////                            System.out.println("Wrong input please try again!");
////                        }
////                        break;
////                    }
////                    case VOID: {        // 6
////                        http.sendPostSignOn();  // SignOn
////                        // requires the original GUID or if any Adjust have been performed
////                        // requires the GUID of the last Adjust
////                        guid = console.readLine("Enter GUID(32 A/N chars): ");
////                        http.sendPostVoid();
////                        break;
////                    }
////                    case INDEP_RET: {   // 7
////                        amount = console.readLine("Enter amount(1.00 for $1.00): ");
////                        http.sendPostToDevice("CCR9", pinpad_ip);
////                        break;
////                    }
////                    case DEB_SALE: {    // 8
////                        amount = console.readLine("Enter amount(1.00 for $1.00): ");
////                        http.sendPostToDevice("DB00", pinpad_ip);
////                        break;
////                    }
//                    default: break;
//                }
//
//                responsePars.clear();
//
//            } catch (Exception ioe) {
//                System.out.println(ioe);
//            } // end try/catch
//
//        } //while
//
//    } // main

*/


    /** ====================================================================================== **/



    // HTTP POST request
    /*
    Send URL request to the device iPP320
    amount = amount of the transaction
    transaction = par based on the map below:
        **********************************************************
        CCR1 = Sale
        CCR2 = PreAuth
        CCR9 = Return Unlinked
        DB00 = Sale Debit
        DB01 = Return Unlinked Debit
        *************************************************************

    */


    @Override
    public void setToSale(String amount) {
        // NOT USED FOR NAB
    }

    public void setToSaleCredit(String amount){
        this.strToEncode =
            "<DETAIL>" +
            "<TRAN_TYPE>"+"CCR1"+"</TRAN_TYPE>" +
            "<AMOUNT>"+amount+"</AMOUNT>" +
            "</DETAIL>";
    }

    public void setToSaleDebit(String amount){
        this.strToEncode =
            "<DETAIL>" +
            "<TRAN_TYPE>"+"DB00"+"</TRAN_TYPE>" +
            "<AMOUNT>"+amount+"</AMOUNT>" +
            "</DETAIL>";
    }

    public void setToPreAuth(String amount){
        this.strToEncode =
            "<DETAIL>" +
            "<TRAN_TYPE>"+"CCR2"+"</TRAN_TYPE>" +
            "<AMOUNT>"+amount+"</AMOUNT>" +
            "</DETAIL>";
    }

    public String getPostData() {
        return this.strToEncode;
    }

    public String getUrlString() {
        return this.pinpad_ip;
    }

    public Map<String, String> getResponseMap(){
        return responsePars;
    }

    public boolean isSuccessful() {
//        Log.i(TAG, "Auth Response Code: " + responsePars.get("AUTH_RESP"));
        return ("00".equals( responsePars.get("AUTH_RESP")));
    }

    public boolean isCanceled() {
        Log.i(TAG, "Auth Response Code: " + responsePars.get(Constants.AUTH_RESP));
        return "S0".equals( responsePars.get(Constants.AUTH_RESP));
    }


    /**
     * X =  Manual or no card required
     * D = Swipe or Magnetic
     * G = Chip or inserted
     * @return true if chip entry mode
     */
    public boolean isChip(){
        Log.i(TAG, Constants.ENTRY_MOD_NAB +": "+ responsePars.get(Constants.ENTRY_MOD_NAB));
        return "G".equals(responsePars.get(Constants.ENTRY_MOD_NAB));
    }

    @Override
    public void setDebit() {
        // NOT USED FOR NAB
    }

    @Override
    public void setCredit() {
        // NOT USED FOR NAB
    }

    @Override
    public void setReboot() {
        // NOT USED FOR NAB
    }


    private void sendPostToDevice( String transaction, String pinpad_ip) throws Exception {
        // String url = "http://192.168.1.158:6200";
        String url = pinpad_ip;
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        //add reuqest header
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        con.setDoOutput(true);

        // String urlText = "<DETAIL><TRAN_TYPE>CCR1</TRAN_TYPE><AMOUNT>1.00</AMOUNT></DETAIL>";
        // String transaction = "CCR2";
        // float amount = 1.00f;
        // String urlText = String.format(
        //                      "<DETAIL>" +
        //                      "<TRAN_TYPE>%s</TRAN_TYPE>" +
        //                      "<AMOUNT>%.2f</AMOUNT>" +
        //                      "</DETAIL>", transaction, amount );
        String urlText =
                "<DETAIL>" +
                        "<TRAN_TYPE>"+transaction+"</TRAN_TYPE>" +
                        "<AMOUNT>"+amount+"</AMOUNT>" +
                        "</DETAIL>";

        // Send post request
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(urlText);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();
        System.out.println("Sending 'POST' request to URL : " + url);
        System.out.println("Response Code : " + responseCode+ "\n");

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        String str = response.toString();
//        processResponse(str);

//        printMap(responsePars);
    } //sendPostToDevice


    private void sendPostSignOn() throws Exception {
        String url = "https://"+host+"/2.0.18/SvcInfo";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        String soapAction =  "http://schemas.ipcommerce.com/CWS/v2.0/ServiceInformation/ICWSServiceInformation/SignOnWithToken";
        String signOnWithToken = identityToken;

        String urlText =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<SOAP-ENV:Envelope " +
                        " xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                        " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"> " +
                        "<SOAP-ENV:Body> " +
                        "  <SignOnWithToken xmlns=\"http://schemas.ipcommerce.com/CWS/v2.0/ServiceInformation\"> " +
                        "   <identityToken>"+signOnWithToken+"</identityToken> " +
                        "  </SignOnWithToken> " +
                        "</SOAP-ENV:Body> " +
                        "</SOAP-ENV:Envelope>"  ;

        //add reuqest header
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        con.setRequestProperty("Content-Type","text/xml; charset=UTF-8");
        con.setRequestProperty("Content-Length", ""+Integer.toString(urlText.getBytes().length) );
        con.setRequestProperty("SOAPAction", soapAction);
        con.setDoOutput(true);

        // prepare the message
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        // wr.write(urlText.getBytes());
        wr.writeBytes(urlText);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();

        System.out.println("Sending 'POST' request to URL : " + url);
        System.out.println("Response Code : " + responseCode + "\n");

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        // close connection
        con.disconnect();

        String str = response.toString();
        processResponse(str);

        // printMap(responsePars);
    } //sendPostSignOn

    private void sendPostBatchReport() throws Exception {
        String url = "https://"+host+"/2.0.18/DataServices/TMS";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        String soapAction =  "http://schemas.ipcommerce.com/CWS/v2.0/DataServices/TMS/ITMSOperations/QueryBatch";
        String sessionToken = responsePars.get("SignOnWithTokenResult");
        String urlText =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<SOAP-ENV:Envelope" +
                        "   xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        "   xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"> " +
                        "<SOAP-ENV:Body>" +
                        " <QueryBatch xmlns=\"http://schemas.ipcommerce.com/CWS/v2.0/DataServices/TMS\">" +
                        "   <sessionToken>"+sessionToken+"</sessionToken>"+
                        "   <queryBatchParameters>"+
                        dateRangeTag +
                        batchIdTag +
                        "   <MerchantProfileIds>"+
                        "     <ns3:string xmlns:ns3=\"http://schemas.microsoft.com/2003/10/Serialization/Arrays\">"+merchantID+"</ns3:string>"+
                        "   </MerchantProfileIds>"+
                        "   <ServiceKeys xsi:nil=\"true\"/>"+
                        "   <TransactionIds xsi:nil=\"true\"/>"+
                        "  </queryBatchParameters>"+
                        "  <pagingParameters>"+
                        "   <ns3:Page xmlns:ns3=\"http://schemas.ipcommerce.com/CWS/v2.0/DataServices\">0</ns3:Page>"+
                        "   <ns4:PageSize xmlns:ns4=\"http://schemas.ipcommerce.com/CWS/v2.0/DataServices\">50</ns4:PageSize>"+
                        "  </pagingParameters>"+
                        " </QueryBatch>"+
                        "</SOAP-ENV:Body>" +
                        "</SOAP-ENV:Envelope>";

        //add reuqest header
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        con.setRequestProperty("Content-Type","text/xml; charset=UTF-8");
        con.setRequestProperty("Content-Length", ""+Integer.toString(urlText.getBytes().length) );
        con.setRequestProperty("SOAPAction", soapAction);
        con.setDoOutput(true);

        // prepare the message
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        // wr.write(urlText.getBytes());
        wr.writeBytes(urlText);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();

        System.out.println("Sending 'POST' request to URL : " + url);
        System.out.println("Response Code : " + responseCode + "\n");

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
            //System.out.println(inputLine);
        }
        in.close();

        // close connection
        con.disconnect();

        String str = response.toString();
        processResponse(str);

        printMap(responsePars);
    } //sendPostBatchReport


    private void sendPostUncapturedReport() throws Exception {
        String url = "https://"+host+"/2.0.18/DataServices/TMS";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        String soapAction =  "http://schemas.ipcommerce.com/CWS/v2.0/DataServices/TMS/ITMSOperations/QueryTransactionsSummary";
        String sessionToken = responsePars.get("SignOnWithTokenResult");
        String urlText =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<SOAP-ENV:Envelope" +
                        "   xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        "   xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"> " +
                        "<SOAP-ENV:Body>" +
                        " <QueryTransactionsSummary xmlns=\"http://schemas.ipcommerce.com/CWS/v2.0/DataServices/TMS\">" +
                        "   <sessionToken>"+sessionToken+"</sessionToken>"+
                        "   <queryTransactionsParameters>"+
                        "   <CaptureStates>"+
                        "     <ns1:CaptureState xmlns:ns1=\"http://schemas.ipcommerce.com/CWS/v2.0/Transactions\">ReadyForCapture</ns1:CaptureState>" +
                        "   </CaptureStates>" +
                        "   <MerchantProfileIds>"+
                        "     <ns1:string xmlns:ns1=\"http://schemas.microsoft.com/2003/10/Serialization/Arrays\">"+merchantID+"</ns1:string>" +
                        "   </MerchantProfileIds>" +
                        dateRangeTag +
                        guidTag +
                        "  </queryTransactionsParameters>" +
                        "  <pagingParameters>" +
                        "   <ns1:Page xmlns:ns1=\"http://schemas.ipcommerce.com/CWS/v2.0/DataServices\">0</ns1:Page>" +
                        "   <ns2:PageSize xmlns:ns2=\"http://schemas.ipcommerce.com/CWS/v2.0/DataServices\">50</ns2:PageSize>" +
                        "  </pagingParameters>" +
                        " </QueryTransactionsSummary>" +
                        "</SOAP-ENV:Body>" +
                        "</SOAP-ENV:Envelope>";

        //add reuqest header
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        con.setRequestProperty("Content-Type","text/xml; charset=UTF-8");
        con.setRequestProperty("Content-Length", ""+Integer.toString(urlText.getBytes().length) );
        con.setRequestProperty("SOAPAction", soapAction);
        con.setDoOutput(true);

        // prepare the message
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        // wr.write(urlText.getBytes());
        wr.writeBytes(urlText);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();

        System.out.println("Sending 'POST' request to URL : " + url);
        System.out.println("Response Code : " + responseCode + "\n");

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
            //System.out.println(inputLine);
        }
        in.close();

        // close connection
        con.disconnect();

        String str = response.toString();
        processSummaryResponse(str);

        printMap(responsePars);
    } //sendPostUncapturedReport


    // use tran_ID for testing: 64D4F33596F04123A2371718079506C8
// PaymentAccountDataToken: 64d4f335-96f0-4123-a237-1718079506c804d1f1ea-5c09-472d-820c-ba036bb5eef5
    private void sendPostTransactionsDetail() throws Exception {
        String url = "https://"+host+"/2.0.18/DataServices/TMS";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        String soapAction =  "http://schemas.ipcommerce.com/CWS/v2.0/DataServices/TMS/ITMSOperations/QueryTransactionsDetail";
        String sessionToken = responsePars.get("SignOnWithTokenResult");
        String urlText =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<SOAP-ENV:Envelope" +
                        "   xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        "   xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"> " +
                        "<SOAP-ENV:Body>" +
                        " <QueryTransactionsDetail xmlns=\"http://schemas.ipcommerce.com/CWS/v2.0/DataServices/TMS\">" +
                        "   <sessionToken>"+sessionToken+"</sessionToken>"+
                        "   <queryTransactionsParameters>"+
                        guidTag +
                        "  </queryTransactionsParameters>" +
                        "  <transactionDetailFormat>CWSTransaction</transactionDetailFormat>" +
                        "  <pagingParameters>" +
                        "   <ns2:Page xmlns:ns2=\"http://schemas.ipcommerce.com/CWS/v2.0/DataServices\">0</ns2:Page>" +
                        "   <ns3:PageSize xmlns:ns3=\"http://schemas.ipcommerce.com/CWS/v2.0/DataServices\">10</ns3:PageSize>" +
                        "  </pagingParameters>" +
                        "  <includeRelated>true</includeRelated>" +
                        " </QueryTransactionsDetail>" +
                        "</SOAP-ENV:Body>" +
                        "</SOAP-ENV:Envelope>"
                ;

        //add reuqest header
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        con.setRequestProperty("Content-Type","text/xml; charset=UTF-8");
        con.setRequestProperty("Content-Length", ""+Integer.toString(urlText.getBytes().length) );
        con.setRequestProperty("SOAPAction", soapAction);
        con.setDoOutput(true);

        // prepare the message
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        // wr.write(urlText.getBytes());
        wr.writeBytes(urlText);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();

        System.out.println("Sending 'POST' request to URL : " + url);
        System.out.println("Response Code : " + responseCode + "\n");

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();


        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
            //System.out.println(inputLine);
        }
        in.close();

        // close connection
        con.disconnect();

// testing ////////////////////////
        // System.out.println("Response msg : " + response + "\n");

        String str = response.toString();
        processSummaryResponse(str);
        processResponse(str);

        printMap(responsePars);
    } //sendPostUncapturedReport

    private void sendPostTransactionSummary() throws Exception {
        String url = "https://"+host+"/2.0.18/DataServices/TMS";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        String soapAction =  "http://schemas.ipcommerce.com/CWS/v2.0/DataServices/TMS/ITMSOperations/QueryTransactionsSummary";
        String sessionToken = responsePars.get("SignOnWithTokenResult");
        String urlText =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<SOAP-ENV:Envelope" +
                        "   xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        "   xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"> " +
                        "<SOAP-ENV:Body>" +
                        " <QueryTransactionsSummary xmlns=\"http://schemas.ipcommerce.com/CWS/v2.0/DataServices/TMS\">" +
                        "   <sessionToken>"+sessionToken+"</sessionToken>"+
                        "   <queryTransactionsParameters>"+
                        "   <MerchantProfileIds>"+
                        "     <ns1:string xmlns:ns1=\"http://schemas.microsoft.com/2003/10/Serialization/Arrays\">"+merchantID+"</ns1:string>" +
                        "   </MerchantProfileIds>" +
                        dateRangeTag +
                        guidTag +
                        "  </queryTransactionsParameters>" +
                        "  <pagingParameters>" +
                        "   <ns1:Page xmlns:ns1=\"http://schemas.ipcommerce.com/CWS/v2.0/DataServices\">0</ns1:Page>" +
                        "   <ns2:PageSize xmlns:ns2=\"http://schemas.ipcommerce.com/CWS/v2.0/DataServices\">50</ns2:PageSize>" +
                        "  </pagingParameters>" +
                        " </QueryTransactionsSummary>" +
                        "</SOAP-ENV:Body>" +
                        "</SOAP-ENV:Envelope>";

        //add reuqest header
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        con.setRequestProperty("Content-Type","text/xml; charset=UTF-8");
        con.setRequestProperty("Content-Length", ""+Integer.toString(urlText.getBytes().length) );
        con.setRequestProperty("SOAPAction", soapAction);
        con.setDoOutput(true);

        // prepare the message
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        // wr.write(urlText.getBytes());
        wr.writeBytes(urlText);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();

        System.out.println("Sending 'POST' request to URL : " + url);
        System.out.println("Response Code : " + responseCode + "\n");

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
            //System.out.println(inputLine);
        }
        in.close();

        // close connection
        con.disconnect();

        String str = response.toString();
        processSummaryResponse(str);

        printMap(responsePars);
    } //sendPostUncapturedReport



    private void sendPostBatchClose() throws Exception {
        String url = "https://"+host+"/CWS/1.0/SOAP/TPS.svc";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        String soapAction =  "http://schemas.ipcommerce.com/CWS/v2.0/TransactionProcessing/ICwsTransactionProcessing/CaptureAll";
        String sessionToken = responsePars.get("SignOnWithTokenResult");
        String urlText =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        "   xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"> " +
                        "<SOAP-ENV:Body>" +
                        "  <CaptureAll xmlns=\"http://schemas.ipcommerce.com/CWS/v2.0/TransactionProcessing\"> " +
                        "   <sessionToken>"+sessionToken+"</sessionToken>" +
                        "   <differenceData/>" +
                        "   <applicationProfileId>"+applicationProfileId+"</applicationProfileId>" +
                        "   <merchantProfileId>"+merchantID+"</merchantProfileId>" +
                        "   <workflowId>"+workflowId+"</workflowId>" +
                        "  </CaptureAll>" +
                        "</SOAP-ENV:Body>" +
                        "</SOAP-ENV:Envelope>";


        //add reuqest header
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        con.setRequestProperty("Content-Type","text/xml; charset=UTF-8");
        con.setRequestProperty("Content-Length", ""+Integer.toString(urlText.getBytes().length) );
        con.setRequestProperty("SOAPAction", soapAction);
        con.setDoOutput(true);

        // prepare the message
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        // wr.write(urlText.getBytes());
        wr.writeBytes(urlText);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();

        System.out.println("Sending 'POST' request to URL : " + url);
        System.out.println("Response Code : " + responseCode + "\n");

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
            // System.out.println(inputLine);
        }
        in.close();

        // close connection
        con.disconnect();

        String str = response.toString();
        processResponse(str);

        printMap(responsePars);
    } //sendPostBatchClose


    /************ TOKENIZATION **********************************************
     used to SALE with a card and save a token.
     the response will return a token for this card to be used in the future
     *********************************************************************** */
    private void sendPostTokenization() throws Exception {
        String url = "https://"+host+"/2.0.18/Txn";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        String soapAction =  "http://schemas.ipcommerce.com/CWS/v2.0/TransactionProcessing/ICwsTransactionProcessing/"+transaction;
        String sessionToken = responsePars.get("SignOnWithTokenResult");
        // guid = responsePars.get("AUTH_GUID");

        String urlText =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
                        "<SOAP-ENV:Envelope " +
                        " xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                        " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"> " +
                        " <SOAP-ENV:Body> " +
                        "  <"+transaction+" " +
                        "    xmlns=\"http://schemas.ipcommerce.com/CWS/v2.0/TransactionProcessing\"> " +
                        "   <sessionToken>" + sessionToken + "</sessionToken> " +
                        "   <transaction " +
                        "    xmlns:ns1=\"http://schemas.ipcommerce.com/CWS/v2.0/Transactions/Bankcard\" " +
                        "       xsi:type=\"ns1:BankcardTransaction\"> " +
                        "    <ns1:TenderData>" +
                        ""   +cardData+                 ///* use original card info */
                        ""   +paymentAccountDataToken + ///* replaces card info when using token */
                        "    </ns1:TenderData> " +
                        "    <ns1:TransactionData> " +
                        "     <ns2:Amount " +
                        "      xmlns:ns2=\"http://schemas.ipcommerce.com/CWS/v2.0/Transactions\">"+amount+"</ns2:Amount> " +
                        "     <ns3:CurrencyCode " +
                        "      xmlns:ns3=\"http://schemas.ipcommerce.com/CWS/v2.0/Transactions\">USD</ns3:CurrencyCode> " +
                        "     <ns4:TransactionDateTime " +
                        "      xmlns:ns4=\"http://schemas.ipcommerce.com/CWS/v2.0/Transactions\">2012-12-11T10:28:11</ns4:TransactionDateTime> " +
                        "     <ns5:Reference xmlns:ns5=\"http://schemas.ipcommerce.com/CWS/v2.0/Transactions\">xyt</ns5:Reference> " +
                        "     <ns1:AccountType>"+accountType+"</ns1:AccountType> " +
                        "     <ns1:CustomerPresent>"+customerPresent+"</ns1:CustomerPresent> " +

                        // from auth ////////
                        "     <ns1:EmployeeId>11</ns1:EmployeeId> " +
                        //////////////////

                        "     <ns1:EntryMode>Keyed</ns1:EntryMode> " +
                        "     <ns1:GoodsType>PhysicalGoods</ns1:GoodsType> " +
                        "     <ns1:IndustryType>"+industryType+"</ns1:IndustryType> " +
                        // "      <ns1:InternetTransactionData> " +
                        // "        <ns1:IpAddress>"+dnsIpAddress+"</ns1:IpAddress> " +
                        // "      </ns1:InternetTransactionData> " +
                        "     <ns1:InvoiceNumber>699203</ns1:InvoiceNumber> " +
                        "     <ns1:OrderNumber>"+orderNumber+"</ns1:OrderNumber> " +

                        //////////////////
                        "     <ns1:IsPartialShipment>false</ns1:IsPartialShipment> " +
                        "     <ns1:SignatureCaptured>false</ns1:SignatureCaptured> " +
                        "     <ns1:IsQuasiCash>false</ns1:IsQuasiCash> " +
                        /////////////////

                        "    </ns1:TransactionData> " +
                        "   </transaction> " +
                        "   <applicationProfileId>"+applicationProfileId+"</applicationProfileId> " +
                        "   <merchantProfileId>"+merchantID+"</merchantProfileId>" +
                        "   <workflowId>"+workflowId+"</workflowId> " +
                        "  </"+transaction+"> " +
                        " </SOAP-ENV:Body> " +
                        "</SOAP-ENV:Envelope> " ;

        //add reuqest header
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        con.setRequestProperty("Content-Type","text/xml; charset=UTF-8");
        con.setRequestProperty("Host", host);
        con.setRequestProperty("Content-Length", ""+Integer.toString(urlText.getBytes().length) );
        con.setRequestProperty("SOAPAction", soapAction);
        con.setDoOutput(true);

        // prepare the message
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(urlText);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();

        System.out.println("Sending 'POST' request to URL : " + url);
        System.out.println("Response Code : " + responseCode + "\n");

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        // close connection
        con.disconnect();

        String str = response.toString();
        processResponse(str);

        printMap(responsePars);
    } //sendPostTokenization


    private void sendPostCapture() throws Exception {
        String url = "https://"+host+"/2.0.18/Txn";
        // String url = "https://api.cert.nabcommerce.com/2.0.18/Txn";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        String soapAction =  "http://schemas.ipcommerce.com/CWS/v2.0/TransactionProcessing/ICwsTransactionProcessing/Capture";
        String sessionToken = responsePars.get("SignOnWithTokenResult");
        // guid = responsePars.get("AUTH_GUID");
        System.out.println(guid);

        String urlText =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
                        "<SOAP-ENV:Envelope " +
                        " xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                        " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"> " +
                        " <SOAP-ENV:Body> " +
                        "  <Capture " +
                        "    xmlns=\"http://schemas.ipcommerce.com/CWS/v2.0/TransactionProcessing\"> " +
                        "   <sessionToken>"+sessionToken+"</sessionToken> " +
                        "   <differenceData " +
                        "    xmlns:ns1=\"http://schemas.ipcommerce.com/CWS/v2.0/Transactions/Bankcard\" " +
                        "     xsi:type=\"ns1:BankcardCapture\"> " +
                        "    <ns2:TransactionId xmlns:ns2=\"http://schemas.ipcommerce.com/CWS/v2.0/Transactions\">"+guid+"</ns2:TransactionId> " +
                        "    <ns1:Amount>"+amount+"</ns1:Amount> " +
                        "   </differenceData> " +
                        "   <applicationProfileId>"+applicationProfileId+"</applicationProfileId> " +
                        "   <workflowId>"+workflowId+"</workflowId> " +
                        "  </Capture> " +
                        " </SOAP-ENV:Body> " +
                        "</SOAP-ENV:Envelope> " ;

        //add reuqest header
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        con.setRequestProperty("Content-Type","text/xml; charset=UTF-8");
        con.setRequestProperty("Host", host);
        con.setRequestProperty("Content-Length", ""+Integer.toString(urlText.getBytes().length) );
        con.setRequestProperty("SOAPAction", soapAction);
        con.setDoOutput(true);

        // prepare the message
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(urlText);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();

        System.out.println("Sending 'POST' request to URL : " + url);
        System.out.println("Response Code : " + responseCode + "\n");

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        // close connection
        con.disconnect();

        String str = response.toString();
        processResponse(str);

        printMap(responsePars);
    } //sendPostCapture

    /* ********************** ADJUST / ReturById : will partially refund    ********************************/
    private void sendPostReturnById() throws Exception {
        String url = "https://"+host+"/2.0.18/Txn";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        String soapAction =  "http://schemas.ipcommerce.com/CWS/v2.0/TransactionProcessing/ICwsTransactionProcessing/ReturnById";
        String sessionToken = responsePars.get("SignOnWithTokenResult");
        String urlText =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
                        "<SOAP-ENV:Body>" +
                        "  <ReturnById xmlns=\"http://schemas.ipcommerce.com/CWS/v2.0/TransactionProcessing\">" +
                        "   <sessionToken>"+sessionToken+"</sessionToken>" +
                        "   <differenceData xsi:type=\"ns1:BankcardReturn\" xmlns:ns1=\"http://schemas.ipcommerce.com/CWS/v2.0/Transactions/Bankcard\">" +
                        "    <ns2:TransactionId xmlns:ns2=\"http://schemas.ipcommerce.com/CWS/v2.0/Transactions\">"+guid+"</ns2:TransactionId>" +
                        // "    <ns3:Addendum xsi:nil=\"true\" xmlns:ns3=\"http://schemas.ipcommerce.com/CWS/v2.0/Transactions\"/>" +
                        // "    <ns4:TransactionDateTime xsi:nil=\"true\" xmlns:ns4=\"http://schemas.ipcommerce.com/CWS/v2.0/Transactions\"/>" +
                        "   <ns1:Amount>"+amount+"</ns1:Amount>" +
                        "    <ns1:TenderData xsi:nil=\"true\"/>" +
                        "   </differenceData>" +
                        "   <applicationProfileId>"+applicationProfileId+"</applicationProfileId>" +
                        "   <workflowId>"+workflowId+"</workflowId>" +
                        "  </ReturnById>" +
                        "</SOAP-ENV:Body>" +
                        "</SOAP-ENV:Envelope>";

        //add reuqest header
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        con.setRequestProperty("Content-Type","text/xml; charset=UTF-8");
        con.setRequestProperty("Host", host);
        con.setRequestProperty("Content-Length", ""+Integer.toString(urlText.getBytes().length) );
        con.setRequestProperty("SOAPAction", soapAction);
        con.setDoOutput(true);

        // prepare the message
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        // wr.write(urlText.getBytes());
        wr.writeBytes(urlText);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();

        System.out.println("Sending 'POST' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);
        // System.out.println("URL text : " + urlText);

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        // close connection
        con.disconnect();

        String str = response.toString();
        processResponse(str);

        printMap(responsePars);
    } //sendPostReturnById

    private void sendPostVoid() throws Exception {
        String url = "https://"+host+"/2.0.18/Txn";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        String soapAction =  "http://schemas.ipcommerce.com/CWS/v2.0/TransactionProcessing/ICwsTransactionProcessing/Undo";
        String sessionToken = responsePars.get("SignOnWithTokenResult");
        // System.out.println("SESSION TOKEN: " + sessionToken);
        // guid = responsePars.get("AUTH_GUID");
        // System.out.println("GUID     : " + guid);

        String urlText =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
                        "<SOAP-ENV:Envelope " +
                        " xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                        " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"> " +
                        " <SOAP-ENV:Body> " +
                        "  <Undo " +
                        "    xmlns=\"http://schemas.ipcommerce.com/CWS/v2.0/TransactionProcessing\"> " +
                        "   <sessionToken>"+sessionToken+"</sessionToken> " +
                        "   <differenceData " +
                        "    xmlns:ns1=\"http://schemas.ipcommerce.com/CWS/v2.0/Transactions/Bankcard\" " +
                        "       xsi:type=\"ns1:BankcardUndo\"> " +
                        "    <ns2:TransactionId " +
                        "       xmlns:ns2=\"http://schemas.ipcommerce.com/CWS/v2.0/Transactions\">"+guid+"</ns2:TransactionId> " +
                        "    <ns3:Addendum " +
                        "       xmlns:ns3=\"http://schemas.ipcommerce.com/CWS/v2.0/Transactions\" " +
                        "       xsi:nil=\"true\"/> " +
                        "    <ns1:TenderData xsi:nil=\"true\"/> " +
                        "   </differenceData> " +
                        "   <applicationProfileId>"+applicationProfileId+"</applicationProfileId> " +
                        "   <workflowId>"+workflowId+"</workflowId> " +
                        "  </Undo> " +
                        " </SOAP-ENV:Body> " +
                        "</SOAP-ENV:Envelope> " ;

        //add reuqest header
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        con.setRequestProperty("Content-Type","text/xml; charset=UTF-8");
        con.setRequestProperty("Host", host);
        con.setRequestProperty("Content-Length", ""+Integer.toString(urlText.getBytes().length) );
        con.setRequestProperty("SOAPAction", soapAction);
        con.setDoOutput(true);

        // prepare the message
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        // wr.write(urlText.getBytes());
        wr.writeBytes(urlText);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();

        System.out.println("Sending 'POST' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);
        // System.out.println("URL text : " + urlText);

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        // close connection
        con.disconnect();

        String str = response.toString();
        processResponse(str);

        printMap(responsePars);
    } //sendPostVoid





} //NabCardHttpConnection
