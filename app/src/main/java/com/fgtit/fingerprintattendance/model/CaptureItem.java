package com.fgtit.fingerprintattendance.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by letha on 9/4/2016.
 */
public class CaptureItem {

    @SerializedName("id")
    private int id;
    @SerializedName("captureId")
    private int captureId;
    @SerializedName("longitude")
    private double longitude;
    @SerializedName("latitude")
    private double latitude;
    @SerializedName("timeStamp")
    private String timeStamp;
    @SerializedName("isSync")
    private boolean isSync;

    public CaptureItem() {}

    public CaptureItem(int id, int captureId, double longitude, double latitude, String timeStamp, boolean isSync) {

        super();
        this.id = id;
        this.captureId = captureId;
        this.longitude = longitude;
        this.latitude = latitude;
        this.timeStamp = timeStamp;
        this.isSync = isSync;

    }

    public int getId() { return id; }

    public void setId(int id) { this.id = id; }


    public int getCaptureId() { return captureId; }

    public void setCaptureId(int id) { this.captureId = captureId; }


    public double getLongitude() { return longitude; }

    public void setLongitude(double longitude) { this.longitude = longitude; }


    public double getLatitude() { return latitude; }

    public void setLatitude(double latitude) { this.latitude = latitude; }


    public String getTimeStamp() { return timeStamp; }

    public void setTimeStamp(String timeStamp) { this.timeStamp = timeStamp; }


    public boolean getIsSync() { return isSync; }

    public void setIsSync(boolean isSync) { this.isSync = isSync; }

}

