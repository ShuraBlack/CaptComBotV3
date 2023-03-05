package model.game;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SkinLoader {

    private static final Map<String, Map<String, String>> SKINS = new HashMap<>();

    public static void loadData() {
        InputStream inputStream = SkinLoader.class
                .getClassLoader()
                .getResourceAsStream("skins.json");
        StringBuilder s = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                s.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONArray skins = new JSONArray(s.toString());
        for (int i = 0 ; i < skins.length() ; i++) {
            JSONObject deck = skins.getJSONObject(i);
            String name = deck.getString("name");
            Map<String, String> cards = new HashMap<>();
            JSONArray cardObject = deck.getJSONArray("cards");
            for (int j = 0 ; j < cardObject.length() ; j++) {
                JSONObject card = cardObject.getJSONObject(j);
                cards.put(card.getString("symbol"), card.getString("skin"));
            }
            SKINS.put(name, cards);
        }
    }

    public static String getSkin(String skin, String cards) {
        if (cards.equals("")) {
            return cards;
        }
        StringBuilder top = new StringBuilder();
        StringBuilder bottom = new StringBuilder();
        Map<String, String> subset = SKINS.get(skin);
        Arrays.stream(cards.split(",")).forEach(card -> {
            String[] args = card.split(" ");
            top.append(subset.get(args[0])).append(" ");
            bottom.append(subset.get(args[1])).append(" ");
        });
        return top + "\n" + bottom;
    }

    public static String showTree() {
        StringBuilder s = new StringBuilder("Skins:\n");
        for (Map.Entry<String, Map<String, String>> entry : SKINS.entrySet()) {
            s.append("|_ ").append(entry.getKey()).append("\n");
            for (Map.Entry<String, String> deck : entry.getValue().entrySet()) {
                s.append("\t|_ ").append(deck.getKey()).append(" -> ").append(deck.getValue()).append("\n");
            }
        }
        return s.toString();
    }

}
