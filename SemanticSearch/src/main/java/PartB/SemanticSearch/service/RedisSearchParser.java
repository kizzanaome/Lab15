package PartB.SemanticSearch.service;


import java.nio.charset.StandardCharsets;
import java.util.*;

/** Minimal parser for FT.SEARCH raw reply to human-friendly maps. */
public class RedisSearchParser {

    public static List<Map<String, Object>> parseSearch(Object raw) {
        // The first element is the total count; the rest come in pairs: key, fields
        // WITHSCORES will also include scores; here we just extract fields we returned.
        List<?> list = (List<?>) raw;
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 1; i < list.size(); i += 2) {
            Object keyObj = list.get(i);
            Object fieldsObj = list.get(i + 1);

            String key = bytesToString(keyObj);
            Map<String, Object> row = new HashMap<>();
            row.put("key", key);

            // fieldsObj is a List: [field, value, field, value, ...] possibly also a score in between
            if (fieldsObj instanceof List<?> fields) {
                for (int j = 0; j < fields.size() - 1; j += 2) {
                    String f = bytesToString(fields.get(j));
                    String v = bytesToString(fields.get(j + 1));
                    row.put(f, v);
                }
            }

            out.add(row);
        }
        return out;
    }

    private static String bytesToString(Object o) {
        if (o instanceof byte[] b) return new String(b, StandardCharsets.UTF_8);
        return String.valueOf(o);
    }
}
