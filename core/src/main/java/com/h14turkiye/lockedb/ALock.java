package com.h14turkiye.lockedb;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.Getter;
import lombok.Setter;

/**
* Represents an abstract distributed lock with asynchronous operations.
* Implementations must provide mechanisms for acquiring, releasing, and checking lock status.
*/
public abstract class ALock {

    protected final String uuid = UUID.randomUUID().toString();
    
    /** The unique key identifying the lock. */
    @Getter @Setter protected String key;
    
    /**
    * This is an optional parameter. If not set, no password will be used.
    * When a password is set, an acquired lock can be re-acquired even if it is already locked,
    * provided the password matches.
    *
    * @param password the password to be used for authentication
    * @return the current instance of ALockBuilder for chaining
    */
    @Getter @Setter protected String password;
    
    /** The expiration time of the lock in milliseconds. */
    @Getter @Setter protected long expiresAfterMS;
    
    /** The timeout duration for acquiring the lock in milliseconds. */
    @Getter @Setter protected long timeoutMS;
    
    /** Executor service for handling asynchronous lock operations. */
    public static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    
    /**
    * Schedules a task to be executed after the specified delay.
    *
    * @param runnable the task to execute
    * @param delayMS the delay in milliseconds
    * @return a CompletableFuture that completes when the scheduled task is executed
    */
    public static CompletableFuture<Void> schedule(Runnable runnable, long delayMS) {
        return CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(delayMS);
                runnable.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, executor);
    }
    
    /**
    * Attempts to acquire the lock asynchronously.
    *
    * @return a CompletableFuture that resolves to:
    *         - {@code true} if the lock was successfully acquired.
    *         - {@code false} if the operation timed out before the lock could be acquired.
    *         - {@code null} if the lock was released before it could be acquired.
    */
    public abstract CompletableFuture<Boolean> acquire();
    
    /**
    * Releases the lock asynchronously.
    *
    * @return a CompletableFuture that resolves to {@code true} if the lock was successfully released, otherwise {@code false}.
    */
    public abstract CompletableFuture<Boolean> release();
    
    /**
    * Checks asynchronously whether the lock is currently held by any process.
    *
    * @return a CompletableFuture that resolves to {@code true} if the lock is currently held, otherwise {@code false}.
    */
    public abstract CompletableFuture<Boolean> isLocked();
    
    /**
    * Checks asynchronously whether the lock can be acquired.
    *
    * @return a CompletableFuture that resolves to {@code true} if the lock is acquirable, otherwise {@code false}.
    */
    public abstract CompletableFuture<Boolean> isAcquirable();

    protected CompletableFuture<Boolean> acquireFuture;

    protected void scheduleExpirationRemoval() {
        if (expiresAfterMS > 0) {
            schedule(() -> {
                try {
                    if (!acquireFuture.isDone() || acquireFuture.get()) {
                        release();
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }, expiresAfterMS);
        }
    }
    
}
