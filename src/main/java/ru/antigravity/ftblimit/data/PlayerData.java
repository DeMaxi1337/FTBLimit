package ru.antigravity.ftblimit.data;

import ru.antigravity.ftblimit.config.GroupConfig;

import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private String name;
    private String customGroup;
    private int dailyExcavations;
    private int dailyBonus;
    private Integer permanentLimitOverride;
    private long lastExcavationTime;
    private long lockoutUntil;
    private long totalExcavations;
    private long totalBlocks;

    private boolean banned;
    private String banReason;
    private String bannedBy;
    private String banDate;

    private transient long lastBannedNotificationTime;
    private transient long lastCooldownNotificationTime;

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCustomGroup() {
        return customGroup;
    }

    public void setCustomGroup(String customGroup) {
        this.customGroup = (customGroup != null && !customGroup.trim().isEmpty()) ? customGroup.trim().toLowerCase() : null;
    }

    public int getDailyExcavations() {
        checkLockoutExpiration();
        return dailyExcavations;
    }

    public void setDailyExcavations(int dailyExcavations) {
        this.dailyExcavations = Math.max(0, dailyExcavations);
    }

    public int getDailyBonus() {
        checkLockoutExpiration();
        return dailyBonus;
    }

    public void setDailyBonus(int dailyBonus) {
        this.dailyBonus = Math.max(0, dailyBonus);
    }

    public void addDailyBonus(int amount) {
        this.dailyBonus = Math.max(0, this.dailyBonus + amount);
    }

    public Integer getPermanentLimitOverride() {
        return permanentLimitOverride;
    }

    public void setPermanentLimitOverride(Integer permanentLimitOverride) {
        if (permanentLimitOverride != null && permanentLimitOverride < 0) {
            this.permanentLimitOverride = null;
        } else {
            this.permanentLimitOverride = permanentLimitOverride;
        }
    }

    public boolean hasPermanentLimitOverride() {
        return permanentLimitOverride != null && permanentLimitOverride >= 0;
    }

    public long getLastExcavationTime() {
        return lastExcavationTime;
    }

    public void setLastExcavationTime(long lastExcavationTime) {
        this.lastExcavationTime = lastExcavationTime;
    }

    public long getLockoutUntil() {
        return lockoutUntil;
    }

    public void setLockoutUntil(long lockoutUntil) {
        this.lockoutUntil = Math.max(0L, lockoutUntil);
    }

    public boolean isLockedOut() {
        checkLockoutExpiration();
        return lockoutUntil > System.currentTimeMillis();
    }

    public long getRemainingLockoutSeconds() {
        checkLockoutExpiration();
        long diff = lockoutUntil - System.currentTimeMillis();
        return diff > 0 ? (diff + 999) / 1000 : 0L;
    }

    public void checkLockoutExpiration() {
        if (lockoutUntil > 0 && System.currentTimeMillis() >= lockoutUntil) {
            this.dailyExcavations = 0;
            this.dailyBonus = 0;
            this.lockoutUntil = 0L;
        }
    }

    public void clearDelay() {
        this.lockoutUntil = 0L;
        this.dailyExcavations = 0;
        this.lastExcavationTime = 0L;
    }

    public long getTotalExcavations() {
        return totalExcavations;
    }

    public void setTotalExcavations(long totalExcavations) {
        this.totalExcavations = Math.max(0, totalExcavations);
    }

    public long getTotalBlocks() {
        return totalBlocks;
    }

    public void setTotalBlocks(long totalBlocks) {
        this.totalBlocks = Math.max(0, totalBlocks);
    }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned, String reason, String bannedBy, String banDate) {
        this.banned = banned;
        this.banReason = reason;
        this.bannedBy = bannedBy;
        this.banDate = banDate;
    }

    public String getBanReason() {
        return banReason != null ? banReason : "None";
    }

    public String getBannedBy() {
        return bannedBy != null ? bannedBy : "Console";
    }

    public String getBanDate() {
        return banDate != null ? banDate : "Unknown";
    }

    public int getEffectiveDailyLimit(GroupConfig group) {
        if (hasPermanentLimitOverride()) {
            return permanentLimitOverride + getDailyBonus();
        }
        if (group == null || group.isUnlimited()) {
            return -1;
        }
        return group.getDailyLimit() + getDailyBonus();
    }

    public boolean isLimitReached(GroupConfig group) {
        int limit = getEffectiveDailyLimit(group);
        if (limit < 0) return false;
        return getDailyExcavations() >= limit;
    }

    public int getRemainingDailyExcavations(GroupConfig group) {
        int limit = getEffectiveDailyLimit(group);
        if (limit < 0) return Integer.MAX_VALUE;
        return Math.max(0, limit - getDailyExcavations());
    }

    public void recordExcavation(int blocks) {
        this.dailyExcavations++;
        this.totalExcavations++;
        this.totalBlocks += Math.max(1, blocks);
        this.lastExcavationTime = System.currentTimeMillis();
    }

    public boolean shouldSendBannedNotification(int cooldownSeconds) {
        long now = System.currentTimeMillis();
        if (now - lastBannedNotificationTime > cooldownSeconds * 1000L) {
            lastBannedNotificationTime = now;
            return true;
        }
        return false;
    }

    public boolean shouldSendCooldownNotification(int cooldownSeconds) {
        long now = System.currentTimeMillis();
        if (now - lastCooldownNotificationTime > cooldownSeconds * 1000L) {
            lastCooldownNotificationTime = now;
            return true;
        }
        return false;
    }
}
