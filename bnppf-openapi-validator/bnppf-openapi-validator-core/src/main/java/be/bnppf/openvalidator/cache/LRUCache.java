package be.bnppf.openvalidator.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe LRU (Least Recently Used) cache implementation.
 * Used for caching path mappings between exposed paths and specification paths.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class LRUCache<K, V> {

    private static final int DEFAULT_MAX_SIZE = 1000;
    private static final float LOAD_FACTOR = 0.75f;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final LinkedHashMap<K, V> cache;
    private volatile int maxSize;

    /**
     * Creates an LRU cache with the default maximum size of 1000 entries.
     */
    public LRUCache() {
        this(DEFAULT_MAX_SIZE);
    }

    /**
     * Creates an LRU cache with the specified maximum size.
     *
     * @param maxSize the maximum number of entries in the cache
     */
    public LRUCache(int maxSize) {
        this.maxSize = maxSize;
        this.cache = new LinkedHashMap<K, V>(16, LOAD_FACTOR, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > LRUCache.this.maxSize;
            }
        };
    }

    /**
     * Associates the specified value with the specified key in this cache.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with key, or null
     */
    public V put(K key, V value) {
        lock.writeLock().lock();
        try {
            return cache.put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or null if this cache contains no mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the key is mapped, or null
     */
    public V get(K key) {
        lock.readLock().lock();
        try {
            return cache.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns true if this cache contains a mapping for the specified key.
     *
     * @param key key whose presence in this cache is to be tested
     * @return true if this cache contains a mapping for the specified key
     */
    public boolean containsKey(K key) {
        lock.readLock().lock();
        try {
            return cache.containsKey(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Removes the mapping for the specified key from this cache if present.
     *
     * @param key key whose mapping is to be removed from the cache
     * @return the previous value associated with key, or null
     */
    public V remove(K key) {
        lock.writeLock().lock();
        try {
            return cache.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes all of the mappings from this cache.
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            cache.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns the number of key-value mappings in this cache.
     *
     * @return the number of key-value mappings in this cache
     */
    public int size() {
        lock.readLock().lock();
        try {
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the maximum size of this cache.
     *
     * @return the maximum number of entries
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * Sets the maximum size of this cache.
     * If the new size is smaller than the current number of entries,
     * the cache will be trimmed on the next put operation.
     *
     * @param maxSize the new maximum size
     */
    public void setMaxSize(int maxSize) {
        if (maxSize < 1) {
            throw new IllegalArgumentException("maxSize must be at least 1");
        }
        this.maxSize = maxSize;
    }

    /**
     * Returns true if this cache contains no key-value mappings.
     *
     * @return true if this cache contains no key-value mappings
     */
    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return cache.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * If the specified key is not already associated with a value,
     * associates it with the given value.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or null
     */
    public V putIfAbsent(K key, V value) {
        lock.writeLock().lock();
        try {
            V existing = cache.get(key);
            if (existing == null) {
                cache.put(key, value);
                return null;
            }
            return existing;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or defaultValue if this cache contains no mapping for the key.
     *
     * @param key          the key whose associated value is to be returned
     * @param defaultValue the default mapping of the key
     * @return the value to which the key is mapped, or defaultValue
     */
    public V getOrDefault(K key, V defaultValue) {
        lock.readLock().lock();
        try {
            V value = cache.get(key);
            return value != null ? value : defaultValue;
        } finally {
            lock.readLock().unlock();
        }
    }
}
