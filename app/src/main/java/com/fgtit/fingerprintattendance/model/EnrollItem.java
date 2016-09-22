package com.fgtit.fingerprintattendance.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by letha on 9/4/2016.
 */
public class EnrollItem {

    @SerializedName("id")
    private int id;
    @SerializedName("image")
    private String image;
    @SerializedName("firstName")
    private String firstName;
    @SerializedName("lastName")
    private String lastName;
    @SerializedName("leftThumb")
    private String leftThumb;
    @SerializedName("rightThumb")
    private String rightThumb;
    @SerializedName("isSync")
    private boolean isSync;


    public EnrollItem() {}

    public EnrollItem(int id, String image, String firstName, String lastName,
                      String leftThumb, String rightThumb, boolean isSync) {

        super();
        this.id = id;
        this.image = image;
        this.firstName = firstName;
        this.lastName = lastName;
        this.leftThumb = leftThumb;
        this.rightThumb = rightThumb;
        this.isSync = isSync;
    }

    public int getId() { return id; }

    public void setId(int id) { this.id = id; }


    public String getImage() { return image; }

    public void setImage(String image) { this.image = image; }


    public String getFirstName() { return firstName; }

    public void setFirstName(String firstName) { this.firstName = firstName; }


    public String getLastNme() { return lastName; }

    public void setLastName(String lastName) { this.lastName = lastName; }


    public String getLeftThumb() { return leftThumb; }

    public void setLeftThumb(String leftThumb) { this.leftThumb = leftThumb; }


    public String getRightThumb() { return rightThumb; }

    public void setRightThumb(String rightThumb) { this.rightThumb = rightThumb; }


    public boolean getIsSync() { return isSync; }

    public void setIsSync(boolean isSync) { this.isSync = isSync; }

}
