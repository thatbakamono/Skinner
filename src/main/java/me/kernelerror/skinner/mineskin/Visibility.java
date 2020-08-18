package me.kernelerror.skinner.mineskin;

// Based on https://github.com/InventivetalentDev/MineskinClient
public enum Visibility {
    PUBLIC(0),
    PRIVATE(1);

    private final int code;

    Visibility(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}