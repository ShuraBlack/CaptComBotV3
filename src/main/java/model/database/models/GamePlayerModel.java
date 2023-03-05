package model.database.models;

public class GamePlayerModel {

    private String userid;
    private boolean daily;
    private String booster;
    private int daily_bonus;
    private long money;
    private String select_deck;
    private String deck;

    public GamePlayerModel() {
    }

    public GamePlayerModel(String userid, boolean daily, String booster, int daily_bonus, long money, String select_deck, String deck) {
        this.userid = userid;
        this.daily = daily;
        this.booster = booster;
        this.daily_bonus = daily_bonus;
        this.money = money;
        this.select_deck = select_deck;
        this.deck = deck;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public boolean isDaily() {
        return daily;
    }

    public void setDaily(boolean daily) {
        this.daily = daily;
    }

    public String getBooster() {
        return booster;
    }

    public void setBooster(String booster) {
        this.booster = booster;
    }

    public int getDaily_bonus() {
        return daily_bonus;
    }

    public void setDaily_bonus(int daily_bonus) {
        this.daily_bonus = daily_bonus;
    }

    public long getMoney() {
        return money;
    }

    public void setMoney(long money) {
        this.money = money;
    }

    public String getSelect_deck() {
        return select_deck;
    }

    public void setSelect_deck(String select_deck) {
        this.select_deck = select_deck;
    }

    public String getDeck() {
        return deck;
    }

    public void setDeck(String deck) {
        this.deck = deck;
    }
}
