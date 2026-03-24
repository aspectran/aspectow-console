/*
 * Copyright (c) 2020-present The Aspectran Project
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
package com.aspectran.aspectow.console.common.db.tx;

import com.aspectran.core.component.bean.annotation.After;
import com.aspectran.core.component.bean.annotation.Aspect;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Before;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.ExceptionThrown;
import com.aspectran.core.component.bean.annotation.Finally;
import com.aspectran.core.component.bean.annotation.Joinpoint;
import com.aspectran.core.component.bean.annotation.Qualifier;
import com.aspectran.core.component.bean.annotation.Scope;
import com.aspectran.core.context.rule.type.ScopeType;
import com.aspectran.mybatis.SqlSessionAdvice;
import org.apache.ibatis.session.SqlSessionFactory;

/**
 * An aspect for handling simple database transactions.
 * It advises methods annotated with {@code @consoleSqlSession} to manage the lifecycle
 * of a {@link org.apache.ibatis.session.SqlSession}.
 * <ul>
 * <li>A transaction scope will be started (i.e., NOT auto-commit).</li>
 * <li>A Connection object will be acquired from the DataSource instance
 *     configured by the active environment.</li>
 * <li>The transaction isolation level will be the default used by the driver or
 *     data source.</li>
 * <li>No PreparedStatements will be reused, and no updates will be batched.</li>
 * </ul>
 *
 * <p>Created: 2025. 2. 15.</p>
 */
@Component
@Bean(lazyDestroy = true)
@Scope(ScopeType.PROTOTYPE)
@Aspect(
        id = "consoleTxAspect",
        order = 0
)
@Joinpoint(
        pointcut = {
                "+: **@consoleSqlSession"
        }
)
public class ConsoleTxAspect extends SqlSessionAdvice {

    @Autowired
    public ConsoleTxAspect(@Qualifier("consoleSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        super(sqlSessionFactory);
    }

    @Before
    public void open() {
        super.open();
    }

    @After
    public void commit() {
        super.commit();
    }

    /**
     * Rolls back the transaction if an exception occurs during execution.
     */
    @ExceptionThrown
    public void rollback() {
        super.rollback();
    }

    @Finally
    public void close() {
        super.close();
    }

}
