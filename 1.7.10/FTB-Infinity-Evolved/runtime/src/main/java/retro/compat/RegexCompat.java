package retro.compat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replacement-string handling with the JDK's rules ({@code $n}, {@code ${name}}, {@code \x}). TeaVM's own parser
 * rejects valid references such as {@code "%$1s"}; here each match's replacement is expanded first and handed to
 * TeaVM fully escaped.
 */
public final class RegexCompat {
    private RegexCompat() {
    }

    private static String expand(Matcher m, String repl) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int n = repl.length();
        while (i < n) {
            char c = repl.charAt(i);
            if (c == '\\') {
                i++;
                if (i >= n) {
                    throw new IllegalArgumentException("character to be escaped is missing");
                }
                sb.append(repl.charAt(i++));
            } else if (c == '$') {
                i++;
                if (i >= n) {
                    throw new IllegalArgumentException("Illegal group reference: group index is missing");
                }
                char d = repl.charAt(i);
                if (d == '{') {
                    int end = repl.indexOf('}', i);
                    if (end < 0) {
                        throw new IllegalArgumentException("named capturing group is missing trailing '}'");
                    }
                    String name = repl.substring(i + 1, end);
                    String g = m.group(name);
                    if (g != null) {
                        sb.append(g);
                    }
                    i = end + 1;
                } else {
                    int ref = d - '0';
                    if (ref < 0 || ref > 9) {
                        throw new IllegalArgumentException("Illegal group reference");
                    }
                    i++;
                    if (ref > m.groupCount()) {
                        throw new IndexOutOfBoundsException("No group " + ref);
                    }
                    while (i < n) {
                        int nd = repl.charAt(i) - '0';
                        if (nd < 0 || nd > 9) {
                            break;
                        }
                        int next = ref * 10 + nd;
                        if (m.groupCount() < next) {
                            break;
                        }
                        ref = next;
                        i++;
                    }
                    String g = m.group(ref);
                    if (g != null) {
                        sb.append(g);
                    }
                }
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private static String quote(String s) {
        if (s.indexOf('\\') < 0 && s.indexOf('$') < 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '$') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public static Matcher appendReplacement(Matcher m, StringBuffer sb, String repl) {
        return m.appendReplacement(sb, quote(expand(m, repl)));
    }

    public static String replaceAll(Matcher m, String repl) {
        m.reset();
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, quote(expand(m, repl)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static String replaceFirst(Matcher m, String repl) {
        m.reset();
        StringBuffer sb = new StringBuffer();
        if (m.find()) {
            m.appendReplacement(sb, quote(expand(m, repl)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static String stringReplaceAll(String s, String regex, String repl) {
        return replaceAll(compile(regex).matcher(s), repl);
    }

    public static String stringReplaceFirst(String s, String regex, String repl) {
        return replaceFirst(compile(regex).matcher(s), repl);
    }

    public static Pattern compile(String regex) {
        return Pattern.compile(workaround(regex));
    }

    public static Pattern compile(String regex, int flags) {
        return Pattern.compile((flags & Pattern.LITERAL) != 0 ? regex : workaround(regex), flags);
    }

    public static boolean stringMatches(String s, String regex) {
        return compile(regex).matcher(s).matches();
    }

    /**
     * TeaVM 0.15's engine fails to match an optional group written {@code (x){0,1}} inside a repeated group
     * ({@code ( (\-){0,1}\d+){3}} rejects " -1 2 3"; Forge's OBJ model loader uses that shape), while the
     * equivalent {@code (x)?} works. Rewrites {0,1} to ? outside character classes and quoted sections.
     */
    static String workaround(String regex) {
        if (regex.indexOf("{0,1}") < 0) {
            return regex;
        }
        StringBuilder sb = new StringBuilder(regex.length());
        int classDepth = 0;
        boolean quoted = false;
        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
            if (quoted) {
                if (c == '\\' && regex.startsWith("\\E", i)) {
                    quoted = false;
                    sb.append("\\E");
                    i++;
                } else {
                    sb.append(c);
                }
            } else if (c == '\\' && i + 1 < regex.length()) {
                if (regex.charAt(i + 1) == 'Q') {
                    quoted = true;
                }
                sb.append(c).append(regex.charAt(++i));
            } else if (c == '[') {
                classDepth++;
                sb.append(c);
            } else if (c == ']' && classDepth > 0) {
                classDepth--;
                sb.append(c);
            } else if (classDepth == 0 && regex.startsWith("{0,1}", i)) {
                sb.append('?');
                i += 4;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
