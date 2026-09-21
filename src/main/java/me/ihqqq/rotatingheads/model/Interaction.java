package me.ihqqq.rotatingheads.model;

import java.util.List;

public record Interaction(boolean enabled, List<String> any, List<String> left,
                          List<String> right) {
    public Interaction {
        any = List.copyOf(any == null ? List.of() : any);
        left = List.copyOf(left == null ? List.of() : left);
        right = List.copyOf(right == null ? List.of() : right);
    }

    public Interaction withEnabled(boolean value) {
        return new Interaction(value, any, left, right);
    }
}
