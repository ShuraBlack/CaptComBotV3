package model.database.models;

public class UserTimeModel {

    private String userid;
    private int time_sec;

    @Override
    public String toString() {
        long sec = time_sec % 60;
        long min = time_sec / 60;
        long hour = min / 60;
        long day = hour / 24;

        min = min % 60;
        hour = hour % 24;
        day = day % 24;

        return String.format("%s%s%s%s",
                day == 0 ? "" : day + "d ",
                hour == 0 ? "" : hour + "h ",
                min == 0 && hour == 0 ? "" : min + "m ",
                sec + "s"
        );
    }
}
