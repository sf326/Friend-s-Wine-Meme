package com.sf.friendwinememe.client.audio;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LrcParser {
    public static class Line {
        public final double timeSec;
        public final String text;
        public final boolean isTitle;

        Line(double timeSec, String text, boolean isTitle) {
            this.timeSec = timeSec;
            this.text = text;
            this.isTitle = isTitle;
        }
    }

    //[mm:ss.xxx]
    private static final Pattern PATTERN = Pattern.compile(
            "^\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})(?:-(zd))?\\](.*)$");

    public static List<Line> parse(InputStream in) {
        List<Line> result = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String raw;
            while ((raw = br.readLine()) != null) {
                raw = raw.trim();
                if (raw.isEmpty()) continue;

                Matcher m = PATTERN.matcher(raw);
                if (!m.find()) continue;

                int min = Integer.parseInt(m.group(1));
                int sec = Integer.parseInt(m.group(2));
                String fracStr = m.group(3);
                double frac = Integer.parseInt(fracStr) / Math.pow(10, fracStr.length());
                boolean isTitle = "zd".equals(m.group(4));
                String text = m.group(5) == null ? "" : m.group(5).trim();

                result.add(new Line(min * 60 + sec + frac, text, isTitle));
            }
        } catch (Exception ignored) {}
        result.sort(Comparator.comparingDouble(l -> l.timeSec));
        return result;
    }

    public static Line lineAt(List<Line> lines, double sec) {
        if (lines == null || lines.isEmpty()) return null;
        Line current = null;
        for (Line l : lines) {
            if (l.timeSec <= sec) current = l;
            else break;
        }
        return current;
    }
}