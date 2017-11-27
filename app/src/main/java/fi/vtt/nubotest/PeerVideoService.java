package fi.vtt.nubotest;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import org.webrtc.VideoRenderer;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import fi.vtt.nubomedia.kurentoroomclientandroid.RoomError;
import fi.vtt.nubomedia.kurentoroomclientandroid.RoomListener;
import fi.vtt.nubomedia.kurentoroomclientandroid.RoomNotification;
import fi.vtt.nubomedia.kurentoroomclientandroid.RoomResponse;
import fi.vtt.nubomedia.webrtcpeerandroid.NBMMediaConfiguration;
import fi.vtt.nubomedia.webrtcpeerandroid.NBMPeerConnection;
import fi.vtt.nubomedia.webrtcpeerandroid.NBMWebRTCPeer;
import fi.vtt.nubotest.util.Constants;

/**
 * Created by Johnson on 2017/11/10.
 */

public class PeerVideoService extends IntentService implements NBMWebRTCPeer.Observer, RoomListener {
  private static final String TAG = "PeerVideoService";
  private NBMWebRTCPeer nbmWebRTCPeer;
  private Map<Integer, String> videoRequestUserMapping;
  private int publishVideoRequestId;
  private String username;
  private boolean backPressed = false;
  private Thread backPressedThread = null;

  private Handler mHandler = new Handler();
  private CallState callState;
  private Runnable offerWhenReady = new Runnable() {
    @Override
    public void run() {
      // Generate offers to receive video from all peers in the room
      for (Map.Entry<String, Boolean> entry : MainActivity.userPublishList.entrySet()) {
        if (entry.getValue()) {
          GenerateOfferForRemote(entry.getKey());
          Log.i(TAG, "I'm " + username + " DERP: Generating offer for peer " + entry.getKey());
          // Set value to false so that if this function is called again we won't
          // generate another offer for this user
          entry.setValue(false);
        }
      }
    }
  };

  public PeerVideoService() {
    super("PeerVideoService");
  }

  public PeerVideoService(String name) {
    super(name);
  }

  @Override
  protected void onHandleIntent(@Nullable Intent intent) {
    Bundle extras = intent.getExtras();
    this.username = extras.getString(Constants.USER_NAME, "");
    Log.i(TAG, "username: " + username);

    MainActivity.getKurentoRoomAPIInstance().addObserver(this);

    NBMMediaConfiguration peerConnectionParameters = new NBMMediaConfiguration(
        NBMMediaConfiguration.NBMRendererType.OPENGLES,
        NBMMediaConfiguration.NBMAudioCodec.OPUS, 0,
        PeerVideoActivity.CODEC, 0,
        new NBMMediaConfiguration.NBMVideoFormat(PeerVideoActivity.WIDTH, PeerVideoActivity.HEIGHT, PixelFormat.RGB_888, PeerVideoActivity.FPS),
        NBMMediaConfiguration.NBMCameraPosition.BACK);

    videoRequestUserMapping = new HashMap<>();

    nbmWebRTCPeer = new NBMWebRTCPeer(peerConnectionParameters, this, new VideoRenderer.Callbacks() {
      @Override
      public void renderFrame(VideoRenderer.I420Frame i420Frame) {

      }
    }, this);
    nbmWebRTCPeer.registerMasterRenderer(MainActivity.tmpView);
    Log.i(TAG, "Initializing nbmWebRTCPeer...");
    nbmWebRTCPeer.initialize();
    callState = CallState.PUBLISHING;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onInitialize() {
    nbmWebRTCPeer.generateOffer("local", true);
  }

  @Override
  public void onLocalSdpOfferGenerated(SessionDescription sessionDescription, NBMPeerConnection nbmPeerConnection) {
    if (callState == CallState.PUBLISHING || callState == CallState.PUBLISHED) {
      Log.d(TAG, "Sending " + sessionDescription.type);
      publishVideoRequestId = ++Constants.id;
      MainActivity.getKurentoRoomAPIInstance().sendPublishVideo(sessionDescription.description, false, publishVideoRequestId);
    } else { // Asking for remote user video
      Log.d(TAG, "Sending " + sessionDescription.type);
      publishVideoRequestId = ++Constants.id;
      String username = nbmPeerConnection.getConnectionId();
      videoRequestUserMapping.put(publishVideoRequestId, username);
      MainActivity.getKurentoRoomAPIInstance().sendReceiveVideoFrom(username, "webcam", sessionDescription.description, publishVideoRequestId);
    }
  }

  @Override
  public void onLocalSdpAnswerGenerated(SessionDescription localSdpAnswer, NBMPeerConnection connection) {
  }

  @Override
  public void onIceCandidate(IceCandidate iceCandidate, NBMPeerConnection nbmPeerConnection) {
    int sendIceCandidateRequestId = ++Constants.id;
    if (callState == CallState.PUBLISHING || callState == CallState.PUBLISHED) {
      MainActivity.getKurentoRoomAPIInstance().sendOnIceCandidate(this.username, iceCandidate.sdp,
          iceCandidate.sdpMid, Integer.toString(iceCandidate.sdpMLineIndex), sendIceCandidateRequestId);
    } else {
      MainActivity.getKurentoRoomAPIInstance().sendOnIceCandidate(nbmPeerConnection.getConnectionId(), iceCandidate.sdp,
          iceCandidate.sdpMid, Integer.toString(iceCandidate.sdpMLineIndex), sendIceCandidateRequestId);
    }
  }

  @Override
  public void onIceStatusChanged(PeerConnection.IceConnectionState state, NBMPeerConnection connection) {
    Log.i(TAG, "onIceStatusChanged");
  }

  @Override
  public void onRemoteStreamAdded(MediaStream stream, NBMPeerConnection connection) {
    Log.i(TAG, "onRemoteStreamAdded");
    nbmWebRTCPeer.setActiveMasterStream(stream);
  }

  @Override
  public void onRemoteStreamRemoved(MediaStream stream, NBMPeerConnection connection) {
    Log.i(TAG, "onRemoteStreamRemoved");
  }

  @Override
  public void onPeerConnectionError(String error) {
    Log.e(TAG, "onPeerConnectionError:" + error);
  }

  @Override
  public void onDataChannel(DataChannel dataChannel, NBMPeerConnection connection) {
    Log.i(TAG, "[datachannel] Peer opened data channel");
  }

  @Override
  public void onBufferedAmountChange(long l, NBMPeerConnection connection, DataChannel channel) {

  }

  public void sendHelloMessage(DataChannel channel) {
    byte[] rawMessage = "Hello Peer!".getBytes(Charset.forName("UTF-8"));
    ByteBuffer directData = ByteBuffer.allocateDirect(rawMessage.length);
    directData.put(rawMessage);
    directData.flip();
    DataChannel.Buffer data = new DataChannel.Buffer(directData, false);
    channel.send(data);
  }

  @Override
  public void onStateChange(NBMPeerConnection connection, DataChannel channel) {
    Log.i(TAG, "[datachannel] DataChannel onStateChange: " + channel.state());
    if (channel.state() == DataChannel.State.OPEN) {
      sendHelloMessage(channel);
      Log.i(TAG, "[datachannel] Datachannel open, sending first hello");
    }
  }

  @Override
  public void onMessage(DataChannel.Buffer buffer, NBMPeerConnection connection, DataChannel channel) {
    Log.i(TAG, "[datachannel] Message received: " + buffer.toString());
    sendHelloMessage(channel);
  }

  private void GenerateOfferForRemote(String remote_name) {
    nbmWebRTCPeer.generateOffer(remote_name, false);
    callState = CallState.WAITING_REMOTE_USER;
  }

  @Override
  public void onRoomResponse(RoomResponse response) {
    Log.d(TAG, "OnRoomResponse:" + response);
    int requestId = response.getId();

    if (requestId == publishVideoRequestId) {

      SessionDescription sd = new SessionDescription(SessionDescription.Type.ANSWER,
          response.getValue("sdpAnswer").get(0));

      // Check if we are waiting for publication of our own vide
      if (callState == CallState.PUBLISHING) {
        callState = CallState.PUBLISHED;
        nbmWebRTCPeer.processAnswer(sd, "local");
        mHandler.postDelayed(offerWhenReady, 2000);

        // Check if we are waiting for the video publication of the other peer
      } else if (callState == CallState.WAITING_REMOTE_USER) {
        //String user_name = Integer.toString(publishVideoRequestId);
        callState = CallState.RECEIVING_REMOTE_USER;
        String connectionId = videoRequestUserMapping.get(publishVideoRequestId);
        nbmWebRTCPeer.processAnswer(sd, connectionId);
      }
    }

  }

  @Override
  public void onRoomError(RoomError error) {
    Log.e(TAG, "OnRoomError:" + error);
  }

  @Override
  public void onRoomNotification(RoomNotification notification) {
    Log.i(TAG, "OnRoomNotification (state=" + callState.toString() + "):" + notification);
    Map<String, Object> map = notification.getParams();

    if (notification.getMethod().equals(RoomListener.METHOD_ICE_CANDIDATE)) {
      String sdpMid = map.get("sdpMid").toString();
      int sdpMLineIndex = Integer.valueOf(map.get("sdpMLineIndex").toString());
      String sdp = map.get("candidate").toString();
      IceCandidate ic = new IceCandidate(sdpMid, sdpMLineIndex, sdp);

      if (callState == CallState.PUBLISHING || callState == CallState.PUBLISHED) {
        nbmWebRTCPeer.addRemoteIceCandidate(ic, "local");
      } else {
        nbmWebRTCPeer.addRemoteIceCandidate(ic, notification.getParam("endpointName").toString());
      }
    } else if (notification.getMethod().equals(RoomListener.METHOD_PARTICIPANT_PUBLISHED)) {
      mHandler.postDelayed(offerWhenReady, 2000);
    }
  }

  @Override
  public void onRoomConnected() {
  }

  @Override
  public void onRoomDisconnected() {
  }

  private enum CallState {
    IDLE, PUBLISHING, PUBLISHED, WAITING_REMOTE_USER, RECEIVING_REMOTE_USER
  }
}
