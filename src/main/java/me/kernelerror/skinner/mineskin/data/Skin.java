package me.kernelerror.skinner.mineskin.data;

import com.google.gson.annotations.SerializedName;

// Based on https://github.com/InventivetalentDev/MineskinClient
public class Skin {
    public int id;
    public String name;
    public SkinData data;
    public long timestamp;
    @SerializedName("private")
    public boolean prvate;
    public int views;
    public int accountId;
    public double nextRequest;
}