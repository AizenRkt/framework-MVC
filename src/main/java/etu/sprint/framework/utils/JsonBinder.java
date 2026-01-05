package etu.sprint.framework.utils;

import java.lang.reflect.Field;
import java.util.List;

public class JsonBinder {

    public static String toJson(Object obj) throws IllegalAccessException {
        if (obj == null) return "null";

        if (obj instanceof String) {
            return "\"" + escape((String) obj) + "\"";
        }

        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }

        if (obj instanceof List<?>) {
            return listToJson((List<?>) obj);
        }

        return objectToJson(obj);
    }

    private static String listToJson(List<?> list) throws IllegalAccessException {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(toJson(list.get(i)));
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String objectToJson(Object obj) throws IllegalAccessException {
        StringBuilder sb = new StringBuilder("{");
        Field[] fields = obj.getClass().getDeclaredFields();
        boolean first = true;

        for (Field field : fields) {
            field.setAccessible(true);
            Object value = field.get(obj);

            if (!first) sb.append(",");
            sb.append("\"").append(field.getName()).append("\":");
            sb.append(toJson(value));

            first = false;
        }

        sb.append("}");
        return sb.toString();
    }

    private static String escape(String value) {
        return value.replace("\"", "\\\"");
    }
}
