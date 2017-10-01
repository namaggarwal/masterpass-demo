package io.naman.masterpass.demo.controller;

import com.mastercard.masterpass.merchant.CheckoutApi;
import com.mastercard.masterpass.merchant.ExpressCheckoutApi;
import com.mastercard.masterpass.merchant.MerchantInitializationApi;
import com.mastercard.masterpass.merchant.PrecheckoutDataApi;
import com.mastercard.masterpass.merchant.model.*;
import com.mastercard.sdk.core.exceptions.SDKErrorResponseException;
import com.mastercard.sdk.core.models.AccessTokenResponse;
import com.mastercard.sdk.core.models.RequestTokenResponse;
import com.mastercard.sdk.core.services.AccessTokenApi;
import com.mastercard.sdk.core.services.RequestTokenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by e057964 on 26/9/17.
 */

@Controller
public class WebController extends BaseController {

    @Value("${masterpass.merchant.checkoutId}")
    String merchantCheckoutId;

    @RequestMapping("/")
    ModelAndView home(HttpSession httpSession) {

        ModelAndView mv = new ModelAndView("home");
        return mv;
    }


    @RequestMapping("/checkout")
    String checkout(@RequestParam("mpstatus") String status, @RequestParam(value = "oauth_token", required = false) String oauthToken,
                          @RequestParam(value = "oauth_verifier", required = false) String oauthVerifier,
                          @RequestParam(value = "checkout_resource_url", required = false) String checkoutResourceUrl, HttpSession httpSession) {

        if (status.equals("success")) {

            String requestToken = httpSession.getAttribute("request_token").toString();
            System.out.println("Request token is " + requestToken);

            AccessTokenResponse accessTokenResponse = AccessTokenApi.create(oauthToken, oauthVerifier);
            String accessToken = accessTokenResponse.getOauthToken();


            int startIndex = checkoutResourceUrl.lastIndexOf('/') + 1;
            int endIndex = checkoutResourceUrl.indexOf('?') != -1 ?
                    checkoutResourceUrl.indexOf('?')
                    : checkoutResourceUrl.length();
            String checkoutId = checkoutResourceUrl.substring(startIndex, endIndex);

            Checkout checkout = CheckoutApi.show(checkoutId, accessToken);

            ShippingAddress shippingAddress = checkout.getShippingAddress();
            String transactionId = checkout.getTransactionId();



            httpSession.setAttribute("orderNumber", transactionId);
            httpSession.setAttribute("shippingAddress",shippingAddress);
        }



        return "redirect:/success";
    }


    @RequestMapping("/pair")
    ModelAndView pair(HttpSession httpSession) {

        ModelAndView mv = new ModelAndView("pair");
        mv.addObject("checkout_id",merchantCheckoutId);

        String callback_url = "http://localhost:8080/pairSuccess";
        String pairing_request_token = null;

        try {
            RequestTokenResponse requestTokenResponse = RequestTokenApi.create(callback_url);
            pairing_request_token = requestTokenResponse.getOauthToken();

            System.out.println("Pairing Request token is" + pairing_request_token);

            mv.addObject("pairing_request_token", pairing_request_token);
            httpSession.setAttribute("pairing_request_token", pairing_request_token);

        } catch (SDKErrorResponseException e) {

            System.out.println("Error " + e.getMessage() + e.getErrorResponse());
        }

        try {
            MerchantInitializationRequest merchantInitializationRequest = new MerchantInitializationRequest()
                    .originUrl("http://localhost:8080")
                    .oAuthToken(pairing_request_token);

            MerchantInitializationResponse merchantInitializationResponse = MerchantInitializationApi.create(merchantInitializationRequest);
        } catch (Exception e) {

            System.out.println("Error " + e.getMessage());
        }



        return mv;
    }


    @RequestMapping("/pairSuccess")
    ModelAndView pairSuccess(@RequestParam("mpstatus") String status, @RequestParam(value = "pairing_token", required = false) String pairingToken,
                             @RequestParam(value = "pairing_verifier", required = false) String pairingVerifier,HttpSession httpSession) {


        ModelAndView mv = new ModelAndView("pairSuccess");

        if(status.equals("success")){

            AccessTokenResponse accessTokenResponse = AccessTokenApi.create(pairingToken, pairingVerifier);
            String longAccessToken = accessTokenResponse.getOauthToken(); // store for future requests
            mv.addObject("longAccessToken",longAccessToken);
        }

        return mv;
    }

    @RequestMapping("/standardCheckout")
    ModelAndView standardCheckout(HttpSession httpSession) {

        String callback_url = "http://localhost:8080/checkout";
        String request_token = null;
        ModelAndView mv = new ModelAndView("standardCheckout");
        mv.addObject("checkout_id",merchantCheckoutId);
        try {
            RequestTokenResponse requestTokenResponse = RequestTokenApi.create(callback_url);
            request_token = requestTokenResponse.getOauthToken();
            System.out.println("Request token is" + request_token);

            mv.addObject("request_token", request_token);

            httpSession.setAttribute("request_token", request_token);

        } catch (SDKErrorResponseException e) {

            System.out.println("Error " + e.getMessage() + e.getErrorResponse());
        }

        try {
            MerchantInitializationRequest merchantInitializationRequest = new MerchantInitializationRequest()
                    .originUrl("https://localhost:8080/standardCheckout")
                    .oAuthToken(request_token);

            MerchantInitializationResponse merchantInitializationResponse = MerchantInitializationApi.create(merchantInitializationRequest);



        } catch (Exception e) {

            System.out.println("Error " + e.getMessage());
        }


        return mv;


    }

    @RequestMapping("/success")
    ModelAndView success(HttpSession httpSession) {

        ModelAndView mv = new ModelAndView("success");
        mv.addObject("transactionId",httpSession.getAttribute("orderNumber"));

        List<String> shippingAddress = new ArrayList<>();

        ShippingAddress shippingAdd = (ShippingAddress) httpSession.getAttribute("shippingAddress");

        if(shippingAdd != null){

            if(shippingAdd.getLine1() != null){

                shippingAddress.add(shippingAdd.getLine1());

            }
            if(shippingAdd.getLine2() != null){

                shippingAddress.add(shippingAdd.getLine2());

            }
            if(shippingAdd.getLine3() != null){

                shippingAddress.add(shippingAdd.getLine3());

            }

            if(shippingAdd.getCity() != null){

                shippingAddress.add(shippingAdd.getCity());


            }

            if(shippingAdd.getCountry() != null){

                shippingAddress.add(shippingAdd.getCountry());
            }

            if(shippingAdd.getPostalCode()!=null){
                shippingAddress.add(shippingAdd.getPostalCode());
            }

        }


        mv.addObject("shippingAddress",shippingAddress);

        return mv;
    }


    @RequestMapping("/expressCheckout")
    String expressCheckout(HttpSession httpSession) {

        //Replace this by your long access token
        String longAccessToken = "";

        //Create an instance of PrecheckoutDataRequest
        PrecheckoutDataRequest preCheckoutDataRequest = new PrecheckoutDataRequest()
                .pairingDataTypes(new PairingDataTypes()
                        .pairingDataType(new PairingDataType()
                                .type(PairingDataType.TypeEnum.CARD))

                        .pairingDataType(new PairingDataType()
                                .type(PairingDataType.TypeEnum.ADDRESS))

                        .pairingDataType(new PairingDataType()
                                .type(PairingDataType.TypeEnum.PROFILE)));

        //Call the PrecheckoutDataApi with required parameters
        PrecheckoutDataResponse preCheckoutDataResponse = PrecheckoutDataApi.create(longAccessToken, preCheckoutDataRequest);


        //Get a cardID and a shipping Address ID from precheckoutDataResponse
        String preCheckoutId = preCheckoutDataResponse.getPrecheckoutData().getPrecheckoutTransactionId();
        String cardId = preCheckoutDataResponse.getPrecheckoutData().getCards().getCard().get(0).getCardId();
        String shippingAddressId = preCheckoutDataResponse.getPrecheckoutData().getShippingAddresses().getShippingAddress().get(0).getAddressId();

        ExpressCheckoutRequest expressCheckoutRequest = new ExpressCheckoutRequest()
                .cardId(cardId)
                .originUrl("http://localhost:8080")
                .orderAmount((long)1000)
                .advancedCheckoutOverride(false)
                .merchantCheckoutId(merchantCheckoutId)
                .shippingAddressId(shippingAddressId)
                .currencyCode("USD")
                .precheckoutTransactionId(preCheckoutId);


        //Call the ExpressCheckoutApi with required parameters
        ExpressCheckoutResponse expressCheckoutResponse = ExpressCheckoutApi.create(longAccessToken, expressCheckoutRequest);

        Checkout checkout = expressCheckoutResponse.getCheckout();

        ShippingAddress shippingAddress = checkout.getShippingAddress();
        String transactionId = checkout.getTransactionId();



        httpSession.setAttribute("orderNumber", transactionId);
        httpSession.setAttribute("shippingAddress",shippingAddress);

        return "redirect:/success";
    }
}
