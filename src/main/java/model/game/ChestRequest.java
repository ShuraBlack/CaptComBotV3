package model.game;

import model.database.models.GamePlayerModel;
import model.game.blackjack.BlackJackGame;
import model.game.event.EventManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

import static java.awt.Color.BLUE;

public class ChestRequest {

    private static final Map<String, ChestRequestHistory> HISTORY = new HashMap<>();

    private final GamePlayerModel creator;

    private int elegant = 0;
    private int jewel = 0;
    private int royal = 0;

    private long money = 0;
    private long daily = 0;
    private String decks = "";

    public ChestRequest(GamePlayerModel creator) {
        this.creator = creator;
    }

    public void setChestAmount(int elegent, int jewel, int royal) {
        this.elegant = elegent;
        this.jewel = jewel;
        this.royal = royal;
    }

    public static boolean validRequest(ChestRequest request) {
        if (!HISTORY.containsKey(request.getCreator().getUserid())) {
            final ChestRequestHistory entry = new ChestRequestHistory();

            if (entry.isAllowed(request)) {
                entry.add(request.getElegant(), request.getJewel(), request.getRoyal());
                HISTORY.put(request.getCreator().getUserid(), entry);
                return true;
            }
            return false;
        }

        ChestRequestHistory entry = HISTORY.get(request.getCreator().getUserid());
        if (entry.isAllowed(request)) {
            entry.add(request.getElegant(), request.getJewel(), request.getRoyal());
            return true;
        }
        return false;
    }

    public static void getLeftChests(ChestRequest request) {
        if (!HISTORY.containsKey(request.getCreator().getUserid())) {
            request.setChestAmount(50,25,10);
            final ChestRequestHistory entry = new ChestRequestHistory();
            entry.add(50,25,10);
            HISTORY.put(request.getCreator().getUserid(), entry);
            return;
        }

        ChestRequestHistory entry = HISTORY.get(request.getCreator().getUserid());
        request.setChestAmount(50 - entry.getElegant(), 25 - entry.getJewel(), 10 - entry.getRoyal());
        entry.max();
    }

    public static void clearHistory() {
        HISTORY.clear();
    }

    public long getPrice() {
        return (long) ((this.elegant * (1000L * EventManager.getChestDiscount()))
                + (this.jewel * (2500L * EventManager.getChestDiscount()))
                + (this.royal * (5000L * EventManager.getChestDiscount())));
    }

    public void addMoney(long value) {
        this.money += value;
    }

    public void addDaily(long daily) {
        this.daily += daily;
    }

    public void addDeck(String deck) {
        this.decks += deck;
    }

    public long getMoney() {
        return money;
    }

    public long getDaily() {
        return daily;
    }

    public String getDecks() {
        return decks;
    }

    public int getElegant() {
        return elegant;
    }

    public int getJewel() {
        return jewel;
    }

    public int getRoyal() {
        return royal;
    }

    public void addChest (int elegant, int jewel, int royal) {
        this.elegant += elegant;
        this.jewel += jewel;
        this.royal += royal;
    }

    public GamePlayerModel getCreator() {
        return creator;
    }

    public static EmbedBuilder requestToMessage(Member member, ChestRequest request) {
        return new EmbedBuilder()
                .setColor(BLUE)
                .setTitle("Chest Bundle")
                .setDescription(String.format("**User:** %s\n**Chest/s:** %d <:elegant_chest:991673871788933140> %d " +
                                "<:jewel_chest:991676556185444383> %d <:royal_chest:991673873319858207>\n\n" +
                                "**Umsatz:** %s <:gold_coin:886658702512361482>\n**Daily:** %s <:gold_coin:886658702512361482>\n**Deck/s:** %s\n**EP:** %.1f",
                        member.getAsMention(), request.getElegant(), request.getJewel(), request.getRoyal(),
                        BlackJackGame.formatNumber(request.getMoney()), BlackJackGame.formatNumber(request.getDaily()), request.getDecks(), getRating(request)));
    }

    @Override
    public String toString() {
        return "ChestRequest{" +
                "creator=" + creator +
                ", elegant=" + elegant +
                ", jewel=" + jewel +
                ", royal=" + royal +
                ", money=" + money +
                ", daily=" + daily +
                ", decks='" + decks + '\'' +
                '}';
    }

    public static Consumer<ChestRequest> getRoyalWorker() {
        return chestRequest -> {
            int chest = 0;
            while(chest < chestRequest.getRoyal()) {

                Random royalRdm = new Random();
                int royalChance = royalRdm.nextInt(1001);

                if (royalChance < 840) {
                    chestRequest.addMoney(2500);
                } else if (royalChance < 940) {
                    chestRequest.addMoney(10000);
                } else if (royalChance < 985) {
                    chestRequest.addDaily(500);
                } else if (royalChance < 997) {
                    chestRequest.addMoney(80000);
                } else {
                    if (chestRequest.getCreator().getDeck().contains("ROYAL") || chestRequest.getDecks().contains("ROYAL")) {
                        chestRequest.addMoney(500000);
                        chest++;
                        continue;
                    }
                    chestRequest.addDeck("ROYAL,");
                }

                chest++;
            }
        };
    }

    public static Consumer<ChestRequest> getJewelWorker() {
        return chestRequest -> {
            int chest = 0;
            while(chest < chestRequest.getJewel()) {

                Random jewelRdm = new Random();
                int jewelChance = jewelRdm.nextInt(1001);

                if (jewelChance < 700) {
                    chestRequest.addMoney(1200);
                } else if (jewelChance < 920) {
                    chestRequest.addDaily(100);
                } else if (jewelChance < 970) {
                    chestRequest.addDaily(300);
                } else if (jewelChance < 995) {
                    chestRequest.addDaily(500);
                } else {
                    if (chestRequest.getCreator().getDeck().contains("JEWEL") || chestRequest.getDecks().contains("JEWEL")) {
                        chestRequest.addMoney(125000);
                        chest++;
                        continue;
                    }
                    chestRequest.addDeck("JEWEL,");
                }

                chest++;
            }
        };
    }

    public static Consumer<ChestRequest> getElegentWorker() {
        return chestRequest -> {
            int chest = 0;
            while(chest < chestRequest.getElegant()) {

                Random elegantRdm = new Random();
                int elegantChance = elegantRdm.nextInt(1001);

                if (elegantChance < 750) {
                    chestRequest.addMoney(500);
                } else if (elegantChance < 910) {
                    chestRequest.addChest(1,0,0);
                } else if (elegantChance < 960) {
                    chestRequest.addMoney(2500);
                } else if (elegantChance < 993) {
                    chestRequest.addDaily(200);
                } else {
                    if (chestRequest.getCreator().getDeck().contains("ELEGANT") || chestRequest.getDecks().contains("ELEGANT")) {
                        chestRequest.addMoney(50000);
                        chest++;
                        continue;
                    }
                    chestRequest.addDeck("ELEGANT,");
                }

                chest++;
            }
        };
    }

    public static double getRating(ChestRequest request) {
        return (request.getElegant() * 0.4) + (request.getJewel() * 1.2) + (request.getRoyal() * 2.0);
    }
}
