package net.gridmi.api.notifier.app.utils

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder

object Json {

    val gson: Gson = with(GsonBuilder()) {
        val strategy = AnnotationExclusionStrategy()
        addDeserializationExclusionStrategy(strategy)
        addSerializationExclusionStrategy(strategy)
        setExclusionStrategies(strategy)
        create()
    }

    inline fun <reified T> String.fromJson(): T {
        val i = gson.fromJson(this, T::class.java)
        (i as? OnAfterInit)?.onAfterInit()
        return i
    }

    inline fun <reified T> ByteArray.fromJson(): T {
        return toString(Charsets.UTF_8).fromJson()
    }

    fun Any.toJson(): String = gson.toJson(this)

    class AnnotationExclusionStrategy : ExclusionStrategy {

        override fun shouldSkipField(f: FieldAttributes): Boolean {
            return f.getAnnotation(Exclude::class.java) != null
        }

        override fun shouldSkipClass(clazz: Class<*>?): Boolean {
            return false
        }

    }

    @Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Exclude

    interface OnAfterInit {
        fun onAfterInit()
    }

}