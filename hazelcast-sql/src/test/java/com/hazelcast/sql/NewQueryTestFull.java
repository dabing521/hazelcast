package com.hazelcast.sql;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.internal.query.QueryHandle;
import com.hazelcast.internal.query.QueryHandleImpl;
import com.hazelcast.internal.query.exec.RootConsumer;
import com.hazelcast.internal.query.io.Row;
import com.hazelcast.map.impl.query.Query;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.TestHazelcastInstanceFactory;
import com.hazelcast.test.annotation.ParallelJVMTest;
import com.hazelcast.test.annotation.QuickTest;
import org.apache.calcite.linq4j.Enumerable;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class, ParallelJVMTest.class})
public class NewQueryTestFull extends HazelcastTestSupport {

    private static final String QUERY = "select height from persons";
//    private static final String QUERY = "select height from persons where age >= 5 order by name";
//    private static final String QUERY = "select age, height from persons where age >= 5";

    @Test
    public void testEndtoEnd() throws Exception {
        // Start several members.
        TestHazelcastInstanceFactory nodeFactory = createHazelcastInstanceFactory(2);

        Config cfg = new Config();

        HazelcastInstance member1 = nodeFactory.newHazelcastInstance(cfg);
        HazelcastInstance member2 = nodeFactory.newHazelcastInstance(cfg);

        // Add some data.
        for (int i = 0; i < 100; i++)
            member1.getMap("persons").put(i, new Person(i));

        // Execute.
        HazelcastSql2 service = new HazelcastSql2(member1);

        RootConsumer consumer = service.execute2(QUERY);

        Iterator<Row> iter = consumer.iterator();

        while (iter.hasNext()) {
            Row row = iter.next();

            System.out.println(">>> ROW: " + row);
        }
    }

    @Test
    public void testSqlSimple() throws Exception {
        // Start several members.
        TestHazelcastInstanceFactory nodeFactory = createHazelcastInstanceFactory(2);

        Config cfg = new Config();

        HazelcastInstance member1 = nodeFactory.newHazelcastInstance(cfg);
        HazelcastInstance member2 = nodeFactory.newHazelcastInstance(cfg);

        // Insert data.
        for (int i = 0; i < 100; i++)
            member1.getMap("queryMap").put(i, new Person(i));

        HazelcastSql hazelcastSql = HazelcastSql.createFor(member1);

        Enumerable<Object> res = hazelcastSql.query(QUERY);

        for (Object object : res)
            System.out.println(object instanceof Object[] ? Arrays.deepToString((Object[]) object) : object);
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> asList(T... elems) {
        if (elems == null || elems.length == 0)
            return Collections.emptyList();

        ArrayList<T> res = new ArrayList<>(elems.length);

        Collections.addAll(res, elems);

        return res;
    }

    @SuppressWarnings("WeakerAccess")
    public static class Person implements Serializable {
        public final int __key;
        public final String name;
        public final int age;
        public final double height;
        public final boolean active;

        public Person(int key) {
            this.__key = key;
            this.name = "Person " + key;
            this.age = key;
            this.height = 100.5 + key;
            this.active = key % 2 == 0;
        }

    }
}