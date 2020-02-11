package com.github.gotify.messages.provider;

public final class PositionPair {
    private final int allPosition;
    private final int appPosition;

    public PositionPair(int allPosition, int appPosition) {
        this.allPosition = allPosition;
        this.appPosition = appPosition;
    }

    public int getAllPosition() {
        return allPosition;
    }

    public int getAppPosition() {
        return appPosition;
    }
}
