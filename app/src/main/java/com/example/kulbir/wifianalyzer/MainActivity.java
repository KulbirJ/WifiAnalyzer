package com.example.kulbir.wifianalyzer;



import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    TextView mainText;
    TableLayout table;
    WifiManager mainWifi;
    WifiReceiver receiverWifi;
    List<ScanResult> wifiList;
    StringBuilder sb = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainText = (TextView) findViewById(R.id.listview);
        mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        table = (TableLayout) findViewById(R.id.table);

        if (mainWifi.isWifiEnabled() == false)
        {
            // If wifi disabled then enable it
            Toast.makeText(getApplicationContext(), "wifi is disabled..making it enabled",
                    Toast.LENGTH_LONG).show();

            mainWifi.setWifiEnabled(true);
        }
        receiverWifi = new WifiReceiver();
        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mainWifi.startScan();
        mainText.setText("Starting Scan...");
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, "Refresh");
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        mainWifi.startScan();
        mainText.setText("Starting Scan");
        return super.onMenuItemSelected(featureId, item);
    }

    protected void onPause() {
        unregisterReceiver(receiverWifi);
        super.onPause();
    }

    protected void onResume() {
        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }

    protected void addTableRow(ScanResult scanResult) {
        TableRow tableRow = new TableRow(this);
        TextView tempTextView = new TextView(this);

        StringBuilder sb = new StringBuilder();

        tableRow.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.FILL_PARENT,
                AbsListView.LayoutParams.WRAP_CONTENT));

        double level = scanResult.level;
        double freq = scanResult.frequency;

        sb.append("SSID: " + scanResult.SSID + "\n");
        sb.append("Frequency: " + freq + "\n");
        sb.append("Level: " + level + "dBm\n");
        sb.append("Channel: " + convertFrequencyToChannel(scanResult.frequency) + "\n");
        sb.append("Distance: " + calculateDistance(level, freq) + "\n");

        tempTextView.setText(sb);
        tableRow.addView(tempTextView);
        table.addView(tableRow);
    }

    public static int convertFrequencyToChannel(int freq) {
        if (freq >= 2412 && freq <= 2484) {
            return (freq - 2412) / 5 + 1;
        } else if (freq >= 5170 && freq <= 5825) {
            return (freq - 5170) / 5 + 34;
        } else {
            return -1;
        }
    }

    public double calculateDistance(double levelInDb, double freqInMHz)    {
        double exp = (27.55 - (20 * Math.log10(freqInMHz)) + Math.abs(levelInDb)) / 20.0;
        return Math.pow(10.0, exp);
    }

    // Broadcast receiver class called its receive method
    // when number of wifi connections changed

    class WifiReceiver extends BroadcastReceiver {

        // This method call when number of wifi connections changed
        public void onReceive(Context c, Intent intent) {
            sb = new StringBuilder();
            wifiList = mainWifi.getScanResults();

            Collections.sort(wifiList, new Comparator<ScanResult>() {
                @Override
                public int compare(ScanResult lhs, ScanResult rhs) {

                    double lhsDistance = calculateDistance(lhs.level, lhs.frequency);
                    double rhsDistance = calculateDistance(rhs.level, rhs.frequency);

                    return lhsDistance < rhsDistance ? -1 : (lhsDistance < rhsDistance ) ? 1 : 0;
                }
            });

            for (ScanResult scanResult : wifiList) {
                Log.d("ScanResult", scanResult.toString());
                addTableRow(scanResult);
            }
            mainText.setText("");
        }

    }
}