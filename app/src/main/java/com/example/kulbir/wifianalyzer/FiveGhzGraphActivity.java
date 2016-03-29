package com.example.kulbir.wifianalyzer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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

public class FiveGhzGraphActivity extends ActionBarActivity {

    WifiManager mainWifi;
    WifiReceiver receiverWifi;
    List<ScanResult> wifiList;
    private XYPlot fiveGhzGraph;
    Random rand = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_five_ghz_graph);

        mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        fiveGhzGraph = (XYPlot) findViewById(R.id.twoGhzGraph);
        fiveGhzGraph.getLayoutManager().remove(fiveGhzGraph.getLegendWidget());

        fiveGhzGraph.setDomainStep(XYStepMode.INCREMENT_BY_VAL, 2);
        fiveGhzGraph.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 10);
        fiveGhzGraph.setDomainValueFormat(new DecimalFormat("#"));
        fiveGhzGraph.setRangeValueFormat(new DecimalFormat("###"));

        fiveGhzGraph.getGraphWidget().setMarginBottom(75);
        fiveGhzGraph.getGraphWidget().setMarginLeft(90);
        fiveGhzGraph.getGraphWidget().setPaddingRight(20);
        fiveGhzGraph.getGraphWidget().setPaddingTop(40);

        fiveGhzGraph.setUserRangeOrigin(0);
        fiveGhzGraph.setRangeBoundaries(-100, BoundaryMode.FIXED, -20, BoundaryMode.FIXED);
        fiveGhzGraph.setDomainBoundaries(32, BoundaryMode.FIXED, 52, BoundaryMode.FIXED);

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

    protected void onResume() {
        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }

    protected void onPause() {
        unregisterReceiver(receiverWifi);
        super.onPause();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_fiveghz, menu);
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
        }else if (id == R.id.home) {
            startActivity(new Intent(this, MainActivity.class));

            return true;
        }


        return super.onOptionsItemSelected(item);
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

        fiveGhzGraph.addSeries(data, format);
    }

    public int getRandomColour() {
        return Color.rgb(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
    }

    public int convertFrequencyToChannel(int freq) {
        int channel = -1;
        if (freq >= 5170 && freq <= 5825) {
            channel = (freq - 5170) / 5 + 34;
        }
        return channel;
    }

    class WifiReceiver extends BroadcastReceiver {

        // This method call when number of wifi connections changed
        public void onReceive(Context c, Intent intent) {
            fiveGhzGraph.getSeriesRegistry().clear();
            int channel;

            wifiList = mainWifi.getScanResults();

            for (ScanResult scanResult : wifiList) {
                channel = convertFrequencyToChannel(scanResult.frequency);
                Log.d("Channel: " + channel, scanResult.SSID);
                if(channel != -1) {
                    addResultToGraph(channel, scanResult.level, scanResult.SSID);
                }
            }

            fiveGhzGraph.redraw();
        }
    }
}
