package com.fgtit.fingerprintattendance.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fgtit.fingerprintattendance.R;
import com.fgtit.fingerprintattendance.holder.ProgressViewHolder;
import com.fgtit.fingerprintattendance.model.EnrollItem;
import com.fgtit.fingerprintattendance.holder.EnrollHolder;
import com.fgtit.fingerprintattendance.widget.RoundImage;

import java.util.List;

/**
 * Created by ibrahim.abdulkadir@bizzdesk.com on 8/1/2016.
 */
public class EnrollAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>  {

    private List<EnrollItem> enrollItems;
    private Context context;

    private final int VIEW_ITEM = 1;
    private final int VIEW_PROG = 0;

    private int visibleThreshold = 2;
    private int lastVisibleItem, totalItemCount;
    private boolean loading;
    private OnLoadMoreListener onLoadMoreListener;

    public EnrollAdapter(Context context, RecyclerView recyclerView, List<EnrollItem> enrollItems) {
        this.enrollItems = enrollItems;
        this.context = context;


        if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {

            final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);

                    totalItemCount = linearLayoutManager.getItemCount();
                    lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();
                    if (!loading && totalItemCount <= (lastVisibleItem + visibleThreshold)) {
                        // End has been reached
                        // Do something
                        if (onLoadMoreListener != null) {
                            onLoadMoreListener.onLoadMore();
                        }
                        loading = true;
                    }
                }
            });

        }
    }

    @Override
    public int getItemViewType(int position) {
        return enrollItems.get(position) != null ? VIEW_ITEM : VIEW_PROG;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder vh;
        if (viewType == VIEW_ITEM) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_enroll, parent, false);

            vh = new EnrollHolder(v,enrollItems);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_loading_footer, parent, false);

            vh = new ProgressViewHolder(v);
        }
        return vh;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {

        if (holder instanceof EnrollHolder) {
            final EnrollItem item = enrollItems.get(position);

            ((EnrollHolder) holder).userName.setText(item.getFirstName()+" "+item.getLastNme());
            byte[] imgData = Base64.decode(item.getImage(),Base64.DEFAULT);
            Bitmap bmp = BitmapFactory.decodeByteArray(imgData,0,imgData.length);
            Drawable roundedImage = new RoundImage(bmp);
            ((EnrollHolder) holder).userImage.setImageDrawable(roundedImage);
        }
    }

    public void setLoaded() {
        loading = false;
    }

    @Override
    public int getItemCount() {
        return this.enrollItems.size();
    }

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    public interface OnLoadMoreListener {
        void onLoadMore();
    }
}
