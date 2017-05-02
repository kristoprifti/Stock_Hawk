package com.udacity.stockhawk.widget;

import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.ui.DetailActivity;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Created by k.prifti on 28.3.2017 г..
 */

public class StockWidgetRemoteViewsService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsService.RemoteViewsFactory() {

            private DecimalFormat dollarFormatWithPlus = null;
            private DecimalFormat dollarFormat = null;
            private DecimalFormat percentageFormat = null;
            private Cursor mCursor;

            @Override
            public void onCreate () {
                //Timber.d("RemoteViewsFactory - onCreate");
                dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
                dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
                dollarFormatWithPlus.setPositivePrefix("+$");
                percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
                percentageFormat.setMaximumFractionDigits(2);
                percentageFormat.setMinimumFractionDigits(2);
                percentageFormat.setPositivePrefix("+");
            }

            @Override
            public void onDataSetChanged () {
                // refresh
                if (mCursor != null)
                    mCursor.close();

                // Credit: https://github.com/udacity/Advanced_Android_Development/blob/master/app/src/main/java/com/example/android/sunshine/app/widget/DetailWidgetRemoteViewsService.java#L60
                // This method is called by the app hosting the widget (e.g., the launcher)
                // However, our ContentProvider is not exported so it doesn't have access to the
                // data. Therefore we need to clear (and finally restore) the calling identity so
                // that calls use our process and permission
                final long identityToken = Binder.clearCallingIdentity();

                mCursor = getContentResolver().query(
                        Contract.Quote.URI,
                        Contract.Quote.QUOTE_COLUMNS.toArray(new String[]{}),
                        null,
                        null,
                        null);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy () {
                if (mCursor != null) {
                    mCursor.close();
                    mCursor = null;
                }
            }

            @Override
            public int getCount () {
                return mCursor == null ? 0 : mCursor.getCount();
            }

            @Override
            public RemoteViews getViewAt (int position) {
                if (position == AdapterView.INVALID_POSITION || mCursor == null || !mCursor.moveToPosition(position)) {
                    return null;
                }

                RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.stock_widget_list_item);

                String symbol = mCursor.getString(mCursor.getColumnIndex(Contract.Quote.COLUMN_SYMBOL));
                float price = mCursor.getFloat(mCursor.getColumnIndex(Contract.Quote.COLUMN_PRICE));
                float priceChange = mCursor.getFloat(mCursor.getColumnIndex(Contract.Quote.COLUMN_ABSOLUTE_CHANGE));
                float percentPriceChange = mCursor.getFloat(mCursor.getColumnIndex(Contract.Quote.COLUMN_PERCENTAGE_CHANGE));

                if (priceChange > 0) {
                    remoteViews.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_green);
                } else {
                    remoteViews.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_red);
                }

                if (PrefUtils.getDisplayMode(StockWidgetRemoteViewsService.this).equals(StockWidgetRemoteViewsService.this.getString(R.string.pref_display_mode_absolute_key))) {
                    remoteViews.setTextViewText(R.id.change, dollarFormatWithPlus.format(priceChange));
                } else {
                    remoteViews.setTextViewText(R.id.change, percentageFormat.format(percentPriceChange / 100));
                }

                remoteViews.setTextViewText(R.id.symbol, symbol);
                remoteViews.setTextViewText(R.id.price, dollarFormat.format(price));


                //Timber.d("RemoteViewsFactory - getViewAt " + position + "symbol: " + symbol + " price: " + price + " priceChange " + priceChange);

                Intent intent = new Intent(getApplicationContext(), DetailActivity.class);
                intent.putExtra(DetailActivity.EXTRA_SYMBOL, symbol);
                remoteViews.setOnClickFillInIntent(R.id.stock_widget_list_item_id, intent);

                return remoteViews;
            }

            @Override
            public RemoteViews getLoadingView () {
                return new RemoteViews(getPackageName(), R.layout.stock_widget_list_item);
            }

            @Override
            public int getViewTypeCount () {
                return 1;
            }

            @Override
            public long getItemId (int position) {
                if (mCursor.moveToPosition(position))
                    return mCursor.getLong(Contract.Quote.POSITION_ID);
                return position;
            }

            @Override
            public boolean hasStableIds () {
                return true;
            }
        };
    }
}