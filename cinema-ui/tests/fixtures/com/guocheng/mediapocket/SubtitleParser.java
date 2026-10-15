package com.guocheng.mediapocket;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* loaded from: classes.dex */
public final class SubtitleParser {

    public static final class Cue {
        public final int end;
        public final int start;
        public final String text;

        Cue(int i, int i2, String str) {
            this.start = i;
            this.end = i2;
            this.text = str;
        }
    }

    public static ArrayList<Cue> parse(String str) {
        String strSubstring;
        int iIndexOf;
        ArrayList<Cue> arrayList = new ArrayList<>();
        String[] strArrSplit = str.replace("\r", "").split("\n\\s*\n");
        Pattern patternCompile = Pattern.compile("((?:\\d{1,2}:)?\\d{2}:\\d{2}[,.]\\d{3})\\s*-->\\s*((?:\\d{1,2}:)?\\d{2}:\\d{2}[,.]\\d{3})");
        for (String str2 : strArrSplit) {
            Matcher matcher = patternCompile.matcher(str2);
            if (matcher.find() && (iIndexOf = (strSubstring = str2.substring(matcher.end())).indexOf(10)) >= 0) {
                arrayList.add(new Cue(time(matcher.group(1)), time(matcher.group(2)), strSubstring.substring(iIndexOf + 1).trim().replaceAll("<[^>]+>", "")));
            }
        }
        return arrayList;
    }

    static int time(String str) {
        String[] strArrSplit = str.replace(',', '.').split(":");
        double d = Double.parseDouble(strArrSplit[strArrSplit.length - 1]) + (Integer.parseInt(strArrSplit[strArrSplit.length - 2]) * 60);
        if (strArrSplit.length == 3) {
            d += Integer.parseInt(strArrSplit[0]) * 3600;
        }
        return (int) (d * 1000.0d);
    }
}
