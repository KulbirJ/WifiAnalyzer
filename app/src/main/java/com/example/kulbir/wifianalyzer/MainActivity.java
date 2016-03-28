package com.example.kulbir.wifianalyzer;



import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;

public class MainActivity extends Activity {
    TextView mainText;
    TableLayout table;
    WifiManager mainWifi;
    WifiReceiver receiverWifi;
    List<ScanResult> wifiList;
    GraphView twoGhzGraph;
    StringBuilder sb = new StringBuilder();

    int[] twoGhzChannels = new int[14];
    SparseIntArray fiveGhzChannels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainText = (TextView) findViewById(R.id.listview);
        mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        table = (TableLayout) findViewById(R.id.table);
        twoGhzGraph = (GraphView) findViewById(R.id.graph);

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
        int channel = convertFrequencyToChannel(scanResult.frequency);

        sb.append("SSID: " + scanResult.SSID + "\n");
        sb.append("Frequency: " + freq + "\n");
        sb.append("Level: " + level + "dBm\n");
        sb.append("Channel: " + channel + "\n");
        sb.append("Distance: " + calculateDistance(level, freq) + "\n");

        LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(new DataPoint[] {
                new DataPoint(channel - 2, -100),
                new DataPoint(channel - 1, (-100 + level)/2.0),
                new DataPoint(channel, level),
                new DataPoint(channel + 1, (-100 + level)/2.0),
                new DataPoint(channel + 2, -100)
        });
        twoGhzGraph.addSeries(series);

        tempTextView.setText(sb);

        tableRow.addView(tempTextView);
        table.addView(tableRow);
    }

    public int convertFrequencyToChannel(int freq) {
        int channel = -1;
        if (freq >= 2412 && freq <= 2484) {
            channel = (freq - 2412) / 5 + 1;
            twoGhzChannels[channel - 1]++;
        } else if (freq >= 5170 && freq <= 5825) {
            channel = (freq - 5170) / 5 + 34;
            if(fiveGhzChannels.indexOfKey(channel) < 0) {
                fiveGhzChannels.put(channel, 1);
            } else {
                int channelCount = fiveGhzChannels.get(channel) + 1;
                fiveGhzChannels.put(channel, channelCount);
            }
        }
        return channel;
    }

    private void getCurrentSSID(Context context) {
        StringBuilder CurrentSSID = new StringBuilder();
        TableRow tableRow = new TableRow(this);
        TextView tempTextView = new TextView(this);

        try {
            WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

            WifiInfo info = wifi.getConnectionInfo();

            double level = info.getRssi();
            double freq = info.getFrequency();
            CurrentSSID.append("Your SSID: " + info.getSSID() + "\n");
            CurrentSSID.append("Frequency: " + info.getFrequency() + "\n");
            CurrentSSID.append("Level: " + info.getRssi() + "dBm\n");
            CurrentSSID.append("Channel: " + convertFrequencyToChannel(info.getFrequency()) + "\n");
            CurrentSSID.append("Distance: " + calculateDistance(level, freq) + "\n");
            tempTextView.setText(CurrentSSID);
            tableRow.addView(tempTextView);
            Resources resource = context.getResources();
            tableRow.setBackgroundColor(resource.getColor(R.color.blue));
            table.addView(tableRow);
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    public double calculateDistance(double levelInDb, double freqInMHz)    {
        double exp = (27.55 - (20 * Math.log10(freqInMHz)) + Math.abs(levelInDb)) / 20.0;
        return Math.pow(10.0, exp);
    }

    public void resetTwoGhzChannelCount() {
        for(int i=0; i<14; i++) {
            twoGhzChannels[i] = 0;
        }
    }

    public void resetFiveGhzChannelCount() {
        fiveGhzChannels = new SparseIntArray();
    }

    public void displayBestChannels() {
        int bestTwoGhzChannel = 0;
        int bestFiveGhzChannel;

        if(twoGhzChannels[bestTwoGhzChannel] > twoGhzChannels[5]) {
            bestTwoGhzChannel = 5;
        }
        if (twoGhzChannels[bestTwoGhzChannel] > twoGhzChannels[10]) {
            bestTwoGhzChannel = 10;
        }

        if (fiveGhzChannels.size() == 0) {
            mainText.setText("5GHz: N/A         2GHz: " + Integer.toString(bestTwoGhzChannel + 1));
        } else {
            bestFiveGhzChannel = 0;
            for (int i = 1; i < fiveGhzChannels.size(); i++) {
                if (fiveGhzChannels.get(bestFiveGhzChannel) > fiveGhzChannels.get(i)) {
                    bestFiveGhzChannel = i;
                }
            }
            mainText.setText("Set 5GHz to: " + Integer.toString(fiveGhzChannels.keyAt(bestFiveGhzChannel))
                    + "  Set 2GHz to: " + Integer.toString(bestTwoGhzChannel + 1));
        }
    }

    // Broadcast receiver class called its receive method
    // when number of wifi connections changed
    class WifiReceiver extends BroadcastReceiver {

        // This method call when number of wifi connections changed
        public void onReceive(Context c, Intent intent) {
            table.removeAllViews();
            twoGhzGraph.removeAllSeries();
            resetTwoGhzChannelCount();
            resetFiveGhzChannelCount();

            wifiList = mainWifi.getScanResults();

            Collections.sort(wifiList, new Comparator<ScanResult>() {
                @Override
                public int compare(ScanResult lhs, ScanResult rhs) {

                    double lhsDistance = calculateDistance(lhs.level, lhs.frequency);
                    double rhsDistance = calculateDistance(rhs.level, rhs.frequency);

                    return lhsDistance < rhsDistance ? -1 : (lhsDistance < rhsDistance) ? 1 : 0;
                }
            });

            getCurrentSSID(c);

            for (ScanResult scanResult : wifiList) {
                Log.d("ScanResult", scanResult.toString());
                addTableRow(scanResult);
            }

            displayBestChannels();

            for(int i=0; i<14; i++) {
                Log.d("2GHz Channel " + (i + 1), Integer.toString(twoGhzChannels[i]));
            }
            for(int i=0; i<fiveGhzChannels.size(); i++) {
                Log.d("5 GHz" + fiveGhzChannels.keyAt(i), Integer.toString(fiveGhzChannels.get(i)));
            }
        }

    }
}