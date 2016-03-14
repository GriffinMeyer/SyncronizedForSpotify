package com.example.glmeyer.synchronizedforspotify;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.os.Handler;
import android.view.View.OnClickListener;


import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.PlayerStateCallback;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.squareup.picasso.Picasso;

import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends Activity implements
        PlayerNotificationCallback, ConnectionStateCallback, OnClickListener{

    private static final int MESSAGE_READ = 1;
    boolean paused = false;
    private final static int REQUEST_ENABLE_BT = 1;
    // TODO: Replace with your client ID
    private static final String CLIENT_ID = "90eb86d7dd924772994f5134d9eb6cab";
    // TODO: Replace with your redirect URI
    private static final String REDIRECT_URI = "http://griffinmeyer.com/callback/";
    boolean muted;
    AudioManager myAudioManager = null;
    private SeekBar volumeSeekbar = null;
    private ImageButton micButt;
    private EditText searchText;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    boolean control = false;
    private int currValue;

    private static final UUID MY_UUID = UUID.fromString("5a6a493f-6feb-4145-8853-4593aa1b4f1c");

    private Player mPlayer;
    // Request code that will be used to verify if the result comes from correct activity
    // Can be any integer
    private static final int REQUEST_CODE = 1337;
    BluetoothAdapter mBluetooth;
    BluetoothDevice global;
    private android.os.Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //currValue = myAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getInit();
        myAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        muted = false;
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        initControls();
        searchText = (EditText) findViewById(R.id.searchbar);
        micButt = (ImageButton) findViewById(R.id.micbutton);
        micButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptSpeechInput();
            }
        });

        mBluetooth = BluetoothAdapter.getDefaultAdapter();
        if(mBluetooth != null && mBluetooth.isEnabled()){
            ImageButton btButton = (ImageButton)findViewById(R.id.bluetoothbutton);
            btButton.setImageResource(R.drawable.bluetooth_enabled);
            AcceptThread server = new AcceptThread();
            Thread thread = new Thread(server);
            thread.start();
        }

        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "playlist-read", "playlist-read-private", "streaming"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
        //ImageView img = (ImageView)findViewById(R.id.imageView);
        //Picasso.with(this).load("https://i.scdn.co/image/941a834a4e4e9ef191883ac25c5fecf4960573c4").into(img);
        final EditText search = (EditText)findViewById(R.id.searchbar);
        search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_SEARCH) {
                    search(v);
                    return true;
                }
                return false;
            }
        });
    }

    private void initControls()
    {
        try
        {
            volumeSeekbar = (SeekBar)findViewById(R.id.volumebar);
            volumeSeekbar.setMax(myAudioManager
                    .getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            volumeSeekbar.setProgress(myAudioManager
                    .getStreamVolume(AudioManager.STREAM_MUSIC));


            volumeSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
            {
                @Override
                public void onStopTrackingTouch(SeekBar arg0)
                {
                }

                @Override
                public void onStartTrackingTouch(SeekBar arg0)
                {
                }

                @Override
                public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
                    myAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                            progress, 0);
                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void mute(View v) {
        ImageButton m = (ImageButton) findViewById(R.id.mutebutton);
        if (muted) {
            myAudioManager.setStreamVolume(myAudioManager.STREAM_MUSIC, 5, 0);
            muted = false;
            m.setImageResource(R.drawable.muteoff);
            return;

        }else if (!muted) {
            myAudioManager.setStreamVolume(myAudioManager.STREAM_MUSIC, 0, 0);
            muted = true;
            m.setImageResource(R.drawable.mutebutton);
            return;
        }
    }

    public void startBluetooth(View v){
        if(mBluetooth == null){
            Toast toast = Toast.makeText(this, "No bluetooth adapter found!", Toast.LENGTH_SHORT);
            toast.show();

        }else if(mBluetooth != null){
            if(mBluetooth.isEnabled()){
                mBluetooth.disable();
                ImageButton btButton = (ImageButton)findViewById(R.id.bluetoothbutton);
                btButton.setImageResource(R.drawable.bluetooth_disabled);
            }else if(!mBluetooth.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                ImageButton btButton = (ImageButton)findViewById(R.id.bluetoothbutton);
                btButton.setImageResource(R.drawable.bluetooth_enabled);
            }
        }
    }

    int songMax;
    int songPos;

    ImageButton play_button;
    Handler seekHandler = new Handler();
    SeekBar seekBar;

    public void getInit() {
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        play_button = (ImageButton) findViewById(R.id.playButton);
        play_button.setOnClickListener(this);
    }

    Runnable run = new Runnable() {

        @Override
        public void run() {
            seekUpdation();
        }
    };

    public void seekUpdation() {
        seekBar.setMax(songMax);
        seekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        seekBar.setProgress(progress);
                        mPlayer.seekToPosition(progress);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }

                }
        );
        seekHandler.postDelayed(run, 1000);
    }

    @Override
    public void onClick (View view){
        switch (view.getId()) {
            case R.id.playButton:
                playButton(view);
                break;
        }
    }


    public void connectBluetooth(View v){
        if(mBluetooth.isEnabled()){
            Intent getDevice = new Intent(this, DeviceList.class);
            startActivityForResult(getDevice, 69);
        }
    }

    public void search(View v){
        Intent intent = new Intent(this, Search.class);
        EditText et = (EditText)findViewById(R.id.searchbar);
        String searchText = et.getText().toString();
        intent.putExtra("searchText", searchText);
        startActivityForResult(intent, 57);
    }
ArrayList<Item> playQueue = new ArrayList<Item>();

String token = "";
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Log.d("SpotifyLog", resultCode + "");

        if(requestCode == REQ_CODE_SPEECH_INPUT){
            if (resultCode == RESULT_OK && null != intent) {

                ArrayList<String> result = intent
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                searchText.setText(result.get(0));
            }
        }

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
        if(requestCode == 69){
            if(resultCode == Activity.RESULT_OK) {
                TextView conDevice = (TextView)findViewById(R.id.connectedDevice);
                conDevice.setText("Connecting...");
                String macAdd = intent.getStringExtra("btMac");
                String devName = intent.getStringExtra("btName");
                BluetoothDevice dev;
                dev = mBluetooth.getRemoteDevice(macAdd);
                ConnectThread connectThread = new ConnectThread(dev);
                Thread thread = new Thread(connectThread);
                thread.start();
            }
        }
        if(requestCode == 1){

            AcceptThread server = new AcceptThread();
            Thread thread = new Thread(server);
            thread.start();
        }

        if(requestCode == 57){
            if(resultCode == Activity.RESULT_OK) {
                TextView searchBar = (TextView) findViewById(R.id.searchbar);
                searchBar.setText("");
                Item returnedItem = (Item) intent.getSerializableExtra("item");
                if (control) {
                    if (connectedThread != null) {
                        connectedThread.write(("add" + "-" + returnedItem.uri + "-" + returnedItem.album.images.get(1).url + "-" + returnedItem.name + "-" + returnedItem.artists.get(0).name).getBytes());
                    }
                } else {
                    if (playQueue.isEmpty()) {
                        ImageButton btn = (ImageButton) findViewById(R.id.playButton);
                        ImageView albumArt = (ImageView) findViewById(R.id.mainalbumart);
                        TextView title = (TextView) findViewById(R.id.mainTitle);
                        TextView artist = (TextView) findViewById(R.id.mainArtist);
                        Picasso.with(this).load(returnedItem.album.images.get(1).url).into(albumArt);
                        btn.setImageResource(R.drawable.pausebutton);
                        title.setText(returnedItem.name);
                        artist.setText(returnedItem.artists.get(0).name);
                        mPlayer.queue(returnedItem.uri);
                        songStarted = true;
                        playQueue.add(returnedItem);
                        if (connectedThread != null) {
                            connectedThread.write(("playing" + "-" + returnedItem.album.images.get(1).url).getBytes());
                        }
                    } else if (!playQueue.isEmpty()) {
                        playQueue.add(returnedItem);
                        mPlayer.queue(returnedItem.uri);
                    }

                }
            }
        }
    }

    public void controlSwitch(View v){
        Switch s = (Switch) findViewById(R.id.controlSwitch);
        if(connectedThread != null) {
            connectedThread.write("control".getBytes());
        }
        if(s.isChecked()){
            control = true;
        }
        if(!s.isChecked()){
            control = false;
        }
    }

    public void nextTrack(View v) throws IOException {
        if(control && connectedThread != null){
            connectedThread.write("next".getBytes());
        }else {
            Log.d("Queue: ", playQueue.isEmpty() + "");
            mPlayer.skipToNext();
            ImageButton btn = (ImageButton) findViewById(R.id.playButton);
            ImageView albumArt = (ImageView) findViewById(R.id.mainalbumart);
            TextView title = (TextView) findViewById(R.id.mainTitle);
            TextView artist = (TextView) findViewById(R.id.mainArtist);
            if (!playQueue.isEmpty()) {
                Item returnedItem = playQueue.get(playQueue.size() - 1);
                if (connectedThread != null && returnedItem != null) {
                    //connectedThread.write(("play" + "-" + returnedItem.uri + "-" + returnedItem.album.images.get(1).url + "-" + returnedItem.name + "-" + returnedItem.artists.get(0).name).getBytes());
                    if (control) {
                        connectedThread.write(("next" + "-" + returnedItem.uri + "-" + returnedItem.album.images.get(1).url + "-" + returnedItem.name + "-" + returnedItem.artists.get(0).name).getBytes());
                    }
                }
                Picasso.with(this).load(returnedItem.album.images.get(1).url).into(albumArt);
                btn.setImageResource(R.drawable.pausebutton);
                title.setText(returnedItem.name);
                artist.setText(returnedItem.artists.get(0).name);
                playQueue.remove(playQueue.size() - 1);
                if (playQueue.isEmpty()) {
                    title.setText("Chose a song");
                    artist.setText("");
                    btn.setImageResource(R.drawable.playbutton);
                    songStarted = false;
                    albumArt.setImageResource(R.drawable.emptyalbum);
                }
            }
        }
    }

    public void previousTrack(View v){
        if(control && connectedThread != null){
            connectedThread.write("previous".getBytes());
        }else {
            mPlayer.seekToPosition(0);
        }
    }

    PlayerStateCallback playerStateCallback;
    PlayerState playerState;
    boolean songStarted = false;
    boolean playing = false;
    public void playButton(View v) {
        if(control && connectedThread != null){
            connectedThread.write("playToggle".getBytes());
        }else {
            if (!songStarted) {

            } else if (songStarted) {
                //connectedThread.write("songStarted".getBytes());

                mPlayer.getPlayerState(new PlayerStateCallback() {
                    ImageButton btn = (ImageButton) findViewById(R.id.playButton);
                    @Override
                    public void onPlayerState(PlayerState playerState) {
                        if (playerState.playing) {
                            btn.setImageResource(R.drawable.playbutton);
                            mPlayer.pause();
                            playing = false;
                            if(connectedThread != null){connectedThread.write("pause".getBytes());}
                        }
                        if (!playerState.playing) {
                            btn.setImageResource(R.drawable.pausebutton);
                            mPlayer.resume();
                            playing = true;
                            if(connectedThread !=null){connectedThread.write("resume".getBytes());}
                        }
                        songMax = playerState.durationInMs;
                        songPos = playerState.positionInMs;
                        seekBar.setMax(songMax);
                        seekBar.setProgress(songPos);
                        seekUpdation();
                    }
                });

            }
        }
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


    boolean pinged = false;
long currentTime;
    long testLatency(){
            pinged = false;
            currentTime = System.currentTimeMillis();
            connectedThread.write("ping".getBytes());
            while (pinged == false) {

            }
        if(((System.currentTimeMillis()-currentTime) / 2) > 10){
            return  testLatency();
        }else {
            Log.d("Latency: ", String.valueOf((System.currentTimeMillis() - currentTime) / 2));
            return ((System.currentTimeMillis() - currentTime) / 2);
        }
    }

    String output = "";

    ConnectedThread connectedThread;

    void connectedSocket(BluetoothSocket s){

        final BluetoothSocket tempSocket = s;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BluetoothDevice tempDevice = tempSocket.getRemoteDevice();
                TextView conDevice = (TextView) findViewById(R.id.connectedDevice);
                conDevice.setText(tempDevice.getName());
            }
        });


        connectedThread = new ConnectedThread(s);
        Thread thread = new Thread(connectedThread);
        thread.start();
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
                    connectedSocket(socket);
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
            connectedSocket(mmSocket);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private class ConnectedThread implements Runnable {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()


            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    final String readMessage = new String(buffer, 0, bytes);
                    final String [] parts = readMessage.split("-");

                    output = readMessage;
                    if(parts[0].equals("play")){/*
                        if(!parts[1].equals("null")) {
                            mPlayer.play(parts[1]);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ImageView img = (ImageView) findViewById(R.id.mainalbumart);
                                    Picasso.with(MainActivity.this).load(parts[2]).into(img);
                                    TextView songTitle = (TextView) findViewById(R.id.mainTitle);
                                    TextView artist = (TextView) findViewById(R.id.mainArtist);
                                    songTitle.setText(parts[3]);
                                    artist.setText(parts[4]);
                                }
                            });
                        }else if(parts[1].equals("null")){
                            mPlayer.resume();
                        }*/
                    }

                    if(parts[0].equals("add")){
                        final Item item = new Item();
                        item.album = new Album();
                        item.album.images = new ArrayList<Image>(3);
                        item.album.images.add(new Image());
                        item.album.images.add(new Image());
                        item.album.images.add(new Image());
                        item.uri = parts[1];
                        item.album.images.get(1).url = parts[2];
                        item.name = parts[3];
                        item.artists = new ArrayList<Artist>(1);
                        item.artists.add(new Artist());
                        item.artists.get(0).name = parts[4];
                        if(playQueue.isEmpty()){
                            mPlayer.queue(item.uri);
                            songStarted = true;
                            playQueue.add(item);
                            if (connectedThread != null) {
                                connectedThread.write("playing".getBytes());
                            }
                        } else if(!playQueue.isEmpty()){
                            playQueue.add(item);
                            mPlayer.queue(item.uri);
                        }
                        //playQueue.add(item);*/
                    }
                    if(parts[0].equals("next")){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    nextTrack(findViewById(android.R.id.content));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                    if(parts[0].equals("previous")){
                        mPlayer.seekToPosition(0);
                    }

                    if(parts[0].equals("pause")){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ImageButton btn = (ImageButton) findViewById(R.id.playButton);
                                btn.setImageResource(R.drawable.playbutton);
                            }
                        });
                        //mPlayer.pause();
                    }
                    if(parts[0].equals("resume")){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ImageButton btn = (ImageButton) findViewById(R.id.playButton);
                                btn.setImageResource(R.drawable.pausebutton);
                            }
                        });
                        //mPlayer.resume();
                    }
                    if(parts[0].equals("playing")){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ImageButton btn = (ImageButton) findViewById(R.id.playButton);
                                btn.setImageResource(R.drawable.pausebutton);
                                ImageView albumArt = (ImageView) findViewById(R.id.mainalbumart);
                                Picasso.with(MainActivity.this).load(parts[2]).into(albumArt);
                            }
                        });
                    }
                    if(parts[0].equals("playToggle")){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                playButton(findViewById(android.R.id.content));
                            }
                        });
                    }
                    if(parts[0].equals("songStarted")){
                       // songStarted = true;
                    }
                    if(parts[0].equals("control")){
                        if(control = true){
                            control = false;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Switch s = (Switch) findViewById(R.id.controlSwitch);
                                    s.setChecked(false);
                                }
                            });
                        }
                    }

                    if(readMessage.equals("play")){
                        mPlayer.pause();
                        mPlayer.play("spotify:track:6HTJZ0TQJVMSKkUGzAOe2h");
                        paused = false;
                    }
                    if(readMessage.equals("ping")){
                        connectedThread.write("pong".getBytes());
                    }
                    if(readMessage.equals("pong")){
                        pinged = true;
                    }
                    Log.d("READING MESSAGE", readMessage);
                    /*

                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();*/
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

}