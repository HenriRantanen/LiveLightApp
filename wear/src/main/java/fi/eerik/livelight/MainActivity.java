package fi.eerik.livelight;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

public class MainActivity extends Activity implements MessageApi.MessageListener, GoogleApiClient.ConnectionCallbacks {

    private TextView mTextView;
    private static final String WEAR_MESSAGE_PATH = "/message";
    private GoogleApiClient mApiClient;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);


                // Away button listener
                ImageButton buttonAway = (ImageButton)stub.findViewById(R.id.buttonAway);
                buttonAway.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Toast.makeText(getApplicationContext(), "Poissa", Toast.LENGTH_LONG).show();
                        sendMessage(WEAR_MESSAGE_PATH, "away");
                    }
                });

                // Away button listener
                ImageButton buttonHome = (ImageButton)stub.findViewById(R.id.buttonHome);
                buttonHome.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Toast.makeText(getApplicationContext(),"Kotona",Toast.LENGTH_LONG).show();
                        sendMessage(WEAR_MESSAGE_PATH, "home");
                    }
                });

                // Auto button listener
                ImageButton buttonAuto = (ImageButton)stub.findViewById(R.id.buttonAuto);
                buttonAuto.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Toast.makeText(getApplicationContext(),"Auto",Toast.LENGTH_LONG).show();
                        sendMessage(WEAR_MESSAGE_PATH, "auto");
                    }
                });

                // evening button listener
                ImageButton buttonEvening = (ImageButton)stub.findViewById(R.id.buttonEvening);
                buttonEvening.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Toast.makeText(getApplicationContext(),"Evening",Toast.LENGTH_LONG).show();
                        sendMessage(WEAR_MESSAGE_PATH, "evening");
                    }
                });

                // off button listener
                ImageButton buttonOff = (ImageButton)stub.findViewById(R.id.buttonOff);
                buttonOff.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Toast.makeText(getApplicationContext(), "Off", Toast.LENGTH_LONG).show();
                        sendMessage(WEAR_MESSAGE_PATH, "off");
                    }
                });

                // day button listener
                ImageButton buttonDay = (ImageButton)stub.findViewById(R.id.buttonDay);
                buttonDay.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Toast.makeText(getApplicationContext(), "Daylight", Toast.LENGTH_LONG).show();
                        sendMessage(WEAR_MESSAGE_PATH, "day");
                    }
                });

                //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                initGoogleApiClient();
            }

        });
    }

    private void initGoogleApiClient() {
        mApiClient = new GoogleApiClient.Builder( this )
                .addApi( Wearable.API )
                .addConnectionCallbacks( this )
                .build();

        if( mApiClient != null && !( mApiClient.isConnected() || mApiClient.isConnecting() ) )
            mApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if( mApiClient != null && !( mApiClient.isConnected() || mApiClient.isConnecting() ) )
            mApiClient.connect();


    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    protected void connectinCheck(){
        sendMessage(WEAR_MESSAGE_PATH, "hello!");
    }

    @Override
    public void onMessageReceived( final MessageEvent messageEvent ) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String message = new String(messageEvent.getData());
                ProgressBar loader = (ProgressBar) findViewById(R.id.progressBar);
                ImageView imageConnection = (ImageView) findViewById(R.id.imageConnection);

                if (messageEvent.getPath().equalsIgnoreCase(WEAR_MESSAGE_PATH)) {

                    // Show loader
                    if (message.contains("showLoader")){
                        loader.setIndeterminate(true);
                        loader.setVisibility(View.VISIBLE);
                    }
                    // Hide loader
                    if (message.contains("hideLoader")){
                        loader.setIndeterminate(false);
                        loader.setVisibility(View.INVISIBLE);
                    }
                    // Show Wifi icon
                    if (message.contains("con:wifi")){
                        imageConnection.setImageResource(R.drawable.ic_network_wifi_white_24dp);
                    }
                    // Show Cell icon
                    if (message.contains("con:wan")){
                        imageConnection.setImageResource(R.drawable.ic_network_cell_white_24dp);
                    }
                    // Show no internet icon
                    if (message.contains("con:no")){
                        imageConnection.setImageResource(R.drawable.ic_signal_cellular_connected_no_internet_4_bar_white_24dp);
                        Toast.makeText(MainActivity.this, "Error contacting the server", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.MessageApi.addListener(mApiClient, this);
        connectinCheck();
    }

    @Override
    protected void onStop() {
        if ( mApiClient != null ) {
            Wearable.MessageApi.removeListener( mApiClient, this );
            if ( mApiClient.isConnected() ) {
                mApiClient.disconnect();
            }
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if( mApiClient != null )
            mApiClient.unregisterConnectionCallbacks(this );
        super.onDestroy();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    private void sendMessage(final String path, final String text) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( mApiClient ).await();
                for(Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mApiClient, node.getId(), path, text.getBytes() ).await();
                }

                runOnUiThread( new Runnable() {
                    @Override
                    public void run() {
                        //mEditText.setText( "" );
                    }
                });
            }
        }).start();
    }
}
