package com.enumerate.comment.config;

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
 * Jackson 序列化配置:
 *   LocalDateTime → ISO 8601 带 +08:00 偏移
 *
 * MySQL Docker 默认 UTC，DATETIME 列存的是 UTC 值。
 * 转换逻辑: 将 LocalDateTime 视为 UTC → 转为 Asia/Shanghai → 输出带偏移的字符串
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
                    // MySQL DATETIME 存的是 UTC, 先视为 UTC, 再转上海时区
                    ZonedDateTime utc = value.atZone(ZoneId.of("UTC"));
                    ZonedDateTime shanghai = utc.withZoneSameInstant(ZoneId.of("Asia/Shanghai"));
                    gen.writeString(shanghai.format(FORMATTER));
                }
            });
            builder.timeZone("Asia/Shanghai");
        };
    }
}