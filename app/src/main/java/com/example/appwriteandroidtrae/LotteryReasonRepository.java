package com.example.appwriteandroidtrae;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LotteryReasonRepository {

    private static final String BASE_URL = "https://api.taiwanlottery.com/TLCAPIWeB";
    private static final DateTimeFormatter API_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM", Locale.US);
    private static final DateTimeFormatter API_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final LocalDate SEASON_START_DATE = LocalDate.of(2024, 1, 1);

    public LotteryReasonScreenData loadAllData() throws Exception {
        List<LotteryDraw> superLottoDraws = fetchDraws(
                "/Lottery/SuperLotto638Result",
                "superLotto638Res",
                6,
                true,
                createSuperLottoGroups()
        );
        List<LotteryDraw> lotto649Draws = fetchDraws(
                "/Lottery/Lotto649Result",
                "lotto649Res",
                6,
                true,
                createLotto649Groups()
        );
        List<LotteryDraw> daily539Draws = fetchDraws(
                "/Lottery/Daily539Result",
                "daily539Res",
                5,
                false,
                createDaily539Groups()
        );

        return new LotteryReasonScreenData(superLottoDraws, lotto649Draws, daily539Draws);
    }

    private List<LotteryDraw> fetchDraws(
            String endpoint,
            String arrayKey,
            int mainNumberCount,
            boolean hasSpecialNumber,
            List<ComparisonGroup> groups
    ) throws Exception {
        Map<String, LotteryDraw> mergedDraws = new LinkedHashMap<>();
        YearMonth currentMonth = YearMonth.now();
        YearMonth cursor = YearMonth.from(SEASON_START_DATE);

        while (!cursor.isAfter(currentMonth)) {
            YearMonth endMonth = cursor.plusMonths(2);
            if (endMonth.isAfter(currentMonth)) {
                endMonth = currentMonth;
            }

            String response = request(endpoint
                    + "?month=" + API_MONTH_FORMAT.format(cursor)
                    + "&endMonth=" + API_MONTH_FORMAT.format(endMonth)
                    + "&pageNum=1&pageSize=200");

            JSONObject root = new JSONObject(response);
            JSONObject content = root.getJSONObject("content");
            JSONArray results = content.getJSONArray(arrayKey);
            for (int i = 0; i < results.length(); i++) {
                JSONObject item = results.getJSONObject(i);
                LotteryDraw draw = parseDraw(item, mainNumberCount, hasSpecialNumber, groups);
                mergedDraws.put(draw.period, draw);
            }

            cursor = endMonth.plusMonths(1);
        }

        List<LotteryDraw> draws = new ArrayList<>(mergedDraws.values());
        draws.sort(Comparator.comparing((LotteryDraw draw) -> draw.date).reversed());
        return draws;
    }

    private LotteryDraw parseDraw(
            JSONObject item,
            int mainNumberCount,
            boolean hasSpecialNumber,
            List<ComparisonGroup> groups
    ) throws Exception {
        JSONArray numbersArray = item.getJSONArray("drawNumberSize");
        List<Integer> allNumbers = new ArrayList<>();
        for (int i = 0; i < numbersArray.length(); i++) {
            allNumbers.add(numbersArray.getInt(i));
        }

        List<Integer> mainNumbers = new ArrayList<>(allNumbers.subList(0, mainNumberCount));
        Integer specialNumber = hasSpecialNumber && allNumbers.size() > mainNumberCount
                ? allNumbers.get(mainNumberCount)
                : null;

        List<GroupMatch> matches = new ArrayList<>();
        for (ComparisonGroup group : groups) {
            matches.add(calculateMatch(group, mainNumbers, specialNumber));
        }

        return new LotteryDraw(
                item.get("period").toString(),
                LocalDate.parse(item.getString("lotteryDate"), API_DATE_FORMAT),
                mainNumbers,
                specialNumber,
                matches
        );
    }

    private GroupMatch calculateMatch(ComparisonGroup group, List<Integer> mainNumbers, Integer specialNumber) {
        List<Integer> matchedMainNumbers = new ArrayList<>();
        for (int number : group.mainNumbers) {
            if (mainNumbers.contains(number)) {
                matchedMainNumbers.add(number);
            }
        }
        Collections.sort(matchedMainNumbers);
        boolean specialMatched = group.specialNumber != null && group.specialNumber.equals(specialNumber);
        return new GroupMatch(group, matchedMainNumbers, specialMatched);
    }

    private String request(String path) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(BASE_URL + path);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)");

            int statusCode = connection.getResponseCode();
            InputStream stream = statusCode >= 200 && statusCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();

            if (statusCode < 200 || statusCode >= 300) {
                throw new Exception("HTTP " + statusCode + ": " + builder);
            }
            return builder.toString();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private List<ComparisonGroup> createSuperLottoGroups() {
        return Arrays.asList(
                new ComparisonGroup("???", new int[]{7, 11, 23, 32, 33, 38}, 2),
                new ComparisonGroup("???", new int[]{7, 11, 23, 32, 33, 38}, 1),
                new ComparisonGroup("???", new int[]{19, 8, 11, 27, 37, 16}, 8),
                new ComparisonGroup("???", new int[]{19, 8, 4, 3, 37, 16}, 8)
        );
    }

    private List<ComparisonGroup> createLotto649Groups() {
        return Arrays.asList(
                new ComparisonGroup("???", new int[]{19, 8, 11, 27, 37, 16}, null),
                new ComparisonGroup("???", new int[]{19, 8, 4, 3, 37, 16}, null)
        );
    }

    private List<ComparisonGroup> createDaily539Groups() {
        return Arrays.asList(
                new ComparisonGroup("???", new int[]{19, 8, 11, 27, 37}, null),
                new ComparisonGroup("???", new int[]{19, 8, 4, 3, 37}, null)
        );
    }

    public static class LotteryReasonScreenData {
        public final List<LotteryDraw> superLottoDraws;
        public final List<LotteryDraw> lotto649Draws;
        public final List<LotteryDraw> daily539Draws;

        LotteryReasonScreenData(
                List<LotteryDraw> superLottoDraws,
                List<LotteryDraw> lotto649Draws,
                List<LotteryDraw> daily539Draws
        ) {
            this.superLottoDraws = superLottoDraws;
            this.lotto649Draws = lotto649Draws;
            this.daily539Draws = daily539Draws;
        }
    }

    public static class LotteryDraw {
        public final String period;
        public final LocalDate date;
        public final List<Integer> mainNumbers;
        public final Integer specialNumber;
        public final List<GroupMatch> matches;

        LotteryDraw(String period, LocalDate date, List<Integer> mainNumbers, Integer specialNumber, List<GroupMatch> matches) {
            this.period = period;
            this.date = date;
            this.mainNumbers = mainNumbers;
            this.specialNumber = specialNumber;
            this.matches = matches;
        }
    }

    public static class ComparisonGroup {
        public final String name;
        public final int[] mainNumbers;
        public final Integer specialNumber;

        ComparisonGroup(String name, int[] mainNumbers, Integer specialNumber) {
            this.name = name;
            this.mainNumbers = mainNumbers;
            this.specialNumber = specialNumber;
        }
    }

    public static class GroupMatch {
        public final ComparisonGroup group;
        public final List<Integer> matchedMainNumbers;
        public final boolean specialMatched;

        GroupMatch(ComparisonGroup group, List<Integer> matchedMainNumbers, boolean specialMatched) {
            this.group = group;
            this.matchedMainNumbers = matchedMainNumbers;
            this.specialMatched = specialMatched;
        }

        public int getMainMatchCount() {
            return matchedMainNumbers.size();
        }
    }
}

