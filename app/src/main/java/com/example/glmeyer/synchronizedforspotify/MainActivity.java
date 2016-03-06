package com.example.glmeyer.synchronizedforspotify;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragment;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity implements
        PlayerNotificationCallback, ConnectionStateCallback, YouTubePlayer.OnInitializedListener {

    boolean paused = false;
    private final static int REQUEST_ENABLE_BT = 1;
    // TODO: Replace with your client ID
    private static final String CLIENT_ID = "90eb86d7dd924772994f5134d9eb6cab";
    // TODO: Replace with your redirect URI
    private static final String REDIRECT_URI = "http://griffinmeyer.com/callback/";

    private static final String YOUTUBE_API = "AIzaSyBSIIxLZXwEKVsy9XrlaeS5fs25yvBDlYY";

    private static final UUID MY_UUID = UUID.fromString("5a6a493f-6feb-4145-8853-4593aa1b4f1c");

    private Player mPlayer;
    // Request code that will be used to verify if the result comes from correct activity
// Can be any integer
    private static final int REQUEST_CODE = 1337;
    BluetoothAdapter mBluetooth;
    BluetoothDevice global;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        YouTubePlayerFragment youTubePlayerFragment = (YouTubePlayerFragment) getFragmentManager().findFragmentById(R.id.youtube_fragment);
        youTubePlayerFragment.initialize(YOUTUBE_API, this);

        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "playlist-read","playlist-read-private", "streaming"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);

        mBluetooth = BluetoothAdapter.getDefaultAdapter();
        if(mBluetooth == null){
            //No bluetooth support
        }else if(mBluetooth != null) {
            if (!mBluetooth.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }else{
                getPairedDevices();
                global = mBluetooth.getRemoteDevice("84:10:0D:4C:6F:BD");
            }
        }
    }
BluetoothDevice remoteDiv = null;
    public void getPairedDevices(){
        Set<BluetoothDevice> pairedDevices = mBluetooth.getBondedDevices();
        if(pairedDevices.size() > 0){
            for (BluetoothDevice device : pairedDevices){
                Log.d("Bluetooth: ", device.getAddress());

            }
        }
    }

    public void sendPlay(){

    }
    public void startClient(View v){
        ConnectThread connectThread = new ConnectThread(global);
        Thread thread = new Thread(connectThread);
        thread.start();

    }

String token = "";
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Log.d("SpotifyLog", resultCode + "");

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            final AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                Config playerConfig = new Config(this, response.getAccessToken(), CLIENT_ID);
                Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                    @Override
                    public void onInitialized(Player player) {
                        token = response.getAccessToken();
                        mPlayer = player;
                        mPlayer.addConnectionStateCallback(MainActivity.this);
                        mPlayer.addPlayerNotificationCallback(MainActivity.this);

                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                    }
                });
            }
        }
    }

    public void logOut(View v){
        //AuthenticationClient.clearCookies(this);
        AcceptThread server = new AcceptThread();
        Thread thread = new Thread(server);
        thread.start();
    }

    public void playSong(View v){
        //EditText et = (EditText)findViewById(R.id.editText);
        mPlayer.play("spotify:track:6FE2iI43OZnszFLuLtvvmg");
        paused = false;
    }

    public void pauseSong(View v){
        if (paused == false) {
            mPlayer.pause();
            paused = true;
        }else if(paused == true) {
            mPlayer.resume();
            paused = false;
        }
    }

    public void showAuth(View v){
        Log.d("SPOTYLOG", UUID.randomUUID().toString());

    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
    }

    @Override
    public void onLoginFailed(Throwable error) {
        Log.d("MainActivity", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
        Log.d("MainActivity", "Playback event received: " + eventType.name());
        switch (eventType) {
            // Handle event type as necessary
            default:
                break;
        }
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String errorDetails) {
        Log.d("MainActivity", "Playback error received: " + errorType.name());
        switch (errorType) {
            // Handle error type as necessary
            default:
                break;
        }
    }


    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }
    YouTubePlayer globalPlayer;

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {
        if(!b){
            youTubePlayer.cueVideo("nCgQDjiotG0");
            globalPlayer = youTubePlayer;
        }
    }

    public void playVideo(View v){
        globalPlayer.play();
    }


    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {

    }

    void printSocket(BluetoothSocket s){
        Log.d("Socket: ", s.toString());
        mPlayer.play("spotify:track:6FE2iI43OZnszFLuLtvvmg");
    }


    private class AcceptThread implements Runnable {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = mBluetooth.listenUsingRfcommWithServiceRecord("Bluetooth", MY_UUID);
            } catch (IOException e) { }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    Log.d("Server: ", "Server is running");
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    // Do work to manage the connection (in a separate thread)
                    printSocket(socket);
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                    }
                    break;
                }
            }
        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) { }
        }
    }

    private class ConnectThread implements Runnable {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetooth.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            printSocket(mmSocket);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

}

