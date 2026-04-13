package com.ca.centranalytics.integration.config;

import com.ca.centranalytics.integration.domain.entity.Platform;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Component
public class PlatformRequestParamConverter implements Converter<String, Platform> {

    @Override
    public Platform convert(String source) {
        if (!StringUtils.hasText(source)) {
            return null;
        }

        String normalized = source.trim().toUpperCase(Locale.ROOT);
        if ("WHATSAPP".equals(normalized)) {
            return Platform.WAPPI;
        }

        return Platform.valueOf(normalized);
    }
}
