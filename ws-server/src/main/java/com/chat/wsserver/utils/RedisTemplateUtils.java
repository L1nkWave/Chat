package com.chat.wsserver.utils;

import lombok.experimental.UtilityClass;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.lang.NonNull;

import java.util.function.Consumer;

@UtilityClass
public class RedisTemplateUtils {

    /**
     * Provides execution of a bunch of redis-operations as a unit of work (i.e. transaction)
     * using Redis' native commands such as {@code multi}, {@code exec} and {@code discard}.
     *
     * @param consumer operations that is needed to be executed in transaction
     * @param <K> key type of RedisTemplate
     * @param <V> value type of RedisTemplate
     *
     * @see RedisOperations
     */
    public static <K, V> void executeInTxn(@NonNull RedisTemplate<K, V> template,
                                           @NonNull Consumer<RedisOperations<K, V>> consumer) {

        template.execute(new SessionCallback<V>() {

            @Override
            public V execute(@NonNull RedisOperations operations) throws DataAccessException {
                try {
                    operations.multi();
                    //noinspection unchecked
                    consumer.accept(operations);
                    operations.exec();
                } catch (RuntimeException e) {
                    operations.discard();
                }
                return null;
            }

        });

    }

}
