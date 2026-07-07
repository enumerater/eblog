package com.enumerate.query.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * LocalDateTime → ISO 8601 带 +08:00 偏移
 * MySQL DATETIME 存 UTC, 序列化时转 Asia/Shanghai
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss+08:00");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateTimeCustomizer() {
        return builder -> {
            builder.serializerByType(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
                @Override
                public void serialize(LocalDateTime value, JsonGenerator gen,
                                       SerializerProvider provider) throws IOException {
                    if (value == null) {
                        gen.writeNull();
                        return;
                    }
                    ZonedDateTime utc = value.atZone(ZoneId.of("UTC"));
                    ZonedDateTime shanghai = utc.withZoneSameInstant(ZoneId.of("Asia/Shanghai"));
                    gen.writeString(shanghai.format(FORMATTER));
                }
            });
            builder.timeZone("Asia/Shanghai");
        };
    }
}