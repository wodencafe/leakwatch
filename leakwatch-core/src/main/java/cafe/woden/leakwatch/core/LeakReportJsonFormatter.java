package cafe.woden.leakwatch.core;

import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;

/**
 * Small JSON formatter for {@link LeakReport} values.
 */
public final class LeakReportJsonFormatter {
    private LeakReportJsonFormatter() {
    }

    public static String toJson(LeakReport report) {
        StringBuilder json = new StringBuilder(512);
        json.append('{');
        appendString(json, "type", report.type().name());
        appendString(json, "severity", report.severity().name());
        appendNumber(json, "id", report.id());
        appendString(json, "className", report.className());
        appendArray(json, "expectedCleanupMethods", report.expectedCleanupMethods());
        appendNullableString(json, "observedCleanupMethodName", report.observedCleanupMethodName());
        appendString(json, "createdAt", DateTimeFormatter.ISO_INSTANT.format(report.createdAt()));
        appendNumber(json, "ageMillis", report.age().toMillis());
        appendArray(json, "tags", report.tags());
        appendNullableString(json, "allocationSite", report.allocationSite() == null ? null : stackTraceToString(report.allocationSite()));
        appendString(json, "message", report.message());
        appendNullableString(json, "fallbackActionClassName", report.fallbackActionClassName());
        appendNullableString(json, "failureClassName", report.failureClassName());
        appendNullableString(json, "failureMessage", report.failureMessage());
        appendNullableNumber(json, "retentionLiveCount", report.retentionLiveCount());
        appendNullableNumber(json, "retentionMaxLiveInstances", report.retentionMaxLiveInstances());
        appendNullableNumber(json, "retentionApproxShallowBytes", report.retentionApproxShallowBytes());
        appendNullableNumber(json, "retentionMaxApproxShallowBytes", report.retentionMaxApproxShallowBytes());
        appendNullableNumber(json, "retentionApproxBytesOverBudget", report.retentionApproxBytesOverBudget());
        appendNullableString(json, "cleanedAt", report.cleanedAt() == null ? null : DateTimeFormatter.ISO_INSTANT.format(report.cleanedAt()));
        appendNullableNumber(json, "ageSinceCleanupMillis", report.ageSinceCleanup() == null ? null : report.ageSinceCleanup().toMillis());
        appendNullableString(json, "cleanupSite", report.cleanupSite() == null ? null : stackTraceToString(report.cleanupSite()));
        appendNullableNumber(json, "postCleanupGraceMillis", report.postCleanupGraceMillis());
        json.append('}');
        return json.toString();
    }

    private static String stackTraceToString(Throwable throwable) {
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add(throwable.toString());
        for (StackTraceElement element : throwable.getStackTrace()) {
            joiner.add("\tat " + element);
        }
        return joiner.toString();
    }

    private static void appendString(StringBuilder json, String field, String value) {
        appendFieldName(json, field);
        json.append('"').append(escape(value)).append('"');
    }

    private static void appendNullableString(StringBuilder json, String field, String value) {
        appendFieldName(json, field);
        if (value == null) {
            json.append("null");
        } else {
            json.append('"').append(escape(value)).append('"');
        }
    }

    private static void appendNumber(StringBuilder json, String field, long value) {
        appendFieldName(json, field);
        json.append(value);
    }

    private static void appendNullableNumber(StringBuilder json, String field, Long value) {
        appendFieldName(json, field);
        if (value == null) {
            json.append("null");
        } else {
            json.append(value);
        }
    }

    private static void appendArray(StringBuilder json, String field, Iterable<String> values) {
        appendFieldName(json, field);
        json.append('[');
        boolean first = true;
        for (String value : values) {
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append('"').append(escape(value)).append('"');
        }
        json.append(']');
    }

    private static void appendFieldName(StringBuilder json, String field) {
        if (json.length() > 1) {
            json.append(',');
        }
        json.append('"').append(escape(field)).append('"').append(':');
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
