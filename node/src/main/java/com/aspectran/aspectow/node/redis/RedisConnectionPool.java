/*
 * Copyright (c) 2026-present The Aspectran Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aspectran.aspectow.node.redis;

import com.aspectran.core.component.bean.ablility.DisposableBean;
import com.aspectran.core.component.bean.ablility.InitializableBean;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.aware.EnvironmentAware;
import com.aspectran.core.context.env.Environment;
import com.aspectran.utils.StringUtils;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RedisConnectionPool manages the Redis client and provides connections 
 * for synchronous, asynchronous, and pub/sub operations using Lettuce.
 *
 * <p>Created: 2026-04-16</p>
 */
@Component
@Bean(id = "redisConnectionPool")
public class RedisConnectionPool implements InitializableBean, DisposableBean, EnvironmentAware {

    private static final Logger logger = LoggerFactory.getLogger(RedisConnectionPool.class);

    private String redisUri;

    private RedisClient redisClient;

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        // Retrieve Redis URI from environment properties
        redisUri = environment.getProperty("aspectow.redis.uri");
        if (StringUtils.isEmpty(redisUri)) {
            // Default to localhost if not specified
            redisUri = "redis://localhost:6379";
        }
    }

    @Override
    public void initialize() throws Exception {
        logger.info("Initializing RedisClient with URI: {}", redisUri);
        redisClient = RedisClient.create(redisUri);
    }

    @Override
    public void destroy() throws Exception {
        if (redisClient != null) {
            logger.info("Shutting down RedisClient");
            redisClient.shutdown();
        }
    }

    /**
     * Gets a new stateful Redis connection.
     * @return the stateful Redis connection
     */
    public StatefulRedisConnection<String, String> getConnection() {
        return redisClient.connect();
    }

    /**
     * Gets a new stateful Redis Pub/Sub connection.
     * @return the stateful Redis Pub/Sub connection
     */
    public StatefulRedisPubSubConnection<String, String> getPubSubConnection() {
        return redisClient.connectPubSub();
    }

}
