package model.database.models;

public class PlaylistModel {

    private int id;
    private String userid;
    private String songlist;
    private String song;

    public PlaylistModel() {
    }

    public PlaylistModel(int id, String userid, String songlist, String song) {
        this.id = id;
        this.userid = userid;
        this.songlist = songlist;
        this.song = song;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getSonglist() {
        return songlist;
    }

    public void setSonglist(String songlist) {
        this.songlist = songlist;
    }

    public String getSong() {
        return song;
    }

    public void setSong(String song) {
        this.song = song;
    }
}
