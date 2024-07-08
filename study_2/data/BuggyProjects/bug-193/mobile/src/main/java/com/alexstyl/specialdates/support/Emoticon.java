package com.alexstyl.specialdates.support;

import com.alexstyl.specialdates.util.Utils;

public enum Emoticon {
    SMILEY("\uD83D\uDE03", ":)");

    private final String emoji;
    private final String smiley;

    Emoticon(String emoji, String smiley) {
        this.emoji = emoji;
        this.smiley = smiley;
    }

    public String asText() {
        if (supportsEmojis()) {
            return emoji;
        }
        return smiley;
    }

    private boolean supportsEmojis() {
        return Utils.hasKitKat();
    }

}
