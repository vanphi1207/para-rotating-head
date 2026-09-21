package me.ihqqq.rotatingheads.model;

public record HeadOptions(String texture, double scale, double speed,
                          Brightness brightness, double speedX, double speedZ,
                          double bobHeight, int bobPeriod) {
    public HeadOptions {
        texture = texture == null ? "" : texture.trim();
        scale = Math.max(0.05, Math.min(20, scale));
        speed = Math.max(-20, Math.min(20, speed));
        speedX = Math.max(-20, Math.min(20, speedX));
        speedZ = Math.max(-20, Math.min(20, speedZ));
        bobHeight = Math.max(0, Math.min(5, bobHeight));
        bobPeriod = Math.max(10, Math.min(1200, bobPeriod));
    }

    public HeadOptions(String texture, double scale, double speed,
                       Brightness brightness) {
        this(texture, scale, speed, brightness, 0, 0, 0, 60);
    }

    public HeadOptions withTexture(String value) {
        return new HeadOptions(value, scale, speed, brightness, speedX, speedZ,
                bobHeight, bobPeriod);
    }

    public HeadOptions withScale(double value) {
        return new HeadOptions(texture, value, speed, brightness, speedX, speedZ,
                bobHeight, bobPeriod);
    }

    public HeadOptions withSpeed(double value) {
        return new HeadOptions(texture, scale, value, brightness, speedX, speedZ,
                bobHeight, bobPeriod);
    }

    public HeadOptions withSpeedX(double value) {
        return new HeadOptions(texture, scale, speed, brightness, value, speedZ,
                bobHeight, bobPeriod);
    }

    public HeadOptions withSpeedZ(double value) {
        return new HeadOptions(texture, scale, speed, brightness, speedX, value,
                bobHeight, bobPeriod);
    }

    public HeadOptions withBrightness(Brightness value) {
        return new HeadOptions(texture, scale, speed, value, speedX, speedZ,
                bobHeight, bobPeriod);
    }

    public HeadOptions withBobHeight(double value) {
        return new HeadOptions(texture, scale, speed, brightness, speedX, speedZ,
                value, bobPeriod);
    }

    public HeadOptions withBobPeriod(int value) {
        return new HeadOptions(texture, scale, speed, brightness, speedX, speedZ,
                bobHeight, value);
    }
}
