/*
 * Copyright (C) 2011 The Android Open Source Project
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

/**
 * This class defines a collection of waning codes and corresponding
 * descriptions to be carried by Warning header.
 */
public class SipWarning {
    /*
     * SIP warning codes registered to IANA.
     * [cf] http://www.iana.org/assignments/sip-parameters
     */
    public static final int INCOMPATIBLE_NETWORK_PROTOCOL = 300;
    public static final int INCOMPATIBLE_NETWORK_ADDRESS_FORMATS = 301;
    public static final int INCOMPATIBLE_TRANSPORT_PROTOCOL = 302;
    public static final int INCOMPATIBLE_BANDWIDTH_UNITS = 303;
    public static final int MEDIA_TYPE_NOT_AVAILABLE = 304;
    public static final int INCOMPATIBLE_MEDIA_FORMAT = 305;
    public static final int ATTRIBUTE_NOT_UNDERSTOOD = 306;
    public static final int SESSION_DESCRIPTION_PARAMETER_NOT_UNDERSTOOD = 307;
    public static final int MULTICAST_NOT_AVAILABLE = 330;
    public static final int UNICAST_NOT_AVAILABLE = 331;
    public static final int INSUFFICIENT_BANDWIDTH = 370;
    public static final int SIPS_NOT_ALLOWED = 380;
    public static final int SIPS_REQUIRED = 381;
    public static final int MISCELLANEOUS_WARNING = 399;

    public static String toString(int code) {
        switch (code) {
        case INCOMPATIBLE_NETWORK_PROTOCOL:
            return "Incompatible network protocol";
        case INCOMPATIBLE_NETWORK_ADDRESS_FORMATS:
            return "Incompatible network address formats";
        case INCOMPATIBLE_TRANSPORT_PROTOCOL:
            return "Incompatible transport protocol";
        case INCOMPATIBLE_BANDWIDTH_UNITS:
            return "Incompatible bandwidth units";
        case MEDIA_TYPE_NOT_AVAILABLE:
            return "Media type not available";
        case INCOMPATIBLE_MEDIA_FORMAT:
            return "Incompatible media format";
        case ATTRIBUTE_NOT_UNDERSTOOD:
            return "Attribute not understood";
        case SESSION_DESCRIPTION_PARAMETER_NOT_UNDERSTOOD:
            return "Session description parameter not understood";
        case MULTICAST_NOT_AVAILABLE:
            return "Multicast not available";
        case UNICAST_NOT_AVAILABLE:
            return "Unicast not available";
        case INSUFFICIENT_BANDWIDTH:
            return "Insufficient bandwidth";
        case SIPS_NOT_ALLOWED:
            return "SIPS Not Allowed";
        case SIPS_REQUIRED:
            return "SIPS Required";
        case MISCELLANEOUS_WARNING:
            return null; /* User should specify arbitrary information */
        default:
            return "Unknown warning code";
        }
    }
}
