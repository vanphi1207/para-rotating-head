package me.ihqqq.rotatingheads.model;

public record Brightness(int sky, int block) {
    public Brightness {
        sky = Math.max(0, Math.min(15, sky));
        block = Math.max(0, Math.min(15, block));
    }
}
