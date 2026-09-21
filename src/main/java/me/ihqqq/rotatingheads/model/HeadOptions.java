package me.ihqqq.rotatingheads.model;

public record HeadOptions(String texture, double scale, double speed,
                          Brightness brightness, double speedX, double speedZ,
                          double bobHeight, int bobPeriod) {
    public HeadOptions {
        texture = texture == null ? "" : texture.trim();
        brightness = brightness == null ? new Brightness(15, 15) : brightness;
        scale = bounded(scale, 2, 0.05, 20);
        speed = bounded(speed, 0, -20, 20);
        speedX = bounded(speedX, 0, -20, 20);
        speedZ = bounded(speedZ, 0, -20, 20);
        bobHeight = bounded(bobHeight, 0, 0, 5);
        bobPeriod = Math.max(10, Math.min(1200, bobPeriod));
    }

    private static double bounded(double value, double fallback, double min, double max) {
        return !Double.isFinite(value) ? fallback : Math.max(min, Math.min(max, value));
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
