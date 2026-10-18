package com.nedaqalam.school;

import java.util.ArrayList;

/** ساخت محتوای خروجی TXT / HTML(Word) / PDF از یک بخش */
public final class ExportUtil {

    private ExportUtil() {}

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public static String secTitle(String sectionId) {
        Sec s = Store.secById(sectionId);
        return s == null ? sectionId : s.title;
    }

    /** متن ساده TXT */
    public static String buildText(String sectionId) {
        ArrayList<Rec> list = Store.recsOf(sectionId);
        StringBuilder sb = new StringBuilder();
        sb.append("\u0645\u06A9\u062A\u0628 \u062E\u0635\u0648\u0635\u06CC \u0646\u062F\u0627\u0627\u0644\u0642\u0644\u0645\n");
        sb.append("\u0628\u062E\u0634: ").append(secTitle(sectionId)).append('\n');
        sb.append("\u062A\u0627\u0631\u06CC\u062E: ").append(Fa.dateLong(System.currentTimeMillis())).append('\n');
        sb.append("\u062A\u0639\u062F\u0627\u062F: ").append(Fa.d(list.size())).append('\n');
        sb.append("================================\n\n");
        int i = 1;
        for (Rec r : list) {
            sb.append(Fa.d(i++)).append(") ").append(r.title).append('\n');
            if (!r.code.isEmpty()) sb.append("   \u06A9\u062F: ").append(r.code).append('\n');
            if (!r.phone.isEmpty()) sb.append("   \u062A\u0645\u0627\u0633: ").append(r.phone).append('\n');
            if (!r.note.isEmpty()) sb.append("   \u062A\u0648\u0636\u06CC\u062D\u0627\u062A: ").append(r.note.replace("\n", " ")).append('\n');
            if (r.fileName != null && !r.fileName.isEmpty()) {
                sb.append("   \u067E\u06CC\u0648\u0633\u062A: ").append(r.fileName)
                  .append(" (").append(r.typeLabel()).append(")").append('\n');
            }
            sb.append("   \u062A\u0627\u0631\u06CC\u062E \u062B\u0628\u062A: ").append(Fa.date(r.created)).append('\n');
            sb.append("--------------------------------\n");
        }
        sb.append("\n\u0633\u0627\u0632\u0646\u062F\u0647: \u062D\u0633\u0627\u0645 \u0627\u0644\u062F\u06CC\u0646 \u0633\u06CC\u062F\u06CC \u2014 0791680632\n");
        return sb.toString();
    }

    /** HTML با استایل RTL — هم برای فایل Word و هم برای چاپ PDF */
    public static String buildHtml(String sectionId) {
        ArrayList<Rec> list = Store.recsOf(sectionId);
        StringBuilder sb = new StringBuilder();
        sb.append("<html dir=\"rtl\"><head><meta charset=\"utf-8\"><style>")
          .append("body{font-family:'Noto Naskh Arabic',Tahoma,'DejaVu Sans';direction:rtl;color:#212121}")
          .append("h1{color:#1A237E;font-size:20px;margin-bottom:2px}")
          .append("h2{color:#303F9F;font-size:16px;margin-top:4px}")
          .append("table{border-collapse:collapse;width:100%;margin-top:10px}")
          .append("th,td{border:1px solid #9FA8DA;padding:6px;font-size:12px;text-align:right}")
          .append("th{background:#E8EAF6;color:#1A237E}")
          .append(".meta{color:#555;font-size:11px}")
          .append(".foot{margin-top:14px;color:#777;font-size:11px}")
          .append("</style></head><body>");
        sb.append("<h1>\u0645\u06A9\u062A\u0628 \u062E\u0635\u0648\u0635\u06CC \u0646\u062F\u0627\u0627\u0644\u0642\u0644\u0645</h1>");
        sb.append("<h2>\u0628\u062E\u0634: ").append(esc(secTitle(sectionId))).append("</h2>");
        sb.append("<p class=\"meta\">\u062A\u0627\u0631\u06CC\u062E: ").append(Fa.dateLong(System.currentTimeMillis()))
          .append(" &nbsp;|&nbsp; \u062A\u0639\u062F\u0627\u062F: ").append(Fa.d(list.size())).append("</p>");
        sb.append("<table><tr><th>\u0634\u0645\u0627\u0631\u0647</th><th>\u0639\u0646\u0648\u0627\u0646</th><th>\u06A9\u062F</th>")
          .append("<th>\u062A\u0645\u0627\u0633</th><th>\u062A\u0648\u0636\u06CC\u062D\u0627\u062A</th><th>\u067E\u06CC\u0648\u0633\u062A</th><th>\u062A\u0627\u0631\u06CC\u062E \u062B\u0628\u062A</th></tr>");
        int i = 1;
        for (Rec r : list) {
            sb.append("<tr><td>").append(Fa.d(i++)).append("</td><td>").append(esc(r.title))
              .append("</td><td>").append(esc(r.code))
              .append("</td><td>").append(esc(r.phone))
              .append("</td><td>").append(esc(r.note))
              .append("</td><td>").append(r.fileName == null ? "" : esc(r.fileName))
              .append("</td><td>").append(Fa.date(r.created)).append("</td></tr>");
        }
        sb.append("</table>");
        sb.append("<p class=\"foot\">\u0633\u0627\u0632\u0646\u062F\u0647: \u062D\u0633\u0627\u0645 \u0627\u0644\u062F\u06CC\u0646 \u0633\u06CC\u062F\u06CC \u2014 0791680632 \u2014 hasamsayedi0987@gmail.com</p>");
        sb.append("</body></html>");
        return sb.toString();
    }
}
