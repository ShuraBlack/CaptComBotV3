package model.database.models;

public class RatingModel {

    private String userid;
    private double blackjack;
    private double chest;

    public RatingModel() {
    }

    public RatingModel(String userid, double blackjack, double chest) {
        this.userid = userid;
        this.blackjack = blackjack;
        this.chest = chest;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public double getBlackjack() {
        return blackjack;
    }

    public void setBlackjack(double blackjack) {
        this.blackjack = blackjack;
    }

    public double getChest() {
        return chest;
    }

    public void setChest(double chest) {
        this.chest = chest;
    }
}
