package fi.eerik.livelight.wear;


import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import fi.eerik.livelight.MainActivity;
import fi.eerik.livelight.R;
import fi.eerik.livelight.preset;
import fi.eerik.livelight.settings;


/**
 * Created by paulruiz on 9/26/14.
 */
public class ListenerService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks {

    /*private GoogleApiClient mApiClient;

    @Override
    public void onCreate() {

        // Build a new GoogleApiClient for the Wearable API
        mApiClient = new GoogleApiClient.Builder( this )
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();

        if( mApiClient != null && !( mApiClient.isConnected() || mApiClient.isConnecting() ) )
            mApiClient.connect();
    }

    */

    private static final String WEAR_MESSAGE_PATH = "/message";

    public static final String ACTION_TEXT_CHANGED = "changed";
    public static final String APP_PACKAGE = "fi.eerik.livelight";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        final String message = new String(messageEvent.getData());

        retrieveMessage(message);

        Log.v("wear", "Watch message received");

        // App opened
        if(message.contains("hello!"))
        {
            Log.v("wear", "App opened");
            openApp(this, APP_PACKAGE);

            /*String address = getServerAddress();

            if(address.contains(settings.lanIP))
            {
                new SendToDataLayerThread(WEAR_MESSAGE_PATH, "con:wifi").start();
                Log.v("wear", "WLAN info sent to watch");
            }
            if(address.contains(settings.wanIP))
            {
                new SendToDataLayerThread(WEAR_MESSAGE_PATH, "con:wan").start();
            }*/
        }
    }

    private void retrieveMessage(String message) {
        Intent intent = new Intent();
        intent.setAction(ACTION_TEXT_CHANGED);
        intent.putExtra("content", message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public boolean openApp(Context context, String packageName) {
        PackageManager manager = context.getPackageManager();
        Intent i = manager.getLaunchIntentForPackage(packageName);

        if (i == null) {
            return false;
            //throw new PackageManager.NameNotFoundException();
        }
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        context.startActivity(i);

        return true;
    }

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

    protected String getServerAddress () {

        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifi.getConnectionInfo();

        //TextView connectionView = (TextView) findViewById(R.id.viewConnection);

        // Check if connected to wifi
        if(info.getSSID().equals("\"" + settings.SSID + "\""))
        {
            // Unknown network, not connected
            return "http://" +  settings.lanIP + "/";
        }
        else
        {
            // Yes, connected!
            return "http://" + settings.wanIP + "/";
        }
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    class MyTask extends AsyncTask<URL, Void, String> {

        String connection = getServerAddress();
        @Override
        protected String doInBackground(URL... params) {
            String data = "";

            try {
                URL osoite = params[0];
                data = osoite.toString();
                HttpURLConnection yhteys = (HttpURLConnection) osoite.openConnection();

                try {
                    InputStream in = new BufferedInputStream(yhteys.getInputStream());
                    //data = in.toString();
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


            // Hide the loader on wear


            // If there is no connection
            if(prosessi == "error") {
                // Show error on UI

                // Let the wear know there was an error
                //sendMessage(WEAR_MESSAGE_PATH, "con:no");
            }
            else{
                // Send the connection info to wear
                if (connection.contains(settings.lanIP)) {
                    //sendMessage(WEAR_MESSAGE_PATH, "con:wifi");
                } else if (connection.contains(settings.wanIP)){
                    //sendMessage(WEAR_MESSAGE_PATH, "con:wan");
                }
            }
        }

        @Override
        protected void onPreExecute() {
            // Show loader on app


            // Show process wheel on wear

        }
    } //AsyncTask

    /*class SendToDataLayerThread extends Thread {
        String path;
        String message;

        // Constructor to send a message to the data layer
        SendToDataLayerThread(String p, String msg) {
            path = p;
            message = msg;
        }

        public void run() {
            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mApiClient).await();
            for (Node node : nodes.getNodes()) {
                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mApiClient, node.getId(), path, message.getBytes()).await();
                if (result.getStatus().isSuccess()) {
                    Log.v("myTag", "Message: {" + message + "} sent to: " + node.getDisplayName());
                }
                else {
                    // Log an error
                    Log.v("myTag", "ERROR: failed to send Message");
                }
            }
        }
    }*/
}