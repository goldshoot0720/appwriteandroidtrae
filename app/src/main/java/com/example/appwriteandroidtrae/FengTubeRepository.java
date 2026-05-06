package com.example.appwriteandroidtrae;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FengTubeRepository {

    private static final long THREE_DAYS_MILLIS = 3L * 24L * 60L * 60L * 1000L;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final String[] CHANNEL_URLS = {
            "https://www.youtube.com/@SJdiao/videos",
            "https://www.youtube.com/@henren778",
            "https://www.youtube.com/@libertas1984/videos",
            "https://www.youtube.com/@sunlao/videos",
            "https://www.youtube.com/@Torontobigface/videos",
            "https://www.youtube.com/@junyulan/videos",
            "https://www.youtube.com/@blackwhite_raven/videos",
            "https://www.youtube.com/@quedaren/videos",
            "https://www.youtube.com/@%E5%A4%B8%E5%85%8B%E8%AF%B4",
            "https://www.youtube.com/@%E5%96%B5%E5%96%B5%E7%9C%8B%E4%B8%80%E7%9C%8B/videos"
    };

    public interface Callback {
        void onSuccess(FengTubeResult result);

        void onError(Exception error);
    }

    public static void fetchLatest(Callback callback) {
        EXECUTOR.execute(() -> {
            try {
                List<FengTubeVideo> videos = new ArrayList<>();
                long newestPublishedMillis = 0L;
                int freshCount = 0;
                long freshAfter = System.currentTimeMillis() - THREE_DAYS_MILLIS;

                for (String channelUrl : uniqueChannelUrls()) {
                    String channelPage = fetchText(channelUrl);
                    String channelId = parseChannelId(channelPage);
                    if (channelId == null || channelId.isEmpty()) {
                        continue;
                    }
                    List<FengTubeVideo> channelVideos = parseFeed(
                            fetchText("https://www.youtube.com/feeds/videos.xml?channel_id=" + channelId)
                    );
                    for (int i = 0; i < channelVideos.size() && i < 10; i++) {
                        FengTubeVideo video = channelVideos.get(i);
                        videos.add(video);
                        newestPublishedMillis = Math.max(newestPublishedMillis, video.publishedMillis);
                        if (video.publishedMillis >= freshAfter) {
                            freshCount++;
                        }
                    }
                }

                callback.onSuccess(new FengTubeResult(videos, freshCount, newestPublishedMillis));
            } catch (Exception error) {
                callback.onError(error);
            }
        });
    }

    private static Set<String> uniqueChannelUrls() {
        Set<String> urls = new LinkedHashSet<>();
        for (String url : CHANNEL_URLS) {
            urls.add(url);
        }
        return urls;
    }

    private static String fetchText(String urlText) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlText).openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 Android FengTube");
        connection.setRequestProperty("Accept-Language", "zh-TW,zh;q=0.9,en;q=0.8");
        int code = connection.getResponseCode();
        InputStream stream = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (stream == null) {
            connection.disconnect();
            throw new IllegalStateException("HTTP " + code);
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        } finally {
            connection.disconnect();
        }
        if (code >= 400) {
            throw new IllegalStateException("HTTP " + code);
        }
        return builder.toString();
    }

    private static String parseChannelId(String html) {
        String[] patterns = {
                "\"channelId\"\\s*:\\s*\"(UC[^\"]+)\"",
                "<meta\\s+itemprop=\"channelId\"\\s+content=\"(UC[^\"]+)\"",
                "https://www.youtube.com/channel/(UC[^\"]+)"
        };
        for (String patternText : patterns) {
            Matcher matcher = Pattern.compile(patternText).matcher(html);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return "";
    }

    private static List<FengTubeVideo> parseFeed(String xml) throws Exception {
        List<FengTubeVideo> videos = new ArrayList<>();
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(new StringReader(xml));

        boolean inEntry = false;
        String channelTitle = "";
        String title = "";
        String url = "";
        long publishedMillis = 0L;

        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            String name = parser.getName();
            if (event == XmlPullParser.START_TAG) {
                if ("entry".equals(name)) {
                    inEntry = true;
                    title = "";
                    url = "";
                    publishedMillis = 0L;
                } else if (inEntry && "title".equals(name)) {
                    title = parser.nextText();
                } else if (inEntry && "published".equals(name)) {
                    publishedMillis = parsePublished(parser.nextText());
                } else if (inEntry && "link".equals(name)) {
                    String href = parser.getAttributeValue(null, "href");
                    url = href != null ? href : "";
                } else if (inEntry && "name".equals(name)) {
                    channelTitle = parser.nextText();
                }
            } else if (event == XmlPullParser.END_TAG && "entry".equals(name)) {
                inEntry = false;
                if (!title.isEmpty() && !url.isEmpty()) {
                    videos.add(new FengTubeVideo(channelTitle, title, url, publishedMillis));
                }
            }
            event = parser.next();
        }

        return videos;
    }

    private static long parsePublished(String value) {
        try {
            return Instant.parse(value).toEpochMilli();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    public static class FengTubeResult {
        public final List<FengTubeVideo> videos;
        public final int freshCount;
        public final long newestPublishedMillis;

        FengTubeResult(List<FengTubeVideo> videos, int freshCount, long newestPublishedMillis) {
            this.videos = videos;
            this.freshCount = freshCount;
            this.newestPublishedMillis = newestPublishedMillis;
        }
    }

    public static class FengTubeVideo {
        public final String channelTitle;
        public final String title;
        public final String url;
        public final long publishedMillis;

        FengTubeVideo(String channelTitle, String title, String url, long publishedMillis) {
            this.channelTitle = channelTitle;
            this.title = title;
            this.url = url;
            this.publishedMillis = publishedMillis;
        }
    }
}
