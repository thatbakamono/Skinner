package me.kernelerror.skinner.mineskin.data;

// Based on https://github.com/InventivetalentDev/MineskinClient
public interface SkinCallback {
    void done(Skin skin);

    default void waiting(long delay) { }
    default void uploading() { }
    default void error(String errorMessage) { }
    default void exception(Exception exception) { }
    default void parseException(Exception exception, String body) { }
}