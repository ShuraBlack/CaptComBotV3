package model.database.models;

public class SongIDModel {

    private String song;
    private String songlist;
    private int id;

    public SongIDModel() {
    }

    public SongIDModel(String song, String songlist, int id) {
        this.song = song;
        this.songlist = songlist;
        this.id = id;
    }

    public String getSong() {
        return song;
    }

    public void setSong(String song) {
        this.song = song;
    }

    public String getSonglist() {
        return songlist;
    }

    public void setSonglist(String songlist) {
        this.songlist = songlist;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
