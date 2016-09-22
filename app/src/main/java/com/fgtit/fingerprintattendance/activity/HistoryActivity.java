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
import com.fgtit.fingerprintattendance.adapter.CaptureAdapter;
import com.fgtit.fingerprintattendance.app.ConnectionDetector;
import com.fgtit.fingerprintattendance.model.CaptureItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private static final String TAG = HistoryActivity.class.getSimpleName();
    private ConnectionDetector cd;

    private RecyclerView rView;
    private List<CaptureItem> captureItems;
    private CaptureAdapter captureAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private boolean isRefreshing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);

        Toolbar toolbar = (Toolbar) findViewById(R.id.history_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        cd = new ConnectionDetector(this);

        LinearLayoutManager lLayout = new LinearLayoutManager(this);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.history_swipe_refresh_layout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        rView = (RecyclerView) findViewById(R.id.history_recycler_view);
        rView.setLayoutManager(lLayout);
        rView.setHasFixedSize(true);

        setHistoryAdapter();

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (cd.isConnectingToInternet()) {
                    isRefreshing = true;
                    loadHistory(String.valueOf(0));
                } else {
                    mSwipeRefreshLayout.setRefreshing(false);
                    showSnackBar(0, getString(R.string.err_no_internet));
                }
            }
        });
    }


    private void setHistoryAdapter() {
        captureAdapter = new CaptureAdapter(this, rView, captureItems);
        rView.setAdapter(captureAdapter);

        captureAdapter.setOnLoadMoreListener(new CaptureAdapter.OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                if (!isRefreshing) {
                    //add progress item
                    captureItems.add(null);
                    captureAdapter.notifyItemInserted(captureItems.size() - 1);

                    if (cd.isConnectingToInternet()) {
                        loadHistory(String.valueOf(captureAdapter.getItemCount()-1));
                    }else showSnackBar(1, getString(R.string.err_no_internet));
                }
            }
        });

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



    private  void loadHistory(String pos) {

    }

    //SnackBar function
    private void showSnackBar(final int id, String msg) {
        CoordinatorLayout coordinatorLayout = (CoordinatorLayout)
                findViewById(R.id.history_coordinator_layout);

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
