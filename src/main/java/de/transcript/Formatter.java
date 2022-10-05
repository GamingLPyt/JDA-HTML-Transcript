package de.transcript;

import lombok.experimental.UtilityClass;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * #  || Created ||
 * #      By   >> GamingLPyt
 * #      Date >> October 05, 2022
 * #      Time >> 17:45:24
 * #
 * #  || Project ||
 * #      Name >> DISCORD-HTML-TRANSCRIPT
 */

@UtilityClass
public class Formatter {

    private final Pattern strong = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private final Pattern em = Pattern.compile("\\*(.+?)\\*");
    private final Pattern s = Pattern.compile("~~(.+?)~~");
    private final Pattern u = Pattern.compile("__(.+?)__");
    private final Pattern code = Pattern.compile("```(.+?)```");
    private final Pattern code_1 = Pattern.compile("`(.+?)`");
    private final Pattern newLine = Pattern.compile("\\n");

    public String formatBytes(final long bytes) {
        final int unit = 1024;
        if (bytes < unit)
            return bytes + " B";
        final int exp = (int) (Math.log(bytes) / Math.log(unit));
        final String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public String format(final String originalText) {
        Matcher matcher = strong.matcher(originalText);
        String newText = originalText;
        while (matcher.find()) {
            final String group = matcher.group();
            newText = newText.replace(group,
                    "<strong>" + group.replace("**", "") + "</strong>");
        }
        matcher = em.matcher(newText);
        while (matcher.find()) {
            final String group = matcher.group();
            newText = newText.replace(group,
                    "<em>" + group.replace("*", "") + "</em>");
        }
        matcher = s.matcher(newText);
        while (matcher.find()) {
            final String group = matcher.group();
            newText = newText.replace(group,
                    "<s>" + group.replace("~~", "") + "</s>");
        }
        matcher = u.matcher(newText);
        while (matcher.find()) {
            final String group = matcher.group();
            newText = newText.replace(group,
                    "<u>" + group.replace("__", "") + "</u>");
        }
        matcher = code.matcher(newText);
        boolean findCode = false;
        while (matcher.find()) {
            final String group = matcher.group();
            newText = newText.replace(group,
                    "<div class=\"pre pre--multiline nohighlight\">"
                            + group.replace("```", "").substring(3, -3) + "</div>");
            findCode = true;
        }
        if (!findCode) {
            matcher = code_1.matcher(newText);
            while (matcher.find()) {
                final String group = matcher.group();
                newText = newText.replace(group,
                        "<span class=\"pre pre--inline\">" + group.replace("`", "") + "</span>");
            }
        }
        matcher = newLine.matcher(newText);
        while (matcher.find()) newText = newText.replace(matcher.group(), "<br />");
        return newText;
    }

    public String toHex(final Color color) {
        String hex = Integer.toHexString(color.getRGB() & 0xffffff);
        while (hex.length() < 6) hex = "0" + hex;
        return hex;
    }
}
