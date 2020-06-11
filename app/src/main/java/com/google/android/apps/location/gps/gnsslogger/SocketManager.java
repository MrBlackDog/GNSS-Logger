package com.google.android.apps.location.gps.gnsslogger;

import android.os.Build;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;

public class SocketManager {

  //  final byte[] ipAddr = new byte[]{(byte)176, (byte)15, (byte)174, (byte)137};
    final int port = 533;

    public WebSocket Connect()
    {
        OkHttpClient client = new OkHttpClient.Builder().build();
        Request request = new Request.Builder()
                .url("ws://mrblackdog.ddns.net:5000/ws")
                .build();

        WebSocketGolos wsc = new WebSocketGolos();
        WebSocket ws = client.newWebSocket(request, wsc);
        /** Connection format:
         * Phone:ID:Mode:Diff_Mode
         *
         * Phone - Identifier for connected device
         * ID - Unique ID for device
         * Mode: StandAlone or Relative at this time
         * Relative_mode: Base or Target, none if Mode if StandAlone
         */
        ws.send("Phone:" + MainActivity.model + ":" + "Relative:" + "Base");
        return ws;
    }
}
