package model.api;

import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class YouTubeAPI {

    public static String API_KEY;

    public static String getRelatedVideos(String identifier) {
        HttpURLConnection con;
        try {
            URL url = new URL(String.format("https://www.googleapis.com/youtube/v3/search" +
                    "?part=snippet&type=video&relatedToVideoId=%s&key=%s", identifier, API_KEY));

            con = (HttpsURLConnection) url.openConnection();
            con.connect();

            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder s = new StringBuilder();
            for (String line = br.readLine() ; line != null ; line = br.readLine()) {
                s.append(line).append("\n");
            }
            br.close();

            String videoID;
            try {
                videoID = new JSONObject(s.toString()).getJSONArray("items")
                        .getJSONObject(0).getJSONObject("id").getString("videoId");
            } catch (Exception e) {
                return null;
            }
            return String.format("https://youtu.be/%s", videoID);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
