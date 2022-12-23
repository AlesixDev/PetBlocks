@file:Suppress("UNCHECKED_CAST", "CascadeIf", "FoldInitializerAndIfToElvis")

package com.github.shynixn.petblocks.core.logic.business.service

import com.github.shynixn.petblocks.api.legacy.business.annotation.YamlSerialize
import com.github.shynixn.petblocks.api.legacy.business.serializer.YamlSerializer
import com.github.shynixn.petblocks.api.legacy.business.service.YamlSerializationService
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.Reader
import java.io.Writer
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.util.LinkedHashMap
import kotlin.collections.ArrayList
import kotlin.collections.Collection
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableCollection
import kotlin.collections.forEach
import kotlin.collections.set
import kotlin.collections.sortedBy
import kotlin.collections.toTypedArray
import kotlin.reflect.KClass

/**
 * Created by Shynixn 2019.
 * <p>
 * Version 1.2
 * <p>
 * MIT License
 * <p>
 * Copyright (c) 2019 by Shynixn
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
class YamlSerializationServiceImpl : YamlSerializationService {
    /**
     * Serializes the given [instance] to the target [writer].
     */
    override fun serialize(instance: Any, writer: Writer) {
        val serializedContent = serialize(instance)

        val options = DumperOptions()
        options.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        options.isPrettyFlow = true

        val yaml = Yaml(options)
        yaml.dump(serializedContent, writer)
    }

    /**
     * DeSerializes the given [reader] into a new instance of the given [targetObjectClass].
     */
    override fun <R> deserialize(targetObjectClass: Any, reader: Reader): R {
        val yaml = Yaml()
        val serializedContent = yaml.load(reader) as Map<String, Any?>

        return deserialize(targetObjectClass, serializedContent)
    }

    /**
     * Serializes the given [instance] to a key value pair map.
     */
    override fun serialize(instance: Any): Map<String, Any?> {
        return if (instance.javaClass.isArray) {
            serializeArray(null, instance as Array<Any?>)
        } else if (Collection::class.java.isAssignableFrom(instance.javaClass)) {
            serializeCollection(null, instance as Collection<Any?>)
        } else if (Map::class.java.isAssignableFrom(instance.javaClass)) {
            serializeMap(instance as Map<Any, Any>)
        } else {
            serializeObject(instance)!!
        }
    }

    /**
     * DeSerializes the given [dataSource] into a new instance of the given [targetObjectClass].
     */
    override fun <R> deserialize(targetObjectClass: Any, dataSource: Map<String, Any?>): R {
        var objectClazz = targetObjectClass

        if (objectClazz is KClass<*>) {
            objectClazz = objectClazz.java
        }

        if (objectClazz !is Class<*>) {
            throw IllegalArgumentException("Deserialization class is not a java class $objectClazz.")
        }

        if (objectClazz.isInterface) {
            throw IllegalArgumentException("Use a class instead of the $objectClazz.")
        }

        val instance: R?

        try {
            instance = objectClazz.getDeclaredConstructor().newInstance() as R
        } catch (e: Exception) {
            throw IllegalArgumentException("Cannot instanciet the class $objectClazz. Does it have a default constructor?")
        }

        deserializeObject(instance!!, objectClazz, dataSource)

        return instance
    }

    /**
     *  Deserialize the given object collection.
     */
    private fun deserializeCollection(annotation: YamlSerialize, field: Field, collection: MutableCollection<Any?>, dataSource: Any) {
        if (dataSource is Map<*, *>) {
            dataSource.keys.forEach { key ->
                if (key is String && key.toIntOrNull() == null) {
                    throw IllegalArgumentException("Initializing " + annotation.value + " as collection failed as dataSource contains a invalid key.")
                }

                val value = dataSource[key]

                if (value == null) {
                    collection.add(null)
                } else if (getArgumentType(field, 0).isEnum) {
                    @Suppress("UPPER_BOUND_VIOLATED", "UNCHECKED_CAST")
                    collection.add(java.lang.Enum.valueOf<Any>(getArgumentType(field, 0) as Class<Any>, value.toString().toUpperCase()))
                } else if (isPrimitive(value.javaClass)) {
                    collection.add(value)
                } else if (annotation.customserializer != Any::class) {
                    collection.add((annotation.customserializer.java.getDeclaredConstructor().newInstance() as YamlSerializer<*, Map<String, Any?>>).onDeserialization(value as Map<String, Any?>))
                } else {
                    collection.add(deserialize(getArgumentType(field, 0) as Class<Any>, value as Map<String, Any?>))
                }
            }
        } else if (dataSource is Collection<*>) {
            dataSource.forEach { item ->
                collection.add(item)
            }
        } else {
            throw IllegalArgumentException("Initializing " + annotation.value + " from given data source did succeed.")
        }
    }

    /**
     *  Deserialize the given object map.
     */
    private fun deserializeMap(annotation: YamlSerialize, map: MutableMap<Any, Any?>, keyClazz: Class<*>, valueClazz: Class<*>, dataSource: Map<String, Any?>) {
        dataSource.keys.forEach { key ->
            val value = dataSource[key]

            val finalKey = if (keyClazz.isEnum) {
                @Suppress("UPPER_BOUND_VIOLATED", "UNCHECKED_CAST")
                java.lang.Enum.valueOf<Any>(keyClazz as Class<Any>, key.toUpperCase())
            } else if (isPrimitive(keyClazz)) {
                key
            } else {
                throw java.lang.IllegalArgumentException("Initializing " + annotation.value + " as map failed as map key is not primitiv or an enum.")
            }

            if (value == null) {
                map[finalKey] = null
            } else if (isPrimitive(value.javaClass)) {
                map[finalKey] = value
            } else {
                if (valueClazz.isInterface) {
                    if (annotation.implementation == Any::class) {
                        throw IllegalArgumentException("Map Value $annotation is an interface without deserialization implementation.")
                    }

                    map[finalKey] = deserialize(annotation.implementation.javaObjectType, value as Map<String, Any?>)
                } else {
                    map[finalKey] = deserialize(valueClazz, value as Map<String, Any?>)
                }
            }
        }
    }

    /**
     *  Deserialize the given object array.
     */
    private fun deserializeArray(annotation: YamlSerialize, field: Field, array: Array<Any?>, dataSource: Map<String, Any?>) {
        dataSource.keys.forEach { key ->
            if (key.toIntOrNull() == null) {
                throw java.lang.IllegalArgumentException("Initializing " + annotation.value + " as array failed as dataSource contains a invalid key.")
            }

            val keyPlace = key.toInt() - 1
            val value = dataSource[key]

            if (keyPlace < array.size) {
                if (value == null) {
                    array[keyPlace] = null
                } else if (annotation.customserializer != Any::class) {
                    array[keyPlace] =
                        (annotation.customserializer.java.getDeclaredConstructor().newInstance() as YamlSerializer<*, Map<String, Any?>>).onDeserialization(value as Map<String, Any?>)
                } else if (field.type.componentType.isEnum) {
                    @Suppress("UPPER_BOUND_VIOLATED", "UNCHECKED_CAST")
                    array[keyPlace] = java.lang.Enum.valueOf<Any>(field.type as Class<Any>, value.toString().toUpperCase())
                } else if (isPrimitive(value.javaClass)) {
                    array[keyPlace] = value
                } else {
                    array[keyPlace] = deserialize(value.javaClass, value as Map<String, Any?>)
                }
            }
        }
    }

    /**
     * Deserializes the given [instance] of the [instanceClazz] from the [dataSource].
     */
    private fun deserializeObject(instance: Any, instanceClazz: Class<*>, dataSource: Map<String, Any?>) {
        var runningClazz: Class<*>? = instanceClazz

        while (runningClazz != null) {
            runningClazz.declaredFields.forEach { field ->
                field.isAccessible = true
                field.declaredAnnotations.forEach { annotation ->
                    if (annotation.annotationClass == YamlSerialize::class) {
                        deserializeField(field, annotation as YamlSerialize, instance, dataSource)
                    }
                }
            }

            runningClazz = runningClazz.superclass
        }
    }

    /**
     * Deserializes a single field of an object.
     */
    private fun deserializeField(field: Field, annotation: YamlSerialize, instance: Any, dataSource: Map<String, Any?>) {
        if (!dataSource.containsKey(annotation.value)) {
            return
        }

        val value = dataSource[annotation.value]

        if (value == null) {
            field.set(instance, value)
        } else if (annotation.customserializer != Any::class && !field.type.isArray && !Collection::class.java.isAssignableFrom(field.type)) {
            val deserializedValue = (annotation.customserializer.java.getDeclaredConstructor().newInstance() as YamlSerializer<Any, Any>).onDeserialization(value)
            field.set(instance, deserializedValue)
        } else if (isPrimitive(field.type)) {
            field.set(instance, value)
        } else if (field.type.isEnum) run {
            @Suppress("UPPER_BOUND_VIOLATED", "UNCHECKED_CAST")
            field.set(instance, java.lang.Enum.valueOf<Any>(field.type as Class<Any>, value.toString().toUpperCase()))
        }
        else if (field.type.isArray) {
            val array = field.get(instance)

            if (array == null) {
                throw IllegalArgumentException("Array field " + field.name + " should already be initialized with a certain array.")
            }

            deserializeArray(annotation, field, array as Array<Any?>, value as Map<String, Any?>)
        } else if (Collection::class.java.isAssignableFrom(field.type)) {
            val collection = field.get(instance)

            if (collection == null) {
                throw IllegalArgumentException("Collection field " + field.name + " should already be initialized with a certain collection.")
            }

            (collection as MutableCollection<Any?>).clear()

            deserializeCollection(annotation, field, collection, value)
        } else if (Map::class.java.isAssignableFrom(field.type)) {
            val map = field.get(instance)

            if (map == null) {
                throw IllegalArgumentException("Map field " + field.name + " should already be initialized with a certain map.")
            }

            (map as MutableMap<Any, Any?>).clear()

            deserializeMap(annotation, map, getArgumentType(field, 0), getArgumentType(field, 1), value as Map<String, Any?>)
        } else {
            val instanceClazz: Class<*> = if (field.type.isInterface) {
                if (annotation.implementation == Any::class) {
                    throw IllegalArgumentException("Type of field " + field.name + " is an interface without deserialization implementation.")
                }

                annotation.implementation.java
            } else {
                field.type
            }

            field.set(instance, deserialize(instanceClazz, value as Map<String, Any?>))
        }
    }

    /**
     * Serializes the given [instance] into a key value pair map.
     */
    private fun serializeCollection(annotation: YamlSerialize?, instance: Collection<Any?>): Map<String, Any?> {
        return serializeArray(annotation, instance.toTypedArray())
    }

    /**
     * Serializes the given [instances] into a key value pair map.
     */
    private fun serializeArray(annotation: YamlSerialize?, instances: Array<Any?>): Map<String, Any?> {
        val data = LinkedHashMap<String, Any?>()

        for (i in 1..instances.size) {
            val instance = instances[i - 1]

            if (instance == null) {
                data[i.toString()] = null
            } else if (isPrimitive(instance::class.java)) {
                data[i.toString()] = instance
            } else if (annotation != null && annotation.customserializer != Any::class) {
                data[i.toString()] = (annotation.customserializer.java.getDeclaredConstructor().newInstance() as YamlSerializer<Any, Any>).onSerialization(instance)
            } else if (instance::class.java.isEnum) {
                data[i.toString()] = (instance as Enum<*>).name
            } else {
                data[i.toString()] = serialize(instance)
            }
        }

        return data
    }

    /**
     * Serializes the given [instance] map into a key value pair map.
     */
    private fun serializeMap(instance: Map<Any, Any?>): Map<String, Any?> {
        val data = LinkedHashMap<String, Any?>()

        for (key in instance.keys) {

            if (isPrimitive(key::class.java)) {
                data[key.toString()] = serialize(instance[key]!!)
            } else if (key.javaClass.isEnum) {
                val value = instance[key]

                if (value == null) {
                    data[(key as Enum<*>).name] = null
                } else if (isPrimitive(value.javaClass)) {
                    data[(key as Enum<*>).name] = value
                } else {
                    data[(key as Enum<*>).name] = serialize(value)
                }
            } else {
                throw IllegalArgumentException("Given map [$instance] does not contain a primitive type key or enum key.")
            }
        }

        return data
    }

    /**
     * Serializes the given [instance] into a key value pair map.
     */
    private fun serializeObject(instance: Any?): Map<String, Any?>? {
        if (instance == null) {
            return null
        }

        val data = LinkedHashMap<String, Any?>()

        getOrderedAnnotations(instance::class.java).forEach { element ->
            val field = element.second
            val yamlAnnotation = element.first

            if (field.get(instance) == null) {
            } else if (yamlAnnotation.customserializer != Any::class && !field.type.isArray && !Collection::class.java.isAssignableFrom(field.type)) {
                val serializedValue = (yamlAnnotation.customserializer.java.getDeclaredConstructor().newInstance() as YamlSerializer<Any, Any>).onSerialization(field.get(instance))
                data[yamlAnnotation.value] = serializedValue
            } else if (isPrimitive(field.type)) {
                data[yamlAnnotation.value] = field.get(instance)
            } else if (field.type.isEnum || field.type == Enum::class.java) {
                data[yamlAnnotation.value] = (field.get(instance) as Enum<*>).name.toUpperCase()
            } else if (field.type.isArray) {
                data[yamlAnnotation.value] = serializeArray(yamlAnnotation, field.get(instance) as Array<Any?>)
            } else if (Collection::class.java.isAssignableFrom(field.type)) {
                if (getArgumentType(field, 0) == String::class.java) {
                    data[yamlAnnotation.value] = field.get(instance)
                } else {
                    data[yamlAnnotation.value] = serializeCollection(yamlAnnotation, field.get(instance) as Collection<*>)
                }
            } else if (Map::class.java.isAssignableFrom(field.type)) {
                data[yamlAnnotation.value] = serializeMap(field.get(instance) as Map<Any, Any?>)
            } else {
                data[yamlAnnotation.value] = serializeObject(field.get(instance))
            }
        }

        return data
    }

    /**
     * Gets all yaml annotations ordered from a class.
     */
    private fun getOrderedAnnotations(clazz: Class<*>): List<Pair<YamlSerialize, Field>> {
        val result = ArrayList<Pair<YamlSerialize, Field>>()
        var runningClazz: Class<*>? = clazz

        while (runningClazz != null) {
            runningClazz.declaredFields.forEach { field ->
                field.isAccessible = true
                field.declaredAnnotations.forEach { annotation ->
                    if (annotation.annotationClass == YamlSerialize::class) {
                        result.add(Pair(annotation as YamlSerialize, field))
                    }
                }
            }

            runningClazz = runningClazz.superclass
        }

        return result.sortedBy { param -> param.first.orderNumber }
    }

    /**
     * Gets the type of the argument at the given [number] index.
     */
    private fun getArgumentType(field: Field, number: Int): Class<*> {
        return (field.genericType as ParameterizedType).actualTypeArguments[number] as Class<*>
    }

    /**
     * Gets if the given [clazz] is a primitive class.
     */
    private fun isPrimitive(clazz: Class<*>): Boolean {
        return clazz.isPrimitive || clazz == String::class.java || clazz == Int::class.java || clazz == Double::class.java || clazz == Long::class.java || clazz == Float::class.java
    }
}
