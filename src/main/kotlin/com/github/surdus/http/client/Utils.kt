package com.github.surdus.http.client

import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class MapFuture<V, T>(private val inner: Future<V>,
                      private val map: (V) -> T): Future<T> {

    override fun get(): T {
        return inner.get().run(map)
    }

    override fun get(timeout: Long, unit: TimeUnit?): T {
        return inner.get(timeout, unit).run(map)
    }

    override fun isDone(): Boolean {
        return inner.isDone
    }

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        return inner.cancel(mayInterruptIfRunning)
    }

    override fun isCancelled(): Boolean {
        return inner.isCancelled
    }
}

fun <V, T> Future<V>.map(eval: (V) -> T): Future<T> {
    return MapFuture(this, eval)
}
