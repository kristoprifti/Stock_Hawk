package com.udacity.stockhawk.ui;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;

import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import au.com.bytecode.opencsv.CSVReader;
import butterknife.BindView;
import butterknife.ButterKnife;

public class DetailActivity extends AppCompatActivity {

    public static String EXTRA_SYMBOL = "SYMBOL";

    @BindView(R.id.stockPriceValue)
    TextView mStockPriceValue;
    @BindView(R.id.stockAbsoluteChange)
    TextView mStockAbsoluteChange;
    @BindView(R.id.stockPercentageChange)
    TextView mStockPercentageChange;
    @BindView(R.id.stockPriceChart)
    LineChart mStockPriceChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        ButterKnife.bind(this);

        Intent intent = getIntent();
        if (intent != null)
        {
            if (intent.hasExtra(EXTRA_SYMBOL)) {
                String symbol = intent.getStringExtra(EXTRA_SYMBOL);
                if (symbol != null) {
                    Cursor cursor = getContentResolver().query(Contract.Quote.makeUriForStock(symbol),
                            null,
                            null,
                            null,
                            null);

                    if (cursor != null && cursor.moveToFirst()) {
                        String symbolName = cursor.getString(cursor.getColumnIndex(Contract.Quote.COLUMN_NAME));
                        String symbolHistory = cursor.getString(cursor.getColumnIndex(Contract.Quote.COLUMN_HISTORY));
                        double symbolPriceValue = Math.round(cursor.getDouble(cursor.getColumnIndex(Contract.Quote.COLUMN_PRICE)) * 100.0) / 100.0;
                        double symbolAbsoluteChange = Math.round(cursor.getDouble(cursor.getColumnIndex(Contract.Quote.COLUMN_ABSOLUTE_CHANGE)) * 100.0) / 100.0;
                        double symbolPercentageChange = Math.round(cursor.getDouble(cursor.getColumnIndex(Contract.Quote.COLUMN_PERCENTAGE_CHANGE)) * 100.0) / 100.0;

                        if(getSupportActionBar() != null)
                            getSupportActionBar().setTitle(symbolName + " (" + symbol + ")");

                        mStockPriceValue.setText(String.valueOf(symbolPriceValue));
                        mStockAbsoluteChange.setText(getString(R.string.dollar_sign, String.valueOf(symbolAbsoluteChange)));
                        mStockPercentageChange.setText(getString(R.string.percentage_sign, String.valueOf(symbolPercentageChange)));

                        createGraphFromHistory(symbol, symbolHistory);

                        cursor.close();
                    }
                }
            }
        }
    }

    private void createGraphFromHistory(String symbol, String symbolHistory) {
        List<Entry> entries = new ArrayList<>();
        List<String[]> lines = getLines(symbolHistory);

        final List<Long> xAxisValues = new ArrayList<>();
        int xAxisPosition = 0;

        for (int i = lines.size() - 1; i >= 0; i--) {
            String[] line = lines.get(i);

            // setup xAxis
            xAxisValues.add(Long.valueOf(line[0]));
            xAxisPosition++;

            // add entry data
            Entry entry = new Entry(xAxisPosition, // timestamp
                    Float.valueOf(line[1]) // price
            );
            entries.add(entry);
        }

        drawChart(symbol, entries, xAxisValues);
    }

    private void drawChart(String symbol, List<Entry> entries, final List<Long> xAxisValues) {
        Description description = new Description();
        description.setText("");
        mStockPriceChart.setDescription(description);

        LineDataSet dataSet = new LineDataSet(entries, symbol);
        dataSet.setColor(Color.RED);

        LineData lineData = new LineData(dataSet);
        mStockPriceChart.setData(lineData);

        XAxis xAxis = mStockPriceChart.getXAxis();
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                Date date = new Date(xAxisValues.get(xAxisValues.size() - (int) value - 1));
                return (new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(date));
            }
        });

        mStockPriceChart.invalidate();
    }

    private List<String[]> getLines(String symbolHistory) {
        List<String[]> lines = null;
        CSVReader csvReader = new CSVReader(new StringReader(symbolHistory));
        try {
            lines = csvReader.readAll();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }
}
