package ru.nbk.menus.menu.reflection;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ReflectionUtil {

    public static Object getFieldValue(String fieldName, Object o){
        Class clazz = o.getClass();
        List<Class> classes = new ArrayList<>();

        while (clazz != null && !clazz.equals(Object.class)){
            classes.add(clazz);
            clazz = clazz.getSuperclass();
        }

        Field field = classes.stream().map(clz -> {
            try {
                return clz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {}
            return null;
        }).filter(f -> f != null).findFirst().orElse(null);

        if (field == null) return null;

        field.setAccessible(true);
        Object value = null;
        try {
            value = field.get(o);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        field.setAccessible(false);

        return value;
    }

}
