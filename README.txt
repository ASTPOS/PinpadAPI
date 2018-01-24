

=== PINPAD Module for Android ==== 

This module is meant to work with NAB or PAX terminals ONLY, since in the code are used their API handlers, methods, etc. The same API's are used in the Java modules.

-- FLOW ------------------------------------------------------------------------------
                 SettingsActivity
                /
HomeActivity  ->
                \
                 \       PaxPinpacActivity                         PaxPinpacActivity
                  \     /                 \                       /
                   ---->                   -> SignatureActivity ->
                        \                 /                       \
                         NabPinpadActivity                         NabPinpadActivity
--------------------------------------------------------------------------------------



  
-- HomeMainActivity --

The control page. Ideally this page will be replaced by the app controlled page. However, here are the complete methods how to call each of the child pages. 
-----------------------


-- SettingsActivity --

No extra parameters are required in order to start the page.
Here the user can customize and setup all required information in order to use the module:

*Merchant ID: not required for NAB and PAX current solution. It maybe used for future development.

*Pinpad IP Addr: a valid IP address of the pinpad(terminal) device

*Processor: could be PAX or NAB, which predicts if _PinpadActivity will start as PAX_ or Nab_PinpadActivity.

* Debit/Credit: determines what type of card will be used.

*Transaction Name: determines the type of transaction that will be performed (only Sale and PreAuth are enabled, since the rest will be handled by the POS for the moment).

*Tip Threshold xAmt: determines how many times the Tip amount can overcome the Sale Amount. Example: if sale amount is 5.00 and the Tip Threshold is 2, Tip Amount cannot be greater than 10.00.

* Ping Pinpad: initiates a check if the entered IP address is reachable from the device.
-----------------------


-- Nab_ and PAX_ PinpadActivity --
Both classes have similar behavior. Extra parameters required:

*TRANS_AMOUNT (String): used to lock the sale amount. This way customers will not temper any values but Tip Amount
 
*FROM_SIGN (boolean): flag if previous activity was SigningActivity, so the view can be updated 
-----------------------


