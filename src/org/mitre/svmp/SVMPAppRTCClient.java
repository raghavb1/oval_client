/*
 * Copyright (c) 2013 The MITRE Corporation, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this work except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Derived from GAEChannelClient from the libjingle / webrtc AppRTCDemo
// example application distributed under the following license.
/*
 * libjingle
 * Copyright 2013, Google Inc.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.mitre.svmp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.SSLCertificateSocketFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mitre.svmp.auth.AuthData;
import org.mitre.svmp.client.R;
import org.mitre.svmp.protocol.SVMPProtocol;
import org.mitre.svmp.protocol.SVMPProtocol.AuthResponse;
import org.mitre.svmp.protocol.SVMPProtocol.Request;
import org.mitre.svmp.protocol.SVMPProtocol.Request.RequestType;
import org.mitre.svmp.protocol.SVMPProtocol.Response;
import org.mitre.svmp.protocol.SVMPProtocol.Response.ResponseType;
import org.mitre.svmp.protocol.SVMPProtocol.VideoStreamInfo;
import org.mitre.svmp.protocol.SVMPProtocol.WebRTCMessage;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.net.SocketFactory;
import javax.net.ssl.SSLException;

/**
 * Negotiates signaling for chatting with apprtc.appspot.com "rooms".
 * Uses the client<->server specifics of the apprtc AppEngine webapp.
 *
 * To use: create an instance of this object (registering a message handler) and
 * call connectToRoom().  Once that's done call sendMessage() and wait for the
 * registered handler to be called with received messages.
 */
public class SVMPAppRTCClient implements Constants {
  private static final String TAG = "SVMPAppRTCClient";
  private final AppRTCDemoActivity activity;
  private final SVMPChannelClient.MessageHandler gaeHandler;
  private final IceServersObserver iceServersObserver;

  // These members are only read/written under sendQueue's lock.
  //private LinkedList<String> sendQueue = new LinkedList<String>();
  private BlockingQueue<SVMPProtocol.Request> sendQueue = new LinkedBlockingQueue<SVMPProtocol.Request>();
  private AppRTCSignalingParameters appRTCSignalingParameters;

  // used to determine what the EncryptionType for each connection is
  private boolean useSsl;
  private boolean sslDebug;
  
  private ConnectionInfo connectionInfo;
  private DatabaseHandler dbHandler;
  
  private Socket svmpSocket;
  private InputStream socketIn;
  private OutputStream socketOut;
  private boolean proxying = false;
  
  private SocketSender sender = null;
  private SocketListener listener = null;
  
  /**
   * Callback fired once the room's signaling parameters specify the set of
   * ICE servers to use.
   */
  public static interface IceServersObserver {
    public void onIceServers(List<PeerConnection.IceServer> iceServers);
  }

  public SVMPAppRTCClient(
      AppRTCDemoActivity activity, SVMPChannelClient.MessageHandler gaeHandler,
      IceServersObserver iceServersObserver) 
  {
    this.activity = activity;
    this.gaeHandler = gaeHandler;
    this.iceServersObserver = iceServersObserver;
    this.dbHandler = new DatabaseHandler(activity);
  }

  public void connectToRoom(ConnectionInfo connectionInfo) {
    this.connectionInfo = connectionInfo;

    // determine both booleans from the EncryptionType integer
    useSsl = (connectionInfo.getEncryptionType() == ENCRYPTION_SSLTLS
            || connectionInfo.getEncryptionType() == ENCRYPTION_SSLTLS_UNTRUSTED);
    sslDebug = (connectionInfo.getEncryptionType() == ENCRYPTION_SSLTLS_UNTRUSTED);
    
    (new SocketConnector()).execute();
  }

  /**
   * Disconnect from the SVMP proxy channel.
   * @throws IOException 
   */
  public void disconnect() throws IOException {
    proxying = false;
    if (dbHandler != null)
      dbHandler.close();
    if (sender != null)
      sender.cancel(true);
    if (listener != null)
      listener.cancel(true);
    if (socketIn != null)
      socketIn.close();
    if (socketOut != null)
      socketOut.close();
    if (!svmpSocket.isClosed())
      svmpSocket.close();
  }
  
  private void startProxying() {
    proxying = true;
    
    sender = new SocketSender();
    listener = new SocketListener();

    sender.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    listener.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }
  
  public boolean isProxying() {
      return proxying;
  }

  /**
   * Queue a message for sending to the room's channel and send it if already
   * connected (other wise queued messages are drained when the channel is
     eventually established).
   */
  public synchronized void sendMessage(String msg) {
      WebRTCMessage.Builder rtcmsg = WebRTCMessage.newBuilder();
      rtcmsg.setJson(msg);
      
      sendMessage(Request.newBuilder()
              .setType(RequestType.WEBRTC)
              .setWebrtcMsg(rtcmsg)
              .build());
  }
  
  public synchronized void sendMessage(Request msg) {
    if (proxying)
      sendQueue.add(msg);
  }

  public boolean isInitiator() {
    return appRTCSignalingParameters.initiator;
  }

  public MediaConstraints pcConstraints() {
    return appRTCSignalingParameters.pcConstraints;
  }

  public MediaConstraints videoConstraints() {
    return appRTCSignalingParameters.videoConstraints;
  }

  // Struct holding the signaling parameters of an AppRTC room.
  private class AppRTCSignalingParameters {
    public final List<PeerConnection.IceServer> iceServers;
    public final boolean initiator;
    public final MediaConstraints pcConstraints;
    public final MediaConstraints videoConstraints;

    public AppRTCSignalingParameters(
        List<PeerConnection.IceServer> iceServers,
        boolean initiator, MediaConstraints pcConstraints,
        MediaConstraints videoConstraints) {
      this.initiator = initiator;
      this.iceServers = iceServers;
      this.pcConstraints = pcConstraints;
      this.videoConstraints = videoConstraints;
    }
  }

  // AsyncTask that converts an AppRTC room URL into the set of signaling
  // parameters to use with that room.
  private class VideoParameterGetter
      extends AsyncTask<Void, Void, AppRTCSignalingParameters> {

    @Override
    protected AppRTCSignalingParameters doInBackground(Void... params) {
      try {
        toastMe( "Querying for webrtc channel info" );
        
        // send video info request
        Request.Builder req = Request.newBuilder();
        req.setType(RequestType.VIDEO_PARAMS);
        req.build().writeDelimitedTo(socketOut);
        
        // get video info response
        Response resp = Response.parseDelimitedFrom(socketIn);
        
        // parse it and populate a SignalingParams
        if (resp.getType() == ResponseType.VIDSTREAMINFO || resp.hasVideoInfo())
          return getParametersForRoom(resp.getVideoInfo());
        
      } catch (Exception e) {
        Log.e(TAG, e.getMessage());
      }
      return null;
    }

    @Override
    protected void onPostExecute(AppRTCSignalingParameters params) {
      toastMe("Got channel info.");
      startProxying();

      appRTCSignalingParameters = params;
      iceServersObserver.onIceServers(appRTCSignalingParameters.iceServers);
      gaeHandler.onOpen();
    }

    private AppRTCSignalingParameters getParametersForRoom(VideoStreamInfo info) {
      MediaConstraints pcConstraints = constraintsFromJSON(info.getPcConstraints());
      Log.d(TAG, "pcConstraints: " + pcConstraints);

      MediaConstraints videoConstraints = constraintsFromJSON(info.getVideoConstraints());
      Log.d(TAG, "videoConstraints: " + videoConstraints);

      LinkedList<PeerConnection.IceServer> iceServers = iceServersFromPCConfigJSON(info.getIceServers());
      
      return new AppRTCSignalingParameters(iceServers, true, pcConstraints, videoConstraints);
      
    }

    private MediaConstraints constraintsFromJSON(String jsonString) {
      try {
        MediaConstraints constraints = new MediaConstraints();
        JSONObject json = new JSONObject(jsonString);
        JSONObject mandatoryJSON = json.optJSONObject("mandatory");
        if (mandatoryJSON != null) {
          JSONArray mandatoryKeys = mandatoryJSON.names();
          if (mandatoryKeys != null) {
            for (int i = 0; i < mandatoryKeys.length(); ++i) {
              String key = (String) mandatoryKeys.getString(i);
              String value = mandatoryJSON.getString(key);
              constraints.mandatory.add(
                  new MediaConstraints.KeyValuePair(key, value));
            }
          }
        }
        JSONArray optionalJSON = json.optJSONArray("optional");
        if (optionalJSON != null) {
          for (int i = 0; i < optionalJSON.length(); ++i) {
            JSONObject keyValueDict = optionalJSON.getJSONObject(i);
            String key = keyValueDict.names().getString(0);
            String value = keyValueDict.getString(key);
            constraints.optional.add(
                new MediaConstraints.KeyValuePair(key, value));
          }
        }
        return constraints;
      } catch (JSONException e) {
        throw new RuntimeException(e);
      }
    }
  }

  // Return the list of ICE servers described by a WebRTCPeerConnection
  // configuration string.
  private LinkedList<PeerConnection.IceServer> iceServersFromPCConfigJSON(
      String pcConfig) {
    try {
      JSONObject json = new JSONObject(pcConfig);
      JSONArray servers = json.getJSONArray("iceServers");
      LinkedList<PeerConnection.IceServer> ret =
          new LinkedList<PeerConnection.IceServer>();
      for (int i = 0; i < servers.length(); ++i) {
        JSONObject server = servers.getJSONObject(i);
        String url = server.getString("url");
        String credential =
            server.has("credential") ? server.getString("credential") : "";
        ret.add(new PeerConnection.IceServer(url, "", credential));
      }
      return ret;
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  // Connect to the SVMP server
  private class SocketConnector extends AsyncTask <Void, Void, Boolean> {

    private Request authRequest;
    @Override
    protected Boolean doInBackground(Void... params) {
      boolean returnVal = false;
      try {
        // get the auth request, which is either constructed from a session token or made up of auth input (password, etc)
        String sessionToken = dbHandler.getSessionToken(connectionInfo);
        if (sessionToken.length() > 0)
            authRequest = AuthData.makeRequest(connectionInfo, sessionToken);
        else
          // get the auth data req it's removed from memory
          authRequest = AuthData.getRequest(connectionInfo);
        socketConnect();
        returnVal = true;
      } catch (SSLException e) {
        if (e.getMessage().contains("I/O error during system call, Connection reset by peer")) {
          // connection failed, we tried to connect using SSL but proxy's SSL is turned off
          toastMe(""); // cancel current toasts
          Intent intent = new Intent();
          intent.putExtra("connectionID", connectionInfo.getConnectionID());
          intent.putExtra("message", R.string.connectionList_toast_failSSL);
          activity.setResult(SvmpActivity.RESULT_CANCELED, intent);
          activity.finish();
        }
        else {
          Log.e(TAG, e.getMessage());
        }
      } catch (Exception e) {
        Log.e(TAG, e.getMessage());
      }
      return returnVal;
    }

    @Override
    protected void onPostExecute(Boolean result) {
      toastMe( result ? "Connected to " + connectionInfo.getHost() : "Connection failed" );

      if (result)
        new SVMPAuthenticator().execute(authRequest);
    }
      
    private void socketConnect() throws UnknownHostException, IOException {
      SocketFactory sf;

      Log.d(TAG, "Socket connecting to " + connectionInfo.getHost() + ":" + DEFAULT_PORT);

      if (useSsl) {
        if (sslDebug)
          sf = SSLCertificateSocketFactory.getInsecure(0, null);
        else
          sf = SSLCertificateSocketFactory.getDefault(0, null);
     } else {
       sf = SocketFactory.getDefault();
     }

     svmpSocket = sf.createSocket(connectionInfo.getHost(), connectionInfo.getPort());

     socketOut = svmpSocket.getOutputStream();
     socketIn = svmpSocket.getInputStream();
    }
  }

  // Perform authentication request/resposne
  private class SVMPAuthenticator extends AsyncTask <Request, Void, Boolean> {

    @Override
    protected Boolean doInBackground(Request... request) {
      if (!svmpSocket.isConnected())
        return false;

      if (request[0] == null)
        return false;
      
      try {
        // send authentication request
        request[0].writeDelimitedTo(socketOut);

        // get response
        Response resp = Response.parseDelimitedFrom(socketIn);
        if (resp.getType() == ResponseType.AUTH) {
          AuthResponse authResponse = resp.getAuthResponse();
          if (authResponse.getType() == AuthResponse.AuthResponseType.AUTH_OK) {
            // we authenticated successfully, check if we received a session token
            if (authResponse.hasSessionToken())
              dbHandler.updateSessionToken(connectionInfo, authResponse.getSessionToken());

            return true;
          }
        }

        // should be an AuthResponse with a type of AUTH_FAIL, but fail anyway if it isn't
        // if we used a session token and authentication failed, discard it
        dbHandler.updateSessionToken(connectionInfo, "");
        return false;
      } catch (IOException e) {
        Log.e(TAG, e.getMessage());
        return false;
      }
    }

    @Override
    protected void onPostExecute(Boolean result) {
      if (result) {
        // auth succeeded, wait for VMREADY
        (new SVMPReadyWait()).execute();
      } else {
        // authentication failed, handle appropriately
        toastMe(""); // cancel current toasts
          Intent intent = new Intent();
          intent.putExtra("connectionID", connectionInfo.getConnectionID());
          intent.putExtra("message", R.string.svmpActivity_toast_authFailed);
          activity.setResult(SvmpActivity.RESULT_NEEDAUTH, intent);
          activity.finish();
      }
    }
  }

  // AsynTask that waits for the VMREADY message from the SVMP server
  private class SVMPReadyWait extends AsyncTask <Void, Void, Boolean> {
    @Override
    protected Boolean doInBackground(Void... params) {
      // wait for VMREADY
      Response resp;
      try {
        resp = Response.parseDelimitedFrom(socketIn);
        if (resp.getType() == ResponseType.VMREADY)
          return true;
      } catch (IOException e) {
        Log.e(TAG, e.getMessage());
      }
      return false;
    }

    @Override
    protected void onPostExecute(Boolean result) {
      toastMe(result ? "VM is ready" : "Kraken Attack!!!!");

      if (result) {
        // auth succeeded, get room parameters
        (new VideoParameterGetter()).execute();
      } else {
        // TODO
        // got something other than VMREADY, panic
      }
    }
  }
  
  // Send loop thread for when we're in proxying mode
  private class SocketSender extends AsyncTask <Void, Void, Void> {

    @Override
    protected Void doInBackground(Void... params) {
      Log.i(TAG, "Server connection send thread starting");
      while (proxying && svmpSocket != null && svmpSocket.isConnected() && socketOut != null) {
        try {
          Log.i(TAG,"Writing message to VM...");
          sendQueue.take().writeDelimitedTo(socketOut);
        } catch (Exception e) {
          Log.e(TAG,"Exception in sendMessage " + e.getMessage());
        }
      }
      return null;
    }
  }
  
  // Receive loop thread for when we're in proxying mode
  private class SocketListener extends AsyncTask <Void, Void, Void> {
    @Override
    protected Void doInBackground(Void... params) {
      try {
        Log.i(TAG, "Server connection receive thread starting");
        while (proxying && svmpSocket != null && svmpSocket.isConnected() && socketIn != null) {
          Log.d(TAG, "Waiting for incoming message");
          final SVMPProtocol.Response data = SVMPProtocol.Response.parseDelimitedFrom(socketIn);
          Log.d(TAG, "Received incoming message object of type " + data.getType().name());

          if (data != null) {
            activity.runOnUiThread(new Runnable() {
              public void run() {
                gaeHandler.onMessage(data);
              }
            });
          }
        }
        Log.i(TAG, "Server connection receive thread exiting");
      } catch (Exception e) {
        proxying = false;
        Log.i(TAG, "Server connection disconnected.");
      }
      return null;
    }
  }

  private void toastMe(final String msg) {
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Context context = activity.getApplicationContext();
        activity.logAndToast(msg);
      }
    });
  }
}