package ru.antigravity.ftblimit.config;

public class GroupConfig {

    private final String id;
    private final String displayName;
    private final int dailyLimit;
    private final int delayAfterLimit;
    private final int normalDelay;
    private final int priority;

    public GroupConfig(String id, String displayName, int dailyLimit, int delayAfterLimit, int normalDelay, int priority) {
        this.id = id;
        this.displayName = displayName != null ? displayName : id;
        this.dailyLimit = dailyLimit;
        this.delayAfterLimit = Math.max(0, delayAfterLimit);
        this.normalDelay = Math.max(0, normalDelay);
        this.priority = priority;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    public boolean isUnlimited() {
        return dailyLimit < 0;
    }

    public int getDelayAfterLimit() {
        return delayAfterLimit;
    }

    public int getNormalDelay() {
        return normalDelay;
    }

    public int getPriority() {
        return priority;
    }
}
