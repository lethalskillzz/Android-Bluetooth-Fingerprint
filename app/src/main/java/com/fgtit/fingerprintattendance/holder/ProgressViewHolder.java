package com.fgtit.fingerprintattendance.holder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;

import com.fgtit.fingerprintattendance.R;


/**
 * Created by ibrahim.abdulkadir@bizzdesk.com on 7/18/2016.
 */
public class ProgressViewHolder extends RecyclerView.ViewHolder {
    public ProgressBar progressBar;

    public ProgressViewHolder(View v) {
        super(v);
        progressBar = (ProgressBar) v.findViewById(R.id.footer_progressbar);
    }
}
