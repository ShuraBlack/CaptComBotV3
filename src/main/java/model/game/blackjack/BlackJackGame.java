package model.game.blackjack;

import com.shurablack.core.builder.JDAUtil;
import com.shurablack.core.util.ServerUtil;
import model.game.SkinLoader;
import model.manager.DiscordBot;
import com.shurablack.sql.SQLRequest;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static model.database.Statement.UPDATE_GAME_BLACKJACK;

public class BlackJackGame {

    public enum State {
        WAITING,
        PLAYING,
        RESULT,
        STARTING
    }

    private final Queue<BJPlayer> queue = new LinkedList<>();
    private final List<BJPlayer> player = new ArrayList<>();
    private int index = 0;
    private BJPlayer activePlayer;

    private final BJPlayer dealer = new BJPlayer("Dealer","","",0,0, 0,"DEFAULT");

    private final Stack<String> cards = new Stack<>();
    private State state = State.WAITING;

    public BlackJackGame() {
        List<String> arr = new ArrayList<>();
        String[] value = {"2","3","4","5","6","7","8","9","10","jack","queen","king","ass"};
        String[] symbol = {"clubs","spades","diamonds","heart"};
        for (int i = 0 ; i < 5 ; i++) {
            for (String v : value) {
                for (String s : symbol) {
                    arr.add(v + " " + s);
                }
            }
        }

        Collections.shuffle(arr, new SecureRandom());
        Collections.shuffle(arr, new SecureRandom());
        
        cards.addAll(arr);
    }

    public boolean startGame() {
        if (state.equals(State.PLAYING)) {
            return false;
        }
        if (player.stream().map(BJPlayer::getCards).anyMatch(cards -> !cards.equals(""))) {
            index = 0;
            removeCards();
        }
        state = State.PLAYING;
        for (int i = 0 ; i < 2 ; i++) {
            dealer.add(cards.pop());
            for (BJPlayer pl : player) {
                pl.add(cards.pop());
            }
        }
        if (dealer.getCount() == 21) {
            dealer.setBlackjack(true);
        }
        for (BJPlayer pl : player) {
            if (pl.getCount() == 21) {
                pl.setBlackjack(true);
            }
        }
        nextPlayer();
        return true;
    }

    public int addPlayer(String name, String userid, String avatarUrl, long bet, long money, double rating, String selectDeck) {
        if (player.size() == 4 || state.equals(State.PLAYING)) {
            return addQueue(name, userid, avatarUrl, bet, money, rating, selectDeck);
        }
        player.add(new BJPlayer(name, userid, avatarUrl, bet, money, rating, selectDeck));
        return 0;
    }

    public int addQueue(String name, String userid, String avatarUrl, long bet, long money, double rating, String selectDeck) {
        if (queue.stream().anyMatch(player -> player.getUserid().equals(userid))) {
            return -1;
        }
        if (isPlaying(userid)) {
            return -1;
        }
        if (!queue.add(new BJPlayer(name, userid, avatarUrl, bet, money, rating, selectDeck))) {
            return -1;
        }
        return queue.size();
    }

    public void removePlayer(String userid) {
        if (activePlayer != null && activePlayer.getUserid().equals(userid)) {
            activePlayer = player.get(0);
        }
        Optional<BJPlayer> player = this.player.stream().filter(pl -> pl.getUserid().equals(userid)).findFirst();
        player.ifPresent(pl -> {
            createTransaction(pl);
            this.player.remove(pl);
        });
    }

    public boolean isPlaying(String userid) {
        return player.stream().anyMatch(player -> player.getUserid().equals(userid));
    }

    public boolean inQueue(String userid) {
        return queue.stream().anyMatch(player -> player.getUserid().equals(userid));
    }

    public void playerDraw() {
        activePlayer.add(cards.pop());
        if (activePlayer.getCount() >= 20) {
            nextPlayer();
        }
    }

    public void dealerDraw() {
        while(dealer.getCount() < 17
                && player.stream().noneMatch(pl -> pl.getCount() < dealer.getCount())) {
            dealer.add(cards.pop());
        }
    }

    public void doubleDown() {
        if ((activePlayer.getBet() * 200) > activePlayer.getMoney()) {
            return;
        }
        activePlayer.setDoubleDown(true);
        activePlayer.setBet(activePlayer.getBet() * 2);
        activePlayer.add(cards.pop());
        nextPlayer();
    }

    public void nextPlayer() {
        if (index < player.size()) {
            activePlayer = player.get(index);
            index++;
            if (activePlayer.getCount() >= 20) {
                nextPlayer();
            }
        } else {
            activePlayer = null;
        }
    }

    public boolean updateMoney(String userid, long money) {
        Optional<BJPlayer> entry = player.stream().filter(pl -> pl.getUserid().equals(userid)).findFirst();
        if (!entry.isPresent()) {
            return false;
        }

        if (entry.get().getMoney() >= money * 100) {
            return false;
        }
        entry.get().setBet(money * 100);
        return true;
    }

    private void createTransaction(BJPlayer player) {
        SQLRequest.run(DiscordBot.UTIL.getConnectionPool(), UPDATE_GAME_BLACKJACK(player.getUserid()
                , String.format("%.1f", player.getRating()).replace(",","."), (player.getMoney() - player.getStartMoney())));
    }

    public EmbedBuilder createResult() {
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(ServerUtil.BLUE)
                .setTitle("Tisch")
                .setDescription(String.format("**Spieler:** [%s]   **Phase:** [%s]   **Deck:** [%d]"
                        , player.size() == 4 ? "4 (VOLL)" : "" + player.size(), state, cards.size()))
                .addField(String.format("Dealer [%d][x]", dealer.getCount()), SkinLoader.getSkin(dealer.getSelectDeck()
                        ,dealer.getCards()) + (dealer.isBlackjack() ? "\uD83D\uDD14" : ""),false);
        for (BJPlayer pl : player) {
            String result = SkinLoader.getSkin(pl.getSelectDeck(),pl.getCards());
            if (pl.getCount() > 21) { // Verloren
                pl.updateBank(true, 100);
                result += "\n- \uD83D\uDD34 BUST -\n";
            } else if (pl.isBlackjack() && (dealer.getCount() == 21 && !dealer.isBlackjack())) { // Gewonnen
                pl.updateBank(false, 150);
                result += "\n- \uD83D\uDD14 BLACKJACK(3/2) -\n";
            } else if (pl.isBlackjack()) { // Gewonnen
                pl.updateBank(false, 150);
                result += "\n- \uD83D\uDD14 BLACKJACK(3/2) -\n";
            } else if ((pl.getCount() == 21 && !pl.isBlackjack()) && dealer.isBlackjack()) { // Verloren
                pl.updateBank(true, 100);
                result += "\n- \uD83D\uDD34 LOST -\n";
            } else if ((pl.isBlackjack() && dealer.isBlackjack()) || (pl.getCount() == dealer.getCount())) { // Unentschieden
                result += "\n- \uD83D\uDFE1 DRAW -\n";
            } else if (dealer.getCount() > 21 && pl.getCount() < 22) { // Gewonnen
                pl.updateBank(false, 100);
                result += "\n- \uD83D\uDFE2 Dealer BUST(2/1) -\n";
            } else if (pl.getCount() > dealer.getCount()) { // Gewonnen
                pl.updateBank(false, 100);
                result += "\n- \uD83D\uDFE2 HIGHER(2/1) -\n";
            } else if (pl.getCount() < dealer.getCount()) { // Verloren
                pl.updateBank(true, 100);
                result += "\n- \uD83D\uDD34 LOWER -\n";
            }
            eb.addField(String.format("%s [%d][%d<:gold_coin:886658702512361482>] %s - Konto: %s", pl.getName(), pl.getCount(), (pl.getBet() * 100)
                    ,pl.isDoubleDown() ? "<:double_down:991187010448539709>" : "" , formatNumber(pl.getMoney())), result, false);
        }
        eb.setImage("https://www.gamblingsites.org/app/themes/gsorg2018/images/blackjack-hard-hand-example-3.png");
        state = State.RESULT;
        return eb;
    }

    public EmbedBuilder createMessage() {
        String[] args = dealer.getCards().split(",");
        String dCards = "";

        if (!dealer.getCards().equals("") && state.equals(State.PLAYING)) {
            dCards = SkinLoader.getSkin(dealer.getSelectDeck(),args[0] + ",top bottom");
        }

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("Tisch")
                .setDescription(String.format("**Spieler:** [%d]   **Phase:** [%s]   **Deck:** [%d]"
                        , player.size(), state, cards.size()))
                .addField("Dealer [?][x]", (state.equals(State.WAITING) ? "Wartet..." : dCards),false);

        if (activePlayer != null) {
            eb.setThumbnail(activePlayer.getAvatarUrl());
            eb.setAuthor("Aktiver Spieler:\n" + activePlayer.getName(), null, null);
        }

        switch (state) {
            case PLAYING: eb.setColor(ServerUtil.RED);
                break;
            case WAITING: eb.setColor(ServerUtil.GREEN);
                break;
            case STARTING: eb.setColor(new Color(200,200,100));
                break;
        }

        for (BJPlayer pl : player) {
            eb.addField(String.format("%s [%d][%d<:gold_coin:886658702512361482>] %s", pl.getName(), pl.getCount(), (pl.getBet() * 100), pl.isDoubleDown() ? "<:double_down:991187010448539709>" : "")
                    , SkinLoader.getSkin(pl.getSelectDeck(), pl.getCards()), false);
        }
        eb.setImage("https://www.gamblingsites.org/app/themes/gsorg2018/images/blackjack-hard-hand-example-3.png");
        return eb;
    }

    private void removeCards() {
        for (BJPlayer pl : player) {
            String[] playerCards = pl.getCards().split(",");
            for (String playerCard : playerCards) {
                if (playerCard.equals("")) {
                    continue;
                }
                if (pl.isDoubleDown()) {
                    pl.setDoubleDown(false);
                    pl.setBet(pl.getBet() / 2);
                }
                cards.add(ThreadLocalRandom.current().nextInt(0, cards.size()), playerCard);
            }
        }
        player.forEach(BJPlayer::reset);
        index = 0;
        String[] dealerCards = dealer.getCards().split(",");
        for (String dealerCard : dealerCards) {
            if (dealerCard.equals("")) {
                continue;
            }
            cards.add(ThreadLocalRandom.current().nextInt(0, cards.size()), dealerCard);
        }
        dealer.reset();
    }

    public EmbedBuilder reset() {
        removeCards();
        StringBuilder s = new StringBuilder();
        player.removeIf(pl -> {
            if (pl.getMoney() < (pl.getBet() * 100)) {
                createTransaction(pl);
                s.append(JDAUtil.JDA.retrieveUserById(pl.getUserid()).complete().getAsMention()).append("\n");
                return true;
            }
            return false;
        });

        while (player.size() < 4 && !queue.isEmpty()) {
            player.add(queue.poll());
        }

        if (s.length() == 0) {
            return null;
        }
        return new EmbedBuilder()
                .setColor(ServerUtil.RED)
                .setDescription("Folgende Nutzer wurden entfernt:\n" + s)
                .setFooter("Aufgrund von mangelnden Coins");
    }

    public void resetAll() {
        reset();
        player.clear();
        state = State.WAITING;
    }

    public String getTopCards() {
        StringBuilder s = new StringBuilder();
        int count = 0;
        for (int i = cards.size()-1 ; count < 5 ; i--) {
            s.append(cards.get(i)).append(", ");
            count++;
        }
        return s.toString();
    }

    public boolean isEmpty() {
        return player.isEmpty();
    }

    public int playerSize() {
        return player.size();
    }

    public boolean isTurn(String userid) {
        return activePlayer.getUserid().equals(userid);
    }

    public boolean isFinished() {
        return activePlayer == null;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public static String formatNumber(Long value) {
        DecimalFormat df = new DecimalFormat("#,###.##");
        return df.format(value);
    }

    public static int cardValue(String card) {
        String[] args = card.split(" ");
        switch (args[0]) {
            case "2":
            case "3":
            case "4":
            case "5":
            case "6":
            case "7":
            case "8":
            case "9":
            case "10":
                return Integer.parseInt(args[0]);
            case "jack":
            case "queen":
            case "king":
                return 10;
            default:
                return 0;
        }
    }
}
