package com.fgtit.fingerprintattendance.activity;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.fgtit.fingerprintattendance.R;
import com.fgtit.fingerprintattendance.adapter.EnrollAdapter;
import com.fgtit.fingerprintattendance.app.ConnectionDetector;
import com.fgtit.fingerprintattendance.doa.EnrollDataSource;
import com.fgtit.fingerprintattendance.model.EnrollItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RecordsActivity extends AppCompatActivity {

    private static final String TAG = RecordsActivity.class.getSimpleName();
    private ConnectionDetector cd;
    private EnrollDataSource enrollDataSource;

    private RecyclerView rView;
    private List<EnrollItem> enrollItems;
    private EnrollAdapter enrollAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private boolean isRefreshing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_records);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);

        Toolbar toolbar = (Toolbar) findViewById(R.id.records_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        enrollDataSource = new EnrollDataSource(this);
        enrollItems = new ArrayList<EnrollItem>();
        cd = new ConnectionDetector(this);

        LinearLayoutManager lLayout = new LinearLayoutManager(this);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.records_swipe_refresh_layout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        rView = (RecyclerView) findViewById(R.id.records_recycler_view);
        rView.setLayoutManager(lLayout);
        rView.setHasFixedSize(true);

        setRecordsAdapter();

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (cd.isConnectingToInternet()) {
                    isRefreshing = true;
                    loadRecords(String.valueOf(0));
                } else {
                    mSwipeRefreshLayout.setRefreshing(false);
                    showSnackBar(0, getString(R.string.err_no_internet));
                }
            }
        });
    }


    private void setRecordsAdapter() {
        enrollAdapter = new EnrollAdapter(this, rView, enrollItems);
        rView.setAdapter(enrollAdapter);

        enrollAdapter.setOnLoadMoreListener(new EnrollAdapter.OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                if (!isRefreshing) {
                    //add progress item
                    enrollItems.add(null);
                    enrollAdapter.notifyItemInserted(enrollItems.size() - 1);

                    if (cd.isConnectingToInternet()) {
                        loadRecords(String.valueOf(enrollAdapter.getItemCount() - 1));
                    } else showSnackBar(1, getString(R.string.err_no_internet));
                }
            }
        });

        loadLocalRecords();
        /*if (cd.isConnectingToInternet()) {
            isRefreshing = true;
            mSwipeRefreshLayout.post(new Runnable() {
                @Override public void run() {
                    mSwipeRefreshLayout.setRefreshing(true);
                }
            });
            loadExpenseSummary(String.valueOf(0));
        }else showSnackBar(0, getString(R.string.err_no_internet));*/

    }

    private  void loadLocalRecords() {
        enrollDataSource.open();
        List<EnrollItem> items = enrollDataSource.fetchAllEnroll();
        enrollDataSource.close();
        enrollItems.clear();
        for (EnrollItem item: items) {
           enrollItems.add(item);
        }
        enrollAdapter.notifyDataSetChanged();
    }

    private  void loadRecords(String pos) {

    }

    //SnackBar function
    private void showSnackBar(final int id, String msg) {
        CoordinatorLayout coordinatorLayout = (CoordinatorLayout)
                findViewById(R.id.records_coordinator_layout);

        Snackbar snackbar = Snackbar
                .make(coordinatorLayout, msg, Snackbar.LENGTH_LONG);

        // Changing message text color
        snackbar.setActionTextColor(Color.RED);

        // Changing action button text color
        View sbView = snackbar.getView();
        TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(Color.YELLOW);
        snackbar.show();

    }

}
