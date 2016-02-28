package com.trungvu.android_media_player_component.utility;

/**
 * Created by TrungVT on 2/29/16.
 */
public class TimeUtil {

    public static final int SECOND_BY_MILISECONDS = 1000;
    public static final int MINUTE_BY_SECONDS = 60;
    public static final String TIME_SPAN_BY_MINUTES_SECONDS_FORMATTER = "%02d:%02d";

    /**
     * mm:ss Date time format string
     * @param miliseconds
     * @return mm:ss format string
     */
    public static String getMMSSFromMiliseconds(int miliseconds) {
        int totalSecs = miliseconds / SECOND_BY_MILISECONDS;
        return String.format(TIME_SPAN_BY_MINUTES_SECONDS_FORMATTER,
                totalSecs / MINUTE_BY_SECONDS, totalSecs % MINUTE_BY_SECONDS);
    }
}
