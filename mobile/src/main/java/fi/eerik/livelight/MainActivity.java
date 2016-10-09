package fi.eerik.livelight;

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks {

    // Global variables
    static final int DIALOG_ID = 0;
    int hour_x;
    int minute_x;
    static final String NO_CONNECTION_TEXT = "No internet connection";

    // User info
    String userName = "user";
    String userFirstName = "";
    String UserAtHome = "0";
    String alarmSet = "";

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_ID){
            return new TimePickerDialog(MainActivity.this, kTimePickerListener, hour_x, minute_x, true);
        }
        return null;
    }

    protected TimePickerDialog.OnTimeSetListener kTimePickerListener =
            new TimePickerDialog.OnTimeSetListener() {

                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute){
                    hour_x = hourOfDay;
                    minute_x = minute;

                    setAlarm(hour_x, minute_x);
                }

            };

    public static final String ACTION_TEXT_CHANGED = "changed";

    /******** Watch stuff **********/

    private static final String START_ACTIVITY = "/start_activity";
    private static final String WEAR_MESSAGE_PATH = "/message";
    private GoogleApiClient mApiClient;
    private Button mSendButton;

    private void initGoogleApiClient() {
        mApiClient = new GoogleApiClient.Builder( this )
                .addApi( Wearable.API )
                .addConnectionCallbacks( this )
                .build();

        if( mApiClient != null && !( mApiClient.isConnected() || mApiClient.isConnecting() ) )
            mApiClient.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //mApiClient.disconnect();
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        Log.v("wear", "MainActivity destroyed");
    }

    @Override
    public void onResume() {
        super.onResume();

        // Get data from server
        String link = getServerAddress() + "api.php?key="+ settings.apikey +"&status";
        try {
            // Contact server
            new MyTask().execute(new URL(link)).toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(ACTION_TEXT_CHANGED));
        Log.v("wear", "MainActivity connected");
        //sendMessage(START_ACTIVITY, "");
        wearAppOpened();
    }

    private void init() {

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

    @Override
    public void onConnectionSuspended(int i) {
        Log.v("wear", "MainActivity connection suspended");
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String content = intent.getStringExtra("content");

            if(content.contains("home")){
                atHome(true);
            }
            if(content.contains("away")){
                atHome(false);
            }
            if(content.contains("auto")){
                setPreset(0);
            }

            if(content.contains("evening")){
                setPreset(4);
            }

            if(content.contains("off")){
                setPreset(1);
            }

            if(content.contains("day")){
                setPreset(2);
            }

            // Connection check
            if(content.contains("hello!")){
                wearAppOpened();
            }
        }
    };

    // Runs when the watch app is opened
    private void wearAppOpened(){

        String connection = getServerAddress();

        TextView connectionView = (TextView) findViewById(R.id.viewConnection);

        if(connectionView.getText().toString().contains(NO_CONNECTION_TEXT)){
            sendMessage(WEAR_MESSAGE_PATH, "con:no");
        }
        else {
            if (connection.contains(settings.lanIP)) {
                sendMessage(WEAR_MESSAGE_PATH, "con:wifi");
            } else if (connection.contains(settings.wanIP))
            {
                sendMessage(WEAR_MESSAGE_PATH, "con:wan");
            }
        }
    }

    /******** Watch stuff **********/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.v("wear", "MainActivity created");

        // Watch stuff
        init();
        initGoogleApiClient();

        // Draw WIFI/Internet on UI on startup
        getServerAddress();

        // Initialize the list view
        final ListView presetList = (ListView) findViewById(R.id.listPresets);

        ArrayAdapter<preset> adapter = new ArrayAdapter<preset>(this, android.R.layout.simple_list_item_1, preset.presets);
        presetList.setAdapter(adapter);

        // Add listener to the list view
        presetList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String name = ((TextView) view).getText().toString();
                setPreset(position);
            }
        });

        // wakeup button listener
        final Button buttonSetWakeup = (Button) findViewById(R.id.buttonSetWakeup);
        buttonSetWakeup.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(DIALOG_ID);
                updateUIStatus();
            }
        });

        // home switch listener
        final Switch switchAtHome = (Switch) findViewById(R.id.switchAtHome);
        switchAtHome.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (switchAtHome.isChecked()) {
                    UserAtHome = "1";
                    atHome(true);
                } else {
                    UserAtHome = "0";
                    atHome(false);
                }
            }
        });

    } // onCreate

    public void setPreset(Integer id){

        String link = preset.presets[id].getaddress();
        String text = preset.presets[id].text();

        String test = getServerAddress() + link;

        try {
            URL osoite = new URL(test);
            new MyTask().execute(osoite);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getBaseContext(), text, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void atHome(Boolean home)
    {
        URL osoite;
        String link = getServerAddress();
        final Button buttonSetWakeup = (Button) findViewById(R.id.buttonSetWakeup);

        if (home) {
            link = link + "api.php?userStatus=home" + "&key=" + settings.apikey;
        } else {
            link = link + "api.php?userStatus=away" + "&key=" + settings.apikey;
            buttonSetWakeup.setText("Set Wakeup");
        }

        try {
            osoite = new URL(link);
            new MyTask().execute(osoite);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        onResume();
    }

    public void setAlarm (Integer hour, Integer minute){
        String nextAlarm;

        if(hour < 10){nextAlarm = "0"+ hour.toString();}
        else{nextAlarm = hour.toString();}

        if(minute < 10){nextAlarm = nextAlarm+ "+0"+ minute.toString();}
        else{nextAlarm = nextAlarm + "+" + minute.toString();}

        String link = getServerAddress() + "api.php?setAlarm&time=" + nextAlarm + "&key=" + settings.apikey;

        try {
            // Contact server
            new MyTask().execute(new URL(link));
            // Show OK message
            //Toast.makeText(MainActivity.this, "Alarm set to " + hour.toString() + ":" + minute.toString() , Toast.LENGTH_LONG).show();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        alarmSet = hour.toString() + ":" + minute.toString();
        updateUIStatus();
    }

    protected String getServerAddress () {

        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifi.getConnectionInfo();

        TextView connectionView = (TextView) findViewById(R.id.viewConnection);

        // Check if connected to wifi
        if(info.getSSID().equals("\"" + settings.SSID + "\""))
        {
            // Yes, connected!
            connectionView.setText("Connection: WIFI");
            return "http://" +  settings.lanIP + "/";
        }
        else
        {
            // Unknown network, not connected
            connectionView.setText("Connection: Internet");
            return "http://" + settings.wanIP + "/";
        }
    }

    public void updateUIStatus (){
        // Update home switch
        final Switch switchAtHome = (Switch) findViewById(R.id.switchAtHome);
        if(UserAtHome.equals("1")){
            switchAtHome.setChecked(true);
        }
        else {
            switchAtHome.setChecked(false);
        }

        // Update alarm-button
        final Button buttonSetWakeup = (Button) findViewById(R.id.buttonSetWakeup);
        if(alarmSet.equals("")){
            buttonSetWakeup.setText("Set Wakeup");
        }
        else{
            buttonSetWakeup.setText("Wakeup: "+ alarmSet);
        }

        // Users name
        TextView TextViewUserName = (TextView) findViewById(R.id.TextViewUserName);
        TextViewUserName.setText("Hey "+userFirstName+"!");

    }

    class MyTask extends AsyncTask <URL, Void, String>{

        ProgressBar loader = (ProgressBar) findViewById(R.id.progressBar);
        String connection = getServerAddress();

        @Override
        protected String doInBackground(URL... params) {
            String data = "";

            try {
                URL osoite = params[0];
                data = osoite.toString();
                HttpURLConnection yhteys = (HttpURLConnection) osoite.openConnection();


                try {
                    //InputStream in = new BufferedInputStream(yhteys.getInputStream());
                    BufferedInputStream in = new BufferedInputStream(yhteys.getInputStream());
                    byte[] contents = new byte[1024];

                    int bytesRead = 0;
                    String strFileContents = "";

                    while((bytesRead = in.read(contents)) != -1)
                    {
                        strFileContents += new String(contents, 0, bytesRead);
                    }

                    //data = in.toString();
                    data = strFileContents;
                }
                finally {
                    yhteys.disconnect();
                }

            } catch (IOException e) {
                e.printStackTrace();
                return "error";
            }
            return data;
        }

        @Override
        protected void onPostExecute(String prosessi) {

            // Hide the loader on app
            loader.setIndeterminate(false);

            // Hide the loader on wear
            sendMessage(WEAR_MESSAGE_PATH, "hideLoader");

            // If there is no connection
            // if settings message is received
            if(prosessi == "error") {
                // Show error on UI
                TextView connectionView = (TextView) findViewById(R.id.viewConnection);
                connectionView.setText(NO_CONNECTION_TEXT);
                Toast.makeText(MainActivity.this, "Error connecting to LiveLight Server", Toast.LENGTH_LONG).show();

                // Let the wear know there was an error
                sendMessage(WEAR_MESSAGE_PATH, "con:no");
            }
            else if (prosessi.endsWith("</settings>")) {

                String xml = prosessi;

                //User's name
                String tag = "userName";
                int start = xml.indexOf("<" + tag + ">") + tag.length() + 2;
                int stop = xml.indexOf("</" + tag + ">");
                userName = xml.substring(start, stop);

                //User's first name
                userFirstName = userName.substring(0, userName.indexOf(" "));

                //User's status
                tag = "UserAtHome";
                start = xml.indexOf("<" + tag + ">") + tag.length() + 2;
                stop = xml.indexOf("</" + tag + ">");
                UserAtHome = xml.substring(start, stop);

                //Alarm set time
                tag = "alarmSet";
                start = xml.indexOf("<" + tag + ">") + tag.length() + 2;
                stop = xml.indexOf("</" + tag + ">");
                alarmSet = xml.substring(start, stop);
                // Time picker values

                if(alarmSet.length()>4)
                {
                    hour_x = Integer.parseInt(alarmSet.substring(0, 2));
                    minute_x = Integer.parseInt(alarmSet.substring(3, 5));
                }
                else
                {
                    hour_x = 9;
                    minute_x = 0;
                }



                updateUIStatus();
            } else {

                // Show the feedback from server
                Toast.makeText(MainActivity.this, prosessi, Toast.LENGTH_LONG).show();
                // Send the connection info to wear
                if (connection.contains(settings.lanIP)) {
                    sendMessage(WEAR_MESSAGE_PATH, "con:wifi");
                } else if (connection.contains(settings.wanIP)) {
                    sendMessage(WEAR_MESSAGE_PATH, "con:wan");
                }
            }
        }

        @Override
        protected void onPreExecute() {
            // Show loader on app
            loader.setIndeterminate(true);

            // Show process wheel on wear
            sendMessage(WEAR_MESSAGE_PATH, "showLoader");
        }

    } //AsyncTask

} //MainActivity
