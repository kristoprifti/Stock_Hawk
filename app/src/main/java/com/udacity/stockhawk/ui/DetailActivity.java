package com.udacity.stockhawk.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
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
    BarChart mStockPriceChart;

    @SuppressLint("SetTextI18n")
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

                        mStockPriceValue.setText(getString(R.string.dollar_sign, String.valueOf(symbolPriceValue)));
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
        List<BarEntry> entries = new ArrayList<>();
        List<String[]> lines = getLines(symbolHistory);

        final List<Long> xAxisValues = new ArrayList<>();
        int xAxisPosition = 0;

        for (int i = lines.size() - 1; i >= 0; i--) {
            String[] line = lines.get(i);

            // setup xAxis
            xAxisValues.add(Long.valueOf(line[0]));
            xAxisPosition++;

            // add entry data
            BarEntry entry = new BarEntry(xAxisPosition, // timestamp
                    Float.valueOf(line[1]) // price
            );
            entries.add(entry);
        }

        drawBarChart(symbol, entries, xAxisValues);
    }

    private void drawBarChart(String symbol, List<BarEntry> entries, final List<Long> xAxisValues) {
        Description description = new Description();
        description.setText(getString(R.string.chart_description));
        mStockPriceChart.setDescription(description);

        BarDataSet dataSet = new BarDataSet(entries, symbol);
        dataSet.setColor(Color.BLUE);

        BarData barData = new BarData(dataSet);
        mStockPriceChart.setData(barData);

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
