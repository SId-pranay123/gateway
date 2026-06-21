package dev.siddharth.gateway.ratelimit;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

class TokenBucketTest {

    @Test
    void allowsRequestsUpToCapacity() {
        TokenBucket bucket = new TokenBucket(5, 0.5);

        for (int i = 0; i < 5; i++) {
            assertTrue(bucket.tryConsume());
        }
        assertFalse(bucket.tryConsume()); // 6th should fail
    }

    @Test
    void refillsTokensOverTime() throws InterruptedException{
        TokenBucket bucket = new TokenBucket(5, 0.5); // 1 token per 2s

        for (int i = 0; i < 5; i++) {
            assertTrue(bucket.tryConsume());
        }
        assertFalse(bucket.tryConsume()); // bucket empty
    
        Thread.sleep(2100); // wait just over 2s for 1 token to refill
    
        assertTrue(bucket.tryConsume()); // should succeed now
        assertFalse(bucket.tryConsume()); // and empty again
    }

    @Test
    void neverAllowsMoreThanCapacityUnderConcurrency() throws InterruptedException {
        TokenBucket bucket = new TokenBucket(5, 0.0); // no refill, isolate concurrency behavior
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threadCount);
    
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                if (bucket.tryConsume()) {
                    successCount.incrementAndGet();
                }
                latch.countDown();
            });
        }
    
        latch.await();
        executor.shutdown();
    
        assertEquals(5, successCount.get());
    }
 }