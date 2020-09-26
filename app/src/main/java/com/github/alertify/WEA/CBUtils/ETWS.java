package com.github.alertify.WEA.CBUtils;

import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

public class ETWS {

    public static byte[]  hexStringToBytes(String s) {
        byte[] ret;
        if (s == null) return null;
        int sz = s.length();
        ret = new byte[sz/2];
        for (int i=0 ; i <sz ; i+=2) {
            ret[i/2] = (byte) ((hexCharToInt(s.charAt(i)) << 4)
                    | hexCharToInt(s.charAt(i+1)));
        }
        return ret;
    }

    public static int hexCharToInt(char c) {
        if (c >= '0' && c <= '9') return (c - '0');
        if (c >= 'A' && c <= 'F') return (c - 'A' + 10);
        if (c >= 'a' && c <= 'f') return (c - 'a' + 10);
        throw new RuntimeException ("invalid hex char '" + c + "'");
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static SmsCbMessage createFromPdu(Context context, byte[] pdu, int serialNumber, int category) {
        byte[][] pdus = new byte[1][];
        pdus[0] = pdu;
        return createFromPdus(context, pdus, serialNumber, category);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    static SmsCbMessage createFromPdus(Context context, byte[][] pdus, int serialNumber, int category) {
        try {
            for (byte[] pdu : pdus) {
                if (pdu.length <= 88) {
                    // GSM format cell broadcast
                    pdu[0] = (byte) ((serialNumber >>> 8) & 0xff);
                    pdu[1] = (byte) (serialNumber & 0xff);
                    if (category != 0) {
                        pdu[2] = (byte) ((category >>> 8) & 0xff);
                        pdu[3] = (byte) (category & 0xff);
                    }
                } else {
                    // UMTS format cell broadcast
                    pdu[3] = (byte) ((serialNumber >>> 8) & 0xff);
                    pdu[4] = (byte) (serialNumber & 0xff);
                    if (category != 0) {
                        pdu[1] = (byte) ((category >>> 8) & 0xff);
                        pdu[2] = (byte) (category & 0xff);
                    }
                }
            }
            return GsmSmsCbMessage.createSmsCbMessage(context, new SmsCbHeader(pdus[0]), new SmsCbLocation(""), pdus, 0 /* slotIndex */);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
