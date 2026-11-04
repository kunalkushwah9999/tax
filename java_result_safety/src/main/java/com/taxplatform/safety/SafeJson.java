package com.taxplatform.safety;

import java.util.List;
import java.util.Map;

/** Closed projection encoder for the dependency-free module. Not a JSON parser. */
public final class SafeJson {
    private SafeJson() { }

    public static String encode(PublicResult result) {
        StringBuilder out = new StringBuilder();
        append(result.toResponse(), out);
        return out.toString();
    }

    public static String encode(GuidedResult result) {
        StringBuilder out = new StringBuilder();
        append(result.toResponse(), out);
        return out.toString();
    }

    public static String encode(IntakeGuide guide) {
        StringBuilder out = new StringBuilder();
        append(guide.toResponse(), out);
        return out.toString();
    }

    public static String encode(FactEditResult receipt) {
        StringBuilder out = new StringBuilder();
        append(receipt.toResponse(), out);
        return out.toString();
    }

    public static String encode(FactEditHistory history) {
        StringBuilder out = new StringBuilder();
        append(history.toResponse(), out);
        return out.toString();
    }

    public static String encode(ProfessionalQueue queue) {
        StringBuilder out = new StringBuilder();
        append(queue.toResponse(), out);
        return out.toString();
    }

    private static void append(Object value, StringBuilder out) {
        if (value instanceof String text) {
            out.append('"');
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '"' || c == '\\') out.append('\\').append(c);
                else if (c < 0x20 || c == '\u2028' || c == '\u2029') {
                    out.append("\\u").append(String.format("%04x", (int) c));
                } else out.append(c);
            }
            out.append('"');
        } else if (value instanceof Boolean bool) {
            out.append(bool);
        } else if (value instanceof Map<?, ?> map) {
            out.append('{'); boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String)) throw new IllegalArgumentException("string projection key required");
                if (!first) out.append(','); first = false;
                append(entry.getKey(), out); out.append(':'); append(entry.getValue(), out);
            }
            out.append('}');
        } else if (value instanceof List<?> list) {
            out.append('['); boolean first = true;
            for (Object item : list) {
                if (!first) out.append(','); first = false; append(item, out);
            }
            out.append(']');
        } else {
            throw new IllegalArgumentException("unexpected type in public projection");
        }
    }
}
