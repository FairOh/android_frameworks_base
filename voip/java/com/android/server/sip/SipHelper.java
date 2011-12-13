/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.sip;

import gov.nist.javax.sip.SipStackExt;
import gov.nist.javax.sip.clientauthutils.AccountManager;
import gov.nist.javax.sip.clientauthutils.AuthenticationHelper;
import gov.nist.javax.sip.header.extensions.ReferencesHeader;
import gov.nist.javax.sip.header.extensions.ReferredByHeader;
import gov.nist.javax.sip.header.extensions.ReplacesHeader;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;

import android.net.sip.SipProfile;
import android.util.Log;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogTerminatedEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.PeerUnavailableException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.Transaction;
import javax.sip.TransactionAlreadyExistsException;
import javax.sip.TransactionDoesNotExistException;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.TransactionUnavailableException;
import javax.sip.TransactionState;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.AllowHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.ReasonHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.UserAgentHeader;
import javax.sip.header.ViaHeader;
import javax.sip.header.WarningHeader;
import javax.sip.message.Message;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

/**
 * Helper class for holding SIP stack related classes and for various low-level
 * SIP tasks like sending messages.
 */
class SipHelper {
    private static final String TAG = SipHelper.class.getSimpleName();
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_PING = false;

    private SipStack mSipStack;
    private SipProvider mSipProvider;
    private AddressFactory mAddressFactory;
    private HeaderFactory mHeaderFactory;
    private MessageFactory mMessageFactory;

    /*-----------------------------------------------------------------*
     * Constructor
     *-----------------------------------------------------------------*/

    public SipHelper(SipStack sipStack, SipProvider sipProvider)
            throws PeerUnavailableException {
        mSipStack = sipStack;
        mSipProvider = sipProvider;

        SipFactory sipFactory = SipFactory.getInstance();
        mAddressFactory = sipFactory.createAddressFactory();
        mHeaderFactory = sipFactory.createHeaderFactory();
        mMessageFactory = sipFactory.createMessageFactory();
    }

    /*-----------------------------------------------------------------*
     * Create various headers
     *-----------------------------------------------------------------*/

    private AllowHeader createAllowHeader()
            throws ParseException {
        String methods = "ACK,BYE,CANCEL,INVITE,OPTIONS,REGISTER,NOTIFY,REFER";
        return mHeaderFactory.createAllowHeader(methods);
    }

    private WarningHeader createWarningHeader(
        String agentName, int warningCode, String warningMessage)
            throws ParseException, InvalidArgumentException {
        return mHeaderFactory.createWarningHeader(
                    agentName, warningCode, warningMessage);
    }

    private ReasonHeader createReasonHeader(
        String protocol, int cause, String text)
            throws ParseException, InvalidArgumentException {
        return mHeaderFactory.createReasonHeader(protocol, cause, text);
    }

    private UserAgentHeader createUserAgentHeader(List product)
            throws ParseException {
        List list = product;
        if (list == null) {
            list = new LinkedList();
            list.add("SIPAUA/0.1.001");
        }
        return mHeaderFactory.createUserAgentHeader(list);
    }

    private FromHeader createFromHeader(SipProfile profile, String tag)
            throws ParseException {
        return mHeaderFactory.createFromHeader(profile.getSipAddress(), tag);
    }

    private ToHeader createToHeader(SipProfile profile) throws ParseException {
        return createToHeader(profile, null);
    }

    private ToHeader createToHeader(SipProfile profile, String tag)
            throws ParseException {
        return mHeaderFactory.createToHeader(profile.getSipAddress(), tag);
    }

    private CallIdHeader createCallIdHeader() {
        return mSipProvider.getNewCallId();
    }

    private CSeqHeader createCSeqHeader(String method)
            throws ParseException, InvalidArgumentException {
        long sequence = (long) (Math.random() * 10000);
        return mHeaderFactory.createCSeqHeader(sequence, method);
    }

    private MaxForwardsHeader createMaxForwardsHeader()
            throws InvalidArgumentException {
        return mHeaderFactory.createMaxForwardsHeader(70);
    }

    private MaxForwardsHeader createMaxForwardsHeader(int max)
            throws InvalidArgumentException {
        return mHeaderFactory.createMaxForwardsHeader(max);
    }

    private ListeningPoint getListeningPoint() throws SipException {
        ListeningPoint lp = mSipProvider.getListeningPoint(ListeningPoint.UDP);
        if (lp == null) lp = mSipProvider.getListeningPoint(ListeningPoint.TCP);
        if (lp == null) {
            ListeningPoint[] lps = mSipProvider.getListeningPoints();
            if ((lps != null) && (lps.length > 0)) lp = lps[0];
        }
        if (lp == null) {
            throw new SipException("no listening point is available");
        }
        return lp;
    }

    private List<ViaHeader> createViaHeaders()
            throws ParseException, SipException {
        List<ViaHeader> viaHeaders = new ArrayList<ViaHeader>(1);
        ListeningPoint lp = getListeningPoint();
        ViaHeader viaHeader = mHeaderFactory.createViaHeader(lp.getIPAddress(),
                lp.getPort(), lp.getTransport(), null);
        viaHeader.setRPort();
        viaHeaders.add(viaHeader);
        return viaHeaders;
    }

    private ContactHeader createContactHeader(SipProfile profile)
            throws ParseException, SipException {
        return createContactHeader(profile, null, 0);
    }

    private ContactHeader createContactHeader(SipProfile profile,
            String ip, int port) throws ParseException,
            SipException {
        SipURI contactURI = (ip == null)
                ? createSipUri(profile.getUserName(), profile.getProtocol(),
                        getListeningPoint())
                : createSipUri(profile.getUserName(), profile.getProtocol(),
                        ip, port);

        Address contactAddress = mAddressFactory.createAddress(contactURI);
        contactAddress.setDisplayName(profile.getDisplayName());

        return mHeaderFactory.createContactHeader(contactAddress);
    }

    private ContactHeader createWildcardContactHeader() {
        ContactHeader contactHeader  = mHeaderFactory.createContactHeader();
        contactHeader.setWildCard();
        return contactHeader;
    }

    private SipURI createSipUri(String username, String transport,
            ListeningPoint lp) throws ParseException {
        return createSipUri(username, transport, lp.getIPAddress(), lp.getPort());
    }

    private SipURI createSipUri(String username, String transport,
            String ip, int port) throws ParseException {
        SipURI uri = mAddressFactory.createSipURI(username, ip);
        try {
            uri.setPort(port);
            uri.setTransportParam(transport);
        } catch (InvalidArgumentException e) {
            throw new RuntimeException(e);
        }
        return uri;
    }

    /*-----------------------------------------------------------------*
     * Sending request messages
     *-----------------------------------------------------------------*/

    public ClientTransaction sendOptions(SipProfile caller, SipProfile callee,
            String tag) throws SipException {
        try {
            Request request = (caller == callee)
                    ? createRequest(Request.OPTIONS, caller, tag)
                    : createRequest(Request.OPTIONS, caller, callee, tag);

            ClientTransaction clientTransaction =
                    mSipProvider.getNewClientTransaction(request);
            clientTransaction.sendRequest();
            return clientTransaction;
        } catch (ParseException e) {
            /* this.createRequest() */
            throw new SipException("sendOptions()", e);
        } catch (TransactionUnavailableException e) {
            /* Provider.getNewClientTransaction() */
            throw new SipException("sendOptions()", e);
        }
    }

    public ClientTransaction sendRegister(SipProfile userProfile, String tag,
            int expiry) throws SipException {
        try {
            Request request = createRequest(Request.REGISTER, userProfile, tag);
            if (expiry == 0) {
                // remove all previous registrations by wildcard
                // rfc3261#section-10.2.2
                request.addHeader(createWildcardContactHeader());
            } else {
                request.addHeader(createContactHeader(userProfile));
            }
            request.addHeader(mHeaderFactory.createExpiresHeader(expiry));

            ClientTransaction clientTransaction =
                    mSipProvider.getNewClientTransaction(request);
            clientTransaction.sendRequest();
            return clientTransaction;
        } catch (ParseException e) {
            /* this.createRequest() */
            throw new SipException("sendRegister()", e);
        } catch (TransactionUnavailableException e) {
            /* Provider.getNewClientTransaction() */
            throw new SipException("sendRegister()", e);
        }
    }

    public ClientTransaction handleChallenge(ResponseEvent responseEvent,
            AccountManager accountManager) throws SipException {
        AuthenticationHelper authenticationHelper =
                ((SipStackExt) mSipStack).getAuthenticationHelper(
                        accountManager, mHeaderFactory);
        ClientTransaction tid = responseEvent.getClientTransaction();
        ClientTransaction ct = authenticationHelper.handleChallenge(
                responseEvent.getResponse(), tid, mSipProvider, 5);
        if (DEBUG) Log.d(TAG, "send request with challenge response: "
                + ct.getRequest());
        ct.sendRequest();
        return ct;
    }

    public ClientTransaction sendInvite(SipProfile caller, SipProfile callee,
            String sessionDescription, String tag, ReferredByHeader referredBy,
            String replaces) throws SipException {
        try {
            Request request = createRequest(Request.INVITE, caller, callee, tag);
            if (referredBy != null) request.addHeader(referredBy);
            if (replaces != null) {
                request.addHeader(mHeaderFactory.createHeader(
                        ReplacesHeader.NAME, replaces));
            }
            if (sessionDescription != null) {
                setSdpMessage((Message)request, sessionDescription);
            }
            ClientTransaction clientTransaction =
                    mSipProvider.getNewClientTransaction(request);
            if (DEBUG) Log.d(TAG, "send INVITE: " + request);
            clientTransaction.sendRequest();
            return clientTransaction;
        } catch (ParseException e) {
            /* this.createRequest() */
            throw new SipException("sendInvite()", e);
        } catch (TransactionUnavailableException e) {
            /* Provider.getNewClientTransaction() */
            throw new SipException("sendInvite()", e);
        }
    }

    public ClientTransaction sendReinvite(SipProfile userProfile,
            Dialog dialog, String sessionDescription) throws SipException {
        try {
            Request request = dialog.createRequest(Request.INVITE);
            if (sessionDescription != null) {
                setSdpMessage((Message)request, sessionDescription);
            }

            Header userAgentHeader = mHeaderFactory.createHeader("User-Agent",
                    userProfile.getUserAgent());
            request.addHeader(userAgentHeader);

            // Adding rport argument in the request could fix some SIP servers
            // in resolving the initiator's NAT port mapping for relaying the
            // response message from the other end.

            ViaHeader viaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
            if (viaHeader != null) viaHeader.setRPort();

            ClientTransaction clientTransaction =
                    mSipProvider.getNewClientTransaction(request);
            if (DEBUG) Log.d(TAG, "send RE-INVITE: " + request);
            dialog.sendRequest(clientTransaction);
            return clientTransaction;
        } catch (TransactionUnavailableException e) {
            /* Provider.getNewClientTransaction() */
            throw new SipException("sendReinvite()", e);
        } catch (TransactionDoesNotExistException e) {
            /* Dialog.sendRequest() */
            throw new SipException("sendReinvite()", e);
        }
    }

    /**
     * @param event the INVITE ACK request event
     */
    public void sendInviteAck(ResponseEvent event, Dialog dialog)
            throws SipException {
        try {
            Response response = event.getResponse();
            long cseq = ((CSeqHeader) response.getHeader(CSeqHeader.NAME))
                    .getSeqNumber();
            Request request = dialog.createAck(cseq);
            if (DEBUG) Log.d(TAG, "send ACK: " + request);
            dialog.sendAck(request);
        } catch (InvalidArgumentException e) {
            /* Dialog.createAck() */
            throw new SipException("sendInviteAck()", e);
        }
    }

    public void sendBye(Dialog dialog, int statusCode) throws SipException {
        try {
            Request request = dialog.createRequest(Request.BYE);
            if (statusCode > 0) {
                /* Add Reason header (RFC3326) for better UI */
                try {
                    String reasonPhrase =
                        SIPResponse.getReasonPhrase(statusCode);
                    ReasonHeader reasonHeader =
                        createReasonHeader("SIP", statusCode, reasonPhrase);
                    request.addHeader(reasonHeader);
                } catch (ParseException e) {
                    Log.w(TAG, "sendBye(): createReasonHeader: ", e);
                } catch (InvalidArgumentException e) {
                    Log.w(TAG, "sendBye(): createReasonHeader: ", e);
                }
            }
            if (DEBUG) Log.d(TAG, "send BYE: " + request);
            dialog.sendRequest(
                mSipProvider.getNewClientTransaction(request));
        } catch (TransactionDoesNotExistException e) {
            /* Dialog.sendRequest() */
            throw new SipException("sendBye()", e);
        }
    }

    public void sendCancel(ClientTransaction inviteTransaction, int statusCode)
            throws SipException {
        try {
            Request request = inviteTransaction.createCancel();
            if (statusCode > 0) {
                /* Add Reason header (RFC3326) for better UI */
                try {
                    String reasonPhrase =
                        SIPResponse.getReasonPhrase(statusCode);
                    ReasonHeader reasonHeader =
                        createReasonHeader("SIP", statusCode, reasonPhrase);
                    request.addHeader(reasonHeader);
                } catch (ParseException e) {
                    Log.w(TAG, "sendCancel(): createReasonHeader: ", e);
                } catch (InvalidArgumentException e) {
                    Log.w(TAG, "sendCancel(): createReasonHeader: ", e);
                }
            }
            if (DEBUG) Log.d(TAG, "send CANCEL: " + request);
            ClientTransaction clientTransaction =
                    mSipProvider.getNewClientTransaction(request);
            clientTransaction.sendRequest();
        } catch (TransactionUnavailableException e) {
            /* Provider.getNewClientTransaction() */
            throw new SipException("sendCancel()", e);
        }
    }

    public void sendReferNotify(Dialog dialog, String content)
            throws SipException {
        try {
            Request request = dialog.createRequest(Request.NOTIFY);
            request.addHeader(mHeaderFactory.createSubscriptionStateHeader(
                    "active;expires=60"));
            // set content here
            request.setContent(content,
                    mHeaderFactory.createContentTypeHeader(
                            "message", "sipfrag"));
            request.addHeader(mHeaderFactory.createEventHeader(
                    ReferencesHeader.REFER));
            if (DEBUG) Log.d(TAG, "send NOTIFY: " + request);
            dialog.sendRequest(mSipProvider.getNewClientTransaction(request));
        } catch (ParseException e) {
            /* HeaderFactory.createSubscriptionStateHeader() */
            throw new SipException("sendReferNotify()", e);
        } catch (TransactionUnavailableException e) {
            /* Provider.getNewClientTransaction() */
            throw new SipException("sendReferNotify()", e);
        } catch (TransactionDoesNotExistException e) {
            /* Dialog.sendRequest() */
            throw new SipException("sendReferNotify()", e);
        }
    }

    private Request createRequest(String requestType, SipProfile userProfile,
            String tag) throws ParseException, SipException {
        FromHeader fromHeader = createFromHeader(userProfile, tag);
        ToHeader toHeader = createToHeader(userProfile);

        String replaceStr = Pattern.quote(userProfile.getUserName() + "@");
        SipURI requestURI = mAddressFactory.createSipURI(
                userProfile.getUriString().replaceFirst(replaceStr, ""));

        List<ViaHeader> viaHeaders = createViaHeaders();
        CallIdHeader callIdHeader = createCallIdHeader();
        CSeqHeader cSeqHeader = createCSeqHeader(requestType);
        MaxForwardsHeader maxForwards = createMaxForwardsHeader();
        Request request = mMessageFactory.createRequest(requestURI,
                requestType, callIdHeader, cSeqHeader, fromHeader,
                toHeader, viaHeaders, maxForwards);
        Header userAgentHeader = createUserAgentHeader(null);
        request.addHeader(userAgentHeader);
        return request;
    }

    private Request createRequest(String requestType, SipProfile caller,
            SipProfile callee, String tag) throws ParseException, SipException {
        FromHeader fromHeader = createFromHeader(caller, tag);
        ToHeader toHeader = createToHeader(callee);
        SipURI requestURI = callee.getUri();
        List<ViaHeader> viaHeaders = createViaHeaders();
        CallIdHeader callIdHeader = createCallIdHeader();
        CSeqHeader cSeqHeader = createCSeqHeader(requestType);
        MaxForwardsHeader maxForwards = createMaxForwardsHeader();

        Request request = mMessageFactory.createRequest(requestURI,
                requestType, callIdHeader, cSeqHeader, fromHeader,
                toHeader, viaHeaders, maxForwards);

        request.addHeader(createContactHeader(caller));
        return request;
    }

    /*-----------------------------------------------------------------*
     * Sending response messages
     *-----------------------------------------------------------------*/

    public ServerTransaction getServerTransaction(RequestEvent event)
            throws SipException {
        ServerTransaction transaction = event.getServerTransaction();
        if (transaction == null) {
            Request request = event.getRequest();
            try {
                transaction = mSipProvider.getNewServerTransaction(request);
            } catch (TransactionAlreadyExistsException e) {
                throw new SipException("getServerTransaction()", e);
            } catch (TransactionUnavailableException e) {
                throw new SipException("getServerTransaction()", e);
            }
        }
        return transaction;
    }

    public void sendNotAcceptableHere(RequestEvent event,
            String agentName, int warningCode, String warningMessage)
            throws SipException {
        try {
            ArrayList<Header> customHeaders = new ArrayList<Header>();
            WarningHeader warningHeader =
                createWarningHeader(agentName, warningCode, warningMessage);
            customHeaders.add(warningHeader);

            sendResponse(event, Response.NOT_ACCEPTABLE_HERE, customHeaders);
        } catch (ParseException e) {
            throw new SipException("sendNotAcceptableHere()", e);
        } catch (InvalidArgumentException e) {
            throw new SipException("sendNotAcceptableHere()", e);
        }
    }

    /**
     * @param event the INVITE request event
     */
    public ServerTransaction sendRinging(RequestEvent event, String toTag)
            throws SipException {
        /*
         * Different from other cases which calls sendResponse(),
         * we need to return the ServerTransaction to the caller.
         */
        return sendResponse(event, Response.RINGING, null, null, toTag, null);
    }

    /**
     * @param event the INVITE request event
     */
    public void sendInviteOk(RequestEvent event,
            SipProfile localProfile, String sessionDescription,
            ServerTransaction inviteTransaction, String externalIp,
            int externalPort) throws SipException {
        if (inviteTransaction.getState() != TransactionState.COMPLETED) {
            try {
                ArrayList<Header> customHeaders = new ArrayList<Header>();
                ContactHeader contactHeader =
                    createContactHeader(
                        localProfile, externalIp, externalPort);
                customHeaders.add(contactHeader);

                sendResponse(event, Response.OK, customHeaders,
                    sessionDescription, null, inviteTransaction);
            } catch (ParseException e) {
                /* this.createContactHeader() */
                throw new SipException("sendInviteOk()", e);
            }
        } else {
            Log.w(TAG, "sendInviteOk(): transaction already completed");
        }
    }

    public void sendInviteBusyHere(SipProfile userProfile, RequestEvent event,
            ServerTransaction inviteTransaction) throws SipException {
        if (inviteTransaction.getState() != TransactionState.COMPLETED) {
            sendInviteResponse(event, Response.BUSY_HERE, inviteTransaction);
        } else {
            Log.w(TAG, "sendInviteBusyHere(): transaction already completed");
        }
    }

    public void sendInviteRequestTerminated(RequestEvent event,
            ServerTransaction inviteTransaction) throws SipException {
        sendInviteResponse(event, Response.REQUEST_TERMINATED,
            inviteTransaction);
    }

    public void sendInviteTimeout(RequestEvent event,
            ServerTransaction inviteTransaction) throws SipException {
        sendInviteResponse(event, Response.SERVER_TIMEOUT, inviteTransaction);
    }

    public void sendInviteResponse(RequestEvent event, int responseCode,
            ServerTransaction inviteTransaction) throws SipException {
        sendResponse(event, responseCode, null, null, null, inviteTransaction);
    }

    public void sendResponse(RequestEvent event, int responseCode)
            throws SipException {
        sendResponse(event, responseCode, null, null, null, null);
    }

    public void sendResponse(RequestEvent event, int responseCode,
            ArrayList<Header> customHeaders) throws SipException {
        sendResponse(event, responseCode, customHeaders, null, null, null);
    }

    public void sendResponse(RequestEvent event, int responseCode,
            String sessionDescription) throws SipException {
        sendResponse(event, responseCode, null, sessionDescription, null, null);
    }

    private ServerTransaction sendResponse(
            RequestEvent event,
            int responseCode,
            ArrayList<Header> customHeaders,
            String sessionDescription,
            String toTag,
            ServerTransaction serverTransaction)
            throws SipException {
        ServerTransaction transaction = null;
        try {
            /* Build the default response message */
            Request request = event.getRequest();
            Response response =
                mMessageFactory.createResponse(responseCode, request);

            /* Set Allow header if required to do so. */
            setAllowHeader(event, response);

            if (toTag != null) {
                ToHeader toHeader =
                    (ToHeader)response.getHeader(ToHeader.NAME);
                toHeader.setTag(toTag);
            }
            if (sessionDescription != null) {
                setSdpMessage((Message)response, sessionDescription);
            }
            if (customHeaders != null) {
                for (int i = 0, n = customHeaders.size(); i < n; i++) {
                    Header header = customHeaders.get(i);
                    response.setHeader(header);
                }
            }

            if (DEBUG && (!Request.OPTIONS.equals(request.getMethod())
                    || DEBUG_PING)) {
                Log.d(TAG, "send response: " + response);
            }

            if (serverTransaction != null) {
                transaction = serverTransaction;
            } else {
                transaction = getServerTransaction(event);
            }
            transaction.sendResponse(response);
        } catch (ParseException e) {
            /* MessageFactory.createResponse() */
            throw new SipException("sendResponse()", e);
        } catch (InvalidArgumentException e) {
            /* ServerTransaction.sendResponse() */
            throw new SipException("sendResponse()", e);
        }
        return transaction;
    }

    private void setAllowHeader(RequestEvent event, Response response)
            throws SipException {
        /*
         * We SHOULD set the Allow header depending on method type
         * and response code; See RFC3261 Table 2.
         */
        String method = event.getRequest().getMethod();
        if (method.equals(Request.ACK) || method.equals(Request.CANCEL)) {
            ; /* Not applicable */
        } else {
            try {
                switch (response.getStatusCode()) {
                case Response.METHOD_NOT_ALLOWED:
                    /*
                     * Excerpt from RFC3261, section 8.2.1:
                     *
                     * "The UAS MUST also add an Allow header field to
                     * the 405 (Method Not Allowed) response."
                     */
                    response.setHeader(createAllowHeader());
                    break;
                default:
                    if ((response.getStatusCode() / 100) == 2) {
                        response.setHeader(createAllowHeader());
                    }
                    break;
                }
            } catch (ParseException e) {
                throw new SipException("setAllowHeader()", e);
            }
        }
    }

    /*-----------------------------------------------------------------*
     * Call-ID handling
     *-----------------------------------------------------------------*/

    public static String getCallId(EventObject event) {
        if (event == null) return null;
        if (event instanceof RequestEvent) {
            return getCallId(((RequestEvent) event).getRequest());
        } else if (event instanceof ResponseEvent) {
            return getCallId(((ResponseEvent) event).getResponse());
        } else if (event instanceof DialogTerminatedEvent) {
            Dialog dialog = ((DialogTerminatedEvent) event).getDialog();
            return getCallId(((DialogTerminatedEvent) event).getDialog());
        } else if (event instanceof TransactionTerminatedEvent) {
            TransactionTerminatedEvent e = (TransactionTerminatedEvent) event;
            return getCallId(e.isServerTransaction()
                    ? e.getServerTransaction()
                    : e.getClientTransaction());
        } else {
            Object source = event.getSource();
            if (source instanceof Transaction) {
                return getCallId(((Transaction) source));
            } else if (source instanceof Dialog) {
                return getCallId((Dialog) source);
            }
        }
        return "";
    }

    public static String getCallId(Transaction transaction) {
        return ((transaction != null) ? getCallId(transaction.getRequest())
                                      : "");
    }

    private static String getCallId(Message message) {
        CallIdHeader callIdHeader =
                (CallIdHeader) message.getHeader(CallIdHeader.NAME);
        return callIdHeader.getCallId();
    }

    private static String getCallId(Dialog dialog) {
        return dialog.getCallId().getCallId();
    }

    /*-----------------------------------------------------------------*
     * Message body
     *-----------------------------------------------------------------*/

    private ContentTypeHeader createContentTypeHeader(
            String type, String subType) throws SipException {
        try {
            ContentTypeHeader contentTypeHeader =
                mHeaderFactory.createContentTypeHeader(type, subType);
            return contentTypeHeader;
        } catch (ParseException e) {
            throw new SipException("createContentTypeHeader()", e);
        }
    }

    private void setSdpMessage(Message message, String sessionDescription)
            throws SipException {
        try {
            ContentTypeHeader contentTypeHeader =
                createContentTypeHeader("application", "sdp");
            message.setContent(sessionDescription, contentTypeHeader);
        } catch (ParseException e) {
            throw new SipException("setSdpMessage()", e);
        }
    }
}
