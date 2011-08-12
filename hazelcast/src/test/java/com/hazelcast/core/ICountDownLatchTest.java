/*
 * Copyright (c) 2008-2010, Hazel Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.hazelcast.core;

import com.hazelcast.impl.CountDownLatchProxy;
import org.junit.*;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

@RunWith(com.hazelcast.util.RandomBlockJUnit4ClassRunner.class)
public class ICountDownLatchTest {

    @BeforeClass
    @AfterClass
    public static void init() throws Exception {
        Hazelcast.shutdownAll();
    }

    @Before
    public void setUp() throws Exception {
        Hazelcast.shutdownAll();
    }

    @After
    public void tearDown() throws Exception {
        Hazelcast.shutdownAll();
    }

    @Test
    public void testCountDownLatchSimple() throws InterruptedException {
        HazelcastInstance h1 = Hazelcast.newHazelcastInstance(null);
        HazelcastInstance h2 = Hazelcast.newHazelcastInstance(null);
        final ICountDownLatch cdl1 = h1.getCountDownLatch("test");
        final ICountDownLatch cdl2 = h2.getCountDownLatch("test");
        Member h1Member = h1.getCluster().getLocalMember();
        final AtomicInteger result = new AtomicInteger();
        int count = 5;
        assertTrue(cdl1.setCount(count));
        assertEquals(count, ((CountDownLatchProxy) cdl2).getCount());
        assertEquals(h1Member, ((CountDownLatchProxy) cdl1).getOwner());
        assertEquals(h1Member, ((CountDownLatchProxy) cdl2).getOwner());
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    if (cdl2.await(1000, TimeUnit.MILLISECONDS))
                        result.incrementAndGet();
                } catch (Throwable e) {
                    e.printStackTrace();
                    fail();
                }
            }
        };
        thread.start();
        for (int i = count; i > 0; i--) {
            assertEquals(i, ((CountDownLatchProxy) cdl2).getCount());
            cdl1.countDown();
            Thread.sleep(100);
        }
        assertEquals(1, result.get());
    }

    @Test
    public void testCountDownLatchOwnerLeft() throws InterruptedException {
        HazelcastInstance h1 = Hazelcast.newHazelcastInstance(null);
        HazelcastInstance h2 = Hazelcast.newHazelcastInstance(null);
        final ICountDownLatch cdl1 = h1.getCountDownLatch("test");
        final ICountDownLatch cdl2 = h2.getCountDownLatch("test");
        Member h2Member = h2.getCluster().getLocalMember();
        final AtomicInteger result = new AtomicInteger();
        assertNull(((CountDownLatchProxy) cdl1).getOwner());
        assertNull(((CountDownLatchProxy) cdl2).getOwner());
        assertTrue(cdl2.setCount(1));
        assertEquals(1, ((CountDownLatchProxy) cdl1).getCount());
        assertEquals(1, ((CountDownLatchProxy) cdl2).getCount());
        assertEquals(h2Member, ((CountDownLatchProxy) cdl1).getOwner());
        assertEquals(h2Member, ((CountDownLatchProxy) cdl2).getOwner());
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    cdl1.await();
                    fail();
                } catch (MemberLeftException e) {
                    result.incrementAndGet();
                } catch (Throwable e) {
                    e.printStackTrace();
                    fail();
                }
            }
        };
        thread.start();
        Thread.sleep(20);
        h2.shutdown();
        thread.join();
        assertEquals(1, result.get());
    }

    @Test
    public void testCountDownLatchInstanceDestroyed() throws InterruptedException {
        HazelcastInstance h1 = Hazelcast.newHazelcastInstance(null);
        HazelcastInstance h2 = Hazelcast.newHazelcastInstance(null);
        final ICountDownLatch cdl1 = h1.getCountDownLatch("test");
        final ICountDownLatch cdl2 = h2.getCountDownLatch("test");
        Member h1Member = h1.getCluster().getLocalMember();
        final AtomicInteger result = new AtomicInteger();
        cdl1.setCount(1);
        assertEquals(1, ((CountDownLatchProxy) cdl2).getCount());
        assertEquals(h1Member, ((CountDownLatchProxy) cdl1).getOwner());
        assertEquals(h1Member, ((CountDownLatchProxy) cdl2).getOwner());
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    cdl2.await();
                    fail();
                } catch (InstanceDestroyedException e) {
                    result.incrementAndGet();
                } catch (Throwable e) {
                    e.printStackTrace();
                    fail();
                }
            }
        };
        thread.start();
        Thread.sleep(20);
        cdl1.destroy();
        thread.join();
        assertEquals(1, result.get());
    }

    @Test
    public void testCountDownLatchHazelcastShutdown() throws InterruptedException {
        HazelcastInstance h1 = Hazelcast.newHazelcastInstance(null);
        HazelcastInstance h2 = Hazelcast.newHazelcastInstance(null);
        final ICountDownLatch cdl1 = h1.getCountDownLatch("test");
        final ICountDownLatch cdl2 = h2.getCountDownLatch("test");
        Member h1Member = h1.getCluster().getLocalMember();
        final AtomicInteger result = new AtomicInteger();
        cdl1.setCount(1);
        assertEquals(1, ((CountDownLatchProxy) cdl2).getCount());
        assertEquals(h1Member, ((CountDownLatchProxy) cdl1).getOwner());
        assertEquals(h1Member, ((CountDownLatchProxy) cdl2).getOwner());
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    cdl1.await();
                    fail();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    result.incrementAndGet();
                } catch (Throwable e) {
                    e.printStackTrace();
                    fail();
                }
            }
        };
        thread.start();
        Thread.sleep(20);
        h1.shutdown();
        thread.join();
        assertEquals(1, result.get());
    }
}
