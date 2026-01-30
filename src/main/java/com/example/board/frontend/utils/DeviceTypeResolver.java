package com.example.board.frontend.utils;

import java.util.List;
import java.util.Locale;

public class DeviceTypeResolver {
    private static final List<String> mobiles = List.of("mobile", "iphone", "android", "ipod", "mobi");
    private DeviceTypeResolver() {}

    public static DeviceType resolve(String ua) {
        if(ua == null || ua.isBlank()) {
            return DeviceType.PC;
        }
        var str = ua.toLowerCase(Locale.ROOT);
        for (String mobile : mobiles) {
            if(str.contains(mobile))
                return DeviceType.MOBILE;
        }
        return DeviceType.PC;
    }
}
