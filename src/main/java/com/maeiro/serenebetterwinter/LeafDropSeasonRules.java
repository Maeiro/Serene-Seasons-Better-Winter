package com.maeiro.serenebetterwinter;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class LeafDropSeasonRules {
    private static volatile String lastFingerprint = "";
    private static volatile Set<String> cachedSubSeasons = Collections.emptySet();

    private LeafDropSeasonRules() {
    }

    public static boolean isConfiguredLeafDropSubSeason(String subSeasonId) {
        if (subSeasonId == null || subSeasonId.isBlank()) {
            return false;
        }
        return getConfiguredSubseasonsNormalized().contains(normalize(subSeasonId));
    }

    public static Set<String> getConfiguredSubseasonsNormalized() {
        List<? extends String> configured = ServerConfig.LEAF_DROP_SUBSEASONS.get();
        String fingerprint = configured.toString();
        if (fingerprint.equals(lastFingerprint)) {
            return cachedSubSeasons;
        }

        synchronized (LeafDropSeasonRules.class) {
            if (fingerprint.equals(lastFingerprint)) {
                return cachedSubSeasons;
            }

            Set<String> normalized = new LinkedHashSet<>();
            for (String entry : configured) {
                String value = normalize(entry);
                if (value.isEmpty()) {
                    continue;
                }
                if (!value.contains("_")) {
                    SereneBetterWinterMod.LOGGER.warn(
                        "[{}] Ignoring invalid leaf_drop_subseasons entry '{}'. Expected format like MID_WINTER.",
                        SereneBetterWinterMod.MOD_ID,
                        entry
                    );
                    continue;
                }
                normalized.add(value);
            }

            cachedSubSeasons = Collections.unmodifiableSet(normalized);
            lastFingerprint = fingerprint;
            return cachedSubSeasons;
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}

