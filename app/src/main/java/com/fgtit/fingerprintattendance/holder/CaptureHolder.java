package com.fgtit.fingerprintattendance.holder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.fgtit.fingerprintattendance.R;
import com.fgtit.fingerprintattendance.model.CaptureItem;

import java.util.List;

/**
 * Created by ibrahim.abdulkadir@bizzdesk.com  on 7/29/2016.
 */
public class CaptureHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private List<CaptureItem> captureItems;

    public ImageView userImage;
    public TextView userName, dateTime;

    public CaptureHolder(View convertView, List<CaptureItem> captureItems) {
        super(convertView);
        this.captureItems = captureItems;

        itemView.setOnClickListener(this);

        userImage = (ImageView) convertView
                .findViewById(R.id.item_capture_image);

        userName = (TextView) convertView
                .findViewById(R.id.item_capture_name);

        dateTime = (TextView) convertView
                .findViewById(R.id.item_capture_date_time);
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {

            default:
                break;
        }
    }


}
