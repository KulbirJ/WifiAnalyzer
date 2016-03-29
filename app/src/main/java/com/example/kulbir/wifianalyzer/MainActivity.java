package com.example.kulbir.wifianalyzer;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.CatmullRomInterpolator;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.PointLabeler;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYStepMode;

public class MainActivity extends ActionBarActivity {
    TextView mainText;
    TableLayout table;
    WifiManager mainWifi;
    WifiReceiver receiverWifi;
    List<ScanResult> wifiList;
    private XYPlot twoGhzGraph;
    private int lastColor = 0;
    Random rand = new Random();

    int[] twoGhzChannels = new int[14];
    SparseIntArray fiveGhzChannels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainText = (TextView) findViewById(R.id.listview);
        mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        table = (TableLayout) findViewById(R.id.table);

        twoGhzGraph = (XYPlot) findViewById(R.id.twoGhzGraph);
        twoGhzGraph.getLayoutManager().remove(twoGhzGraph.getLegendWidget());

        twoGhzGraph.setDomainStep(XYStepMode.INCREMENT_BY_VAL, 1);
        twoGhzGraph.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 10);
        twoGhzGraph.setDomainValueFormat(new DecimalFormat("#"));
        twoGhzGraph.setRangeValueFormat(new DecimalFormat("###"));

        twoGhzGraph.getGraphWidget().setMarginBottom(75);
        twoGhzGraph.getGraphWidget().setMarginLeft(90);
        twoGhzGraph.getGraphWidget().setPaddingRight(20);
        twoGhzGraph.getGraphWidget().setPaddingTop(40);

        twoGhzGraph.setUserRangeOrigin(0);
        twoGhzGraph.setRangeBoundaries(-100, BoundaryMode.FIXED, -20, BoundaryMode.FIXED);
        twoGhzGraph.setDomainBoundaries(-1, BoundaryMode.FIXED, 14, BoundaryMode.FIXED);

        twoGhzGraph.setVisibility(View.INVISIBLE);


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
        if (id == R.id.twoghz) {
            startActivity(new Intent(this, TwoGhzGraphActivity.class));

            return true;
        }else if (id == R.id.fiveghz) {
            startActivity(new Intent(this, FiveGhzGraphActivity.class));

            return true;
        }


        return super.onOptionsItemSelected(item);
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
        String ssid = scanResult.SSID;

        sb.append("SSID: " + ssid + "\n");
        sb.append("Frequency: " + freq + "\n");
        sb.append("Level: " + level + "dBm\n");
        sb.append("Channel: " + channel + "\n");
        sb.append("Distance: " + calculateDistance(level, freq) + "\n");

        if(channel <= 14) {
            addChannelToTwoGhzGraph(channel, level, ssid);
        }

        tempTextView.setText(sb);

        tableRow.addView(tempTextView);
        table.addView(tableRow);
    }

    public void addChannelToTwoGhzGraph(int channel, double level, final String ssid) {
        Number[] x = {channel - 1, channel, channel + 1};
        Number[] y = {-100, level, -100};

        XYSeries data = new SimpleXYSeries(Arrays.asList(x), Arrays.asList(y), ssid);

        PointLabeler labeler = new PointLabeler() {
            @Override
            public String getLabel(XYSeries series, int index) {
                if(index == 1) {
                    return ssid;
                } else {
                    return "";
                }
            }
        };

        int color = getRandomColour();

        LineAndPointFormatter format = new LineAndPointFormatter(
                color,
                null,                                   // point color
                color,                                   // fill color (none)
                new PointLabelFormatter(Color.WHITE));

        format.setInterpolationParams(
                new CatmullRomInterpolator.Params(20, CatmullRomInterpolator.Type.Centripetal));

        format.setPointLabeler(labeler);

        twoGhzGraph.addSeries(data, format);
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

    public double calculateDistance(double levelInDb, double freqInMHz) {
        double exp = (27.55 - (20 * Math.log10(freqInMHz)) + Math.abs(levelInDb)) / 20.0;
        return Math.pow(10.0, exp);
    }

    public void resetTwoGhzChannelCount() {
        for (int i = 0; i < 14; i++) {
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
                if (fiveGhzChannels.valueAt(bestFiveGhzChannel) > fiveGhzChannels.valueAt(i)) {
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
            twoGhzGraph.getSeriesRegistry().clear();
            twoGhzGraph.setVisibility(View.INVISIBLE);

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

            twoGhzGraph.redraw();

            displayBestChannels();
            twoGhzGraph.setVisibility(View.VISIBLE);

            for(int i=0; i<14; i++) {
                Log.d("2GHz Channel " + (i + 1), Integer.toString(twoGhzChannels[i]));
            }
            for(int i=0; i<fiveGhzChannels.size(); i++) {
                Log.d("5 GHz Channel " + fiveGhzChannels.keyAt(i), Integer.toString(fiveGhzChannels.valueAt(i)));
            }
        }

    }

    public int getRandomColour() {
        return Color.rgb(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
    }
}