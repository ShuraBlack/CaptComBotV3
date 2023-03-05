package model.worker;

import com.shurablack.core.event.EventWorker;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class WarframeTracker extends EventWorker {

    @Override
    public void processPublicChannelEvent(Member member, MessageChannelUnion channel, String message, MessageReceivedEvent event) {
        if (channel.getType() != ChannelType.TEXT) {
            return;
        }
        TextChannel textChannel = channel.asTextChannel();

        String[] args = message.split(" ");
        if (args.length == 2) {
            switch (args[1]) {
                case "cycle":
                    trackerMessage(textChannel);
                    break;
                case "arbi":
                    arbitrationMessage(textChannel);
                    break;
                case "construct":
                    constructionProgressMessage(textChannel);
                    break;
                case "sortie":
                    sortieMessage(textChannel);
                    break;
                case "nightwave":
                    nightwaveMessage(textChannel);
                    break;
                case "voidtrader":
                    voidTraderMessage(textChannel);
                    break;
            }
        } else if (args.length == 4) {
            int number = isNumeric(args[3]);
            if (number == -1) {
                return;
            }
            orderMessage(textChannel, args[2]);
        }
    }

    private JSONObject getInfo(String url) throws IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(url);
        HttpResponse response = client.execute(request);
        HttpEntity entity = response.getEntity();

        String content = EntityUtils.toString(entity);
        return new JSONObject(content);
    }

    private void orderMessage(TextChannel channel, String object) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Warframe.Market - " + object.replace("_"," "));
        eb.setColor(Color.BLACK);
        eb.setFooter("Diese Nachricht wird in 1 Minute gelöscht");
        try {
            List<Order> orders = new ArrayList<>();
            JSONArray array = getInfo("https://api.warframe.market/v1/items/" + object + "/orders?platform=pc")
                    .getJSONObject("payload").getJSONArray("orders");

            for (int i = 0 ; i < array.length() ; i++) {
                JSONObject order = array.getJSONObject(i);
                String status = order.getJSONObject("user").getString("status");
                if (status.equals("ingame") && order.getString("order_type").equals("sell")) {
                    orders.add(new Order(order.getJSONObject("user").getString("ingame_name"), order.getInt("platinum")));
                }
            }
            orders = orders.stream().sorted(Comparator.comparingInt(o -> o.price)).limit(3).collect(Collectors.toList());
            Emoji platinum = channel.getGuild().getEmojiById("886996382655869029");
            for (Order entry : orders) {
                assert platinum != null;
                eb.addField(entry.username,platinum.getFormatted() + entry.price ,true);
            }
        } catch (IOException ignored) {
            eb.addField("","```diff\n- Fehler während des erstellen der Anfrage\n```",false);
        } finally {
            channel.sendMessageEmbeds(eb.build()).complete().delete().queueAfter(1, TimeUnit.MINUTES);
        }
    }

    public int isNumeric(String number) {
        try {
            int i = Integer.parseInt(number);
            if (i < 1) {
                throw new NumberFormatException();
            }
            return i;
        } catch (NumberFormatException nfe) {
            return -1;
        }
    }

    private void trackerMessage(TextChannel channel) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Tracker - HUB (Warframe)");
        eb.setColor(Color.BLACK);
        eb.setFooter("Diese Nachricht wird in 1 Minute gelöscht");
        try {
            JSONObject object = getInfo("https://api.warframestat.us/pc/cetusCycle");
            String state = object.getString("state");
            if (state.equals("day")) {
                state = "☀️ " + state;
            } else {
                state = "\uD83C\uDF19 " + state;
            }
            eb.addField("Cetus","```\n" + state + "\n- " + object.getString("shortString") + "\n```",false);

            object = getInfo("https://api.warframestat.us/pc/vallisCycle");
            state = object.getString("state");
            if (state.equals("cold")) {
                state = "\uD83E\uDDCA " + state;
            } else {
                state = "\uD83D\uDD25 " + state;
            }
            eb.addField("Orb Vallis","```\n" + state + "\n- " + object.getString("shortString") + "\n```",false);

            object = getInfo("https://api.warframestat.us/pc/cambionCycle");
            state = object.getString("active");
            if (state.equals("fass")) {
                state = "\uD83D\uDD34 " + state;
            } else {
                state = "\uD83D\uDD35 " + state;
            }
            eb.addField("Deimos","```\n" + state + "\n```",false);

            object = getInfo("https://api.warframestat.us/pc/earthCycle");
            state = object.getString("state");
            if (state.equals("day")) {
                state = "☀️ " + state;
            } else {
                state = "\uD83C\uDF19 " + state;
            }
            eb.addField("Earth","```\n" + state + "\n- " + object.getString("timeLeft") + " until rotation" + "\n```",false);
        } catch (IOException ignored) {
            eb.addField("","```diff\n- Fehler während des erstellen der Anfrage\n```",false);
        } finally {
            channel.sendMessageEmbeds(eb.build()).complete().delete().queueAfter(1, TimeUnit.MINUTES);
        }
    }

    private void arbitrationMessage(TextChannel channel) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Arbitration - HUB (Warframe)");
        eb.setColor(Color.BLACK);
        eb.setFooter("Diese Nachricht wird in 1 Minute gelöscht");
        try {
            JSONObject object = getInfo("https://api.warframestat.us/pc/arbitration");
            eb.addField(object.getString("node"),"**Type:** " +
                            object.getString("type") + "\n**Fraction:** " +
                            object.getString("enemy") + "\n**Archwing:** " +
                            (object.getBoolean("archwing") ? "Yes" : "No") + "\n**Sharkwng:** " +
                            (object.getBoolean("sharkwing") ? "Yes" : "No")
                    ,false);
        } catch (IOException ignored) {
            eb.addField("","```diff\n- Fehler während des erstellen der Anfrage\n```",false);
        } finally {
            channel.sendMessageEmbeds(eb.build()).complete().delete().queueAfter(1, TimeUnit.MINUTES);
        }
    }

    private void constructionProgressMessage(TextChannel channel) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Construction Progress - HUB (Warframe)");
        eb.setColor(Color.BLACK);
        eb.setFooter("Diese Nachricht wird in 1 Minute gelöscht");
        try {
            JSONObject object = getInfo("https://api.warframestat.us/pc/constructionProgress");
            float razorback = object.getFloat("razorbackProgress");
            String razorSymbol = "\uD83D\uDD32";
            if (razorback == 100.0f) {
                razorSymbol = "\uD83D\uDFE6";
            }
            StringBuilder razor = new StringBuilder();
            for (int i = 0 ; i < (int) razorback / 10 ; i++) {
                razor.append(razorSymbol);
            }
            if ((razorback / 10) % (int) (razorback / 10) > 0.5f) {
                razor.append("◼️");
            }
            eb.addField("Razorback - " + razorback + "%",razor.toString(),false);

            float fomorian = object.getFloat("fomorianProgress");
            String fomSymbol = "\uD83D\uDD32";
            if (fomorian == 100.0f) {
                fomSymbol = "\uD83D\uDFE6";
            }
            StringBuilder fom = new StringBuilder();
            for (int i = 0 ; i < (int) fomorian / 10 ; i++) {
                fom.append(fomSymbol);
            }
            if ((fomorian / 10) % (int) (fomorian / 10) > 0.5f) {
                fom.append("◼️");
            }
            eb.addField("Fomorian - " + fomorian + "%",fom.toString(),false);
        } catch (IOException ignored) {
            eb.addField("", "```diff\n- Fehler während des erstellen der Anfrage\n```", false);
        } finally {
            channel.sendMessageEmbeds(eb.build()).complete().delete().queueAfter(1, TimeUnit.MINUTES);
        }
    }

    private void sortieMessage(TextChannel channel) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Sortie - HUB (Warframe)");
        eb.setColor(Color.BLACK);
        eb.setFooter("Diese Nachricht wird in 2 Minute gelöscht");

        try {
            JSONObject object = getInfo("https://api.warframestat.us/pc/sortie");

            eb.addField("__Information:__","**Remaining:** " + object.getString("eta") +
                    "\n**Faction:** " + object.getString("faction") +
                    "\n**Boss:** " + object.getString("boss"),false);
            eb.addBlankField(false);
            JSONArray variants = new JSONArray(object.getJSONArray("variants"));

            JSONObject first = new JSONObject(variants.get(0).toString());
            eb.addField(first.getString("node"),"**Type:** " + first.getString("missionType") +
                    "\n**Modifier:** " + first.getString("modifier") + "\n```\n" +
                    first.getString("modifierDescription") + "\n```",false);

            JSONObject second = new JSONObject(variants.get(1).toString());
            eb.addField(second.getString("node"),"**Type:** " + second.getString("missionType") +
                    "\n**Modifier:** " + second.getString("modifier") + "\n```\n" +
                    second.getString("modifierDescription") + "\n```",false);

            JSONObject third = new JSONObject(variants.get(2).toString());
            eb.addField(third.getString("node"),"**Type:** " + third.getString("missionType") +
                    "\n**Modifier:** " + third.getString("modifier") + "\n```\n" +
                    third.getString("modifierDescription") + "\n```",false);

        } catch (IOException ignored) {
            eb.addField("", "```diff\n- Fehler während des erstellen der Anfrage\n```", false);
        } finally {
            channel.sendMessageEmbeds(eb.build()).complete().delete().queueAfter(2, TimeUnit.MINUTES);
        }
    }

    private void nightwaveMessage(TextChannel channel) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Nightwave - HUB (Warframe)");
        eb.setColor(Color.BLACK);
        eb.setDescription("\n⚪️Daily \uD83D\uDFE0Weekly \uD83D\uDD35Elite\n");
        eb.setFooter("Diese Nachricht wird in 5 Minute gelöscht");

        try {
            JSONObject object = getInfo("https://api.warframestat.us/pc/nightwave");
            eb.addField("__Information:__","**Season:** " + object.getInt("season") +
                    "\n**Started:** " + object.getString("startString") +
                    "\n**Nightwave:** " + object.getString("tag"),false);
            eb.addBlankField(false);
            JSONArray array = object.getJSONArray("activeChallenges");
            for (int i = 0 ; i < array.length() ; i++) {
                JSONObject mission = new JSONObject(array.get(i).toString());
                String type = "⚪️";
                int reputation = 1000;
                if (mission.getInt("reputation") == 4500) {
                    type = "\uD83D\uDFE0";
                    reputation = 4500;
                } else if (mission.getInt("reputation") == 7000) {
                    type = "\uD83D\uDD35";
                    reputation = 7000;
                }
                eb.addField("__" + mission.getString("title") + "__","**Type:** " + type +
                        "\n**Reputation:** " + reputation + "```\n" + mission.getString("desc") + "\n```",true);
            }
        } catch (IOException ignored) {
            eb.addField("", "```diff\n- Fehler während des erstellen der Anfrage\n```", false);
        } finally {
            channel.sendMessageEmbeds(eb.build()).complete().delete().queueAfter(5, TimeUnit.MINUTES);
        }
    }

    private void voidTraderMessage(TextChannel channel) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("VoidTrader - HUB (Warframe)");
        eb.setColor(Color.BLACK);
        boolean activ = true;
        try {
            JSONObject object = getInfo("https://api.warframestat.us/pc/voidTrader");
            if (object.getBoolean("active")) {
                eb.addField("__Information:__","**Remaining:** " + object.getString("endString") +
                        "\n**Started:** " + object.getString("startString") +
                        "\n**Location:** " + object.getString("location"),false);
                eb.addBlankField(false);
                JSONArray array = object.getJSONArray("inventory");
                for (int i = 0 ; i < array.length() ; i++) {
                    JSONObject item = new JSONObject(array.get(i).toString());
                    eb.addField("__" + item.getString("item") + "__","**Credit:** " + item.getInt("credits") +
                            "\n**Ducat:** " + item.getInt("ducats"),true);
                }
            } else {
                eb.addField("Baro Ki'Teer isnt currently activ","**Arrive:** ",false);
                activ = false;
            }
        } catch (IOException ignored) {
            eb.addField("", "```diff\n- Fehler während des erstellen der Anfrage\n```", false);
        } finally {
            if (activ) {
                eb.setFooter("Diese Nachricht wird in 5 Minute gelöscht");
                channel.sendMessageEmbeds(eb.build()).complete().delete().queueAfter(5, TimeUnit.MINUTES);
            } else {
                eb.setFooter("Diese Nachricht wird in 1 Minute gelöscht");
                channel.sendMessageEmbeds(eb.build()).complete().delete().queueAfter(1, TimeUnit.MINUTES);
            }
        }
    }

    private static class Order {

        private final String username;
        private final int price;

        public Order(String username, int price) {
            this.username = username;
            this.price = price;
        }
    }
}
