package com.example.kulbir.wifianalyzer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class TwoGhzGraphActivity extends Activity {

    WifiManager mainWifi;
    WifiReceiver receiverWifi;
    List<ScanResult> wifiList;
    private XYPlot twoGhzGraph;
    Random rand = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_two_ghz_graph);

        mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

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
    }

    protected void onPause() {
        unregisterReceiver(receiverWifi);
        super.onPause();
    }

    private void addResultToGraph(int channel, double level, final String ssid) {
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

    public int getRandomColour() {
        return Color.rgb(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
    }

    public int convertFrequencyToChannel(int freq) {
        int channel = -1;
        if (freq >= 2412 && freq <= 2484) {
            channel = (freq - 2412) / 5 + 1;
        }
        return channel;
    }

    class WifiReceiver extends BroadcastReceiver {

        // This method call when number of wifi connections changed
        public void onReceive(Context c, Intent intent) {
            twoGhzGraph.getSeriesRegistry().clear();
            int channel;

            wifiList = mainWifi.getScanResults();

            for (ScanResult scanResult : wifiList) {
                channel = convertFrequencyToChannel(scanResult.frequency);
                if(channel != -1) {
                    addResultToGraph(channel, scanResult.level, scanResult.SSID);
                }
            }

            twoGhzGraph.redraw();
        }
    }
}
