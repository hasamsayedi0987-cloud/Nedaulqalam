package com.nedaqalam.school;

/** Persian (Dari) digits + Afghan Jalali calendar. Pure Java. */
public final class Fa {
    private static final String DIG = "\u06F0\u06F1\u06F2\u06F3\u06F4\u06F5\u06F6\u06F7\u06F8\u06F9";
    private static final String[] MONTHS = {
        "\u062D\u0645\u0644", "\u062B\u0648\u0631", "\u062C\u0648\u0632\u0627",
        "\u0633\u0631\u0637\u0627\u0646", "\u0627\u0633\u062F", "\u0633\u0646\u0628\u0644\u0647",
        "\u0645\u06CC\u0632\u0627\u0646", "\u0639\u0642\u0631\u0628", "\u0642\u0648\u0633",
        "\u062C\u062F\u06CC", "\u062F\u0644\u0648", "\u062D\u0648\u062A"
    };

    private Fa() {}

    public static String d(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            b.append(c >= '0' && c <= '9' ? DIG.charAt(c - '0') : c);
        }
        return b.toString();
    }

    public static String d(int n) { return d(String.valueOf(n)); }

    public static int[] toJalali(int gy, int gm, int gd) {
        int[] gdm = {0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334};
        int jy = (gy <= 1600) ? 0 : 979;
        gy -= (gy <= 1600) ? 621 : 1600;
        int gy2 = (gm > 2) ? gy + 1 : gy;
        long days = 365L * gy + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400
                - 80 + gd + gdm[gm - 1];
        jy += 33 * (int) (days / 12053);
        days %= 12053;
        jy += 4 * (int) (days / 1461);
        days %= 1461;
        if (days > 365) {
            jy += (int) ((days - 1) / 365);
            days = (days - 1) % 365;
        }
        int jm = (days < 186) ? (int) (days / 31) + 1 : (int) ((days - 186) / 30) + 7;
        int jd = (int) ((days < 186) ? (days % 31) : ((days - 186) % 30)) + 1;
        return new int[]{jy, jm, jd};
    }

    private static int[] parts(long millis) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(millis);
        return toJalali(c.get(java.util.Calendar.YEAR),
                c.get(java.util.Calendar.MONTH) + 1,
                c.get(java.util.Calendar.DAY_OF_MONTH));
    }

    public static String date(long millis) {
        int[] j = parts(millis);
        return d(j[0]) + "/" + pad(j[1]) + "/" + pad(j[2]);
    }

    public static String dateLong(long millis) {
        int[] j = parts(millis);
        return d(j[2]) + " " + MONTHS[j[1] - 1] + " " + d(j[0]);
    }

    private static String pad(int n) {
        return d(n < 10 ? "0" + n : String.valueOf(n));
    }
}
