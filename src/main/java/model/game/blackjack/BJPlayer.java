package model.game.blackjack;

import model.game.event.EventManager;

public class BJPlayer {

    private final String name;
    private final String userid;
    private final String avatarUrl;
    private StringBuilder cards;
    private long bet;
    private long money;
    private double rating;
    private boolean blackjack = false;
    private boolean doubleDown = false;

    private final String selectDeck;
    private final long startMoney;

    public BJPlayer(String name, String userid, String avatarUrl, long bet, long money, double rating, String selectDeck) {
        this.name = name;
        this.userid = userid;
        this.avatarUrl = avatarUrl;
        this.cards = new StringBuilder("");
        this.bet = bet;
        this.money = money;
        this.rating = rating;
        this.startMoney = money;
        this.selectDeck = selectDeck;
    }

    public void add(String card) {
        this.cards.append(card).append(",");
    }

    public void updateBank(boolean minus, int factor) {
        double rating = calculateRating();
        if (minus) {
            this.money -= (this.bet * factor);
            this.rating -= rating;
        } else {
            this.money += (this.bet * factor) * EventManager.getBlackjackPayout();
            this.rating += rating;
        }
    }

    private double calculateRating() {
        long bet = this.bet;
        if (this.doubleDown) {
            bet /= 2;
        }

        double rtn = 2.5 + (bet * 0.1);
        if (rtn > 20.0) {
            rtn = 20.0;
        }

        if (this.doubleDown) {
            rtn += 5.0;
        }

        return rtn;
    }

    public void reset() {
        this.cards = new StringBuilder("");
        this.blackjack = false;
    }

    private static int countCards(String cards) {
        int ass = 0;
        int counter = 0;
        for (String card : cards.split(",")) {
            if (card.contains("ass")) {
                ass++;
                continue;
            }
            counter += BlackJackGame.cardValue(card);
        }
        for (int i = 0 ; i < ass ; i++) {
            if (counter + 11 > 21) {
                counter += 1;
                continue;
            }
            counter += 11;
        }
        return counter;
    }

    public double getRating() {
        return this.rating;
    }

    public String getSelectDeck() {
        return selectDeck;
    }

    public boolean isDoubleDown() {
        return doubleDown;
    }

    public void setDoubleDown(boolean doubleDown) {
        this.doubleDown = doubleDown;
    }

    public String getName() {
        return name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public boolean isBlackjack() {
        return blackjack;
    }

    public void setBlackjack(boolean blackjack) {
        this.blackjack = blackjack;
    }

    public String getUserid() {
        return userid;
    }

    public int getCount() {
        return countCards(cards.toString());
    }

    public String getCards() {
        return cards.toString();
    }

    public void setBet(long bet) {
        this.bet = bet;
    }

    public long getBet() {
        return bet;
    }

    public long getMoney() {
        return this.money;
    }

    public long getStartMoney() {
        return startMoney;
    }
}
