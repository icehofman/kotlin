package kotlin.collections

/**
 * Represents a source of elements with a [keySelector] function, which can be applied to each element to get its key.
 *
 * A [Grouping] structure serves as an intermediate step in group-and-fold operations.
 * It's created by attaching [keySelector] function to a source of values.
 * To get an instance of [Grouping] use one of `groupingBy` extension functions:
 * - [Iterable.groupingBy]
 * - [Sequence.groupingBy]
 * - [Array.groupingBy]
 * - [CharSequence.groupingBy]
 *
 * For the list of group-and-fold operations available, see the extension functions for [Grouping].
 */
public interface Grouping<T, out K> {
    /** Returns an [Iterator] which returns the elements from the source. */
    fun elementIterator(): Iterator<T>
    /** Extracts the key of an [element]. */
    fun keySelector(element: T): K
}

/**
 * Groups elements from the source by the key and aggregates elements of each group with the specified [operation].
 *
 * The key for each element is provided by the [Grouping.keySelector] function.
 *
 * @param operation function is invoked on each element with the following parameters:
 *  - `key`: the key of a group this element belongs to;
 *  - `value`: the current value of the accumulator of a group, can be `null` if it's first `element` encountered in the group;
 *  - `element`: the element from the source being aggregated;
 *  - `first`: indicates whether it's first `element` encountered in the group.
 *
 * @return a [Map] associating the key of each group with the result of aggregation of the group values.
 */
public inline fun <T, K, R> Grouping<T, K>.aggregate(
        operation: (key: K, value: R?, element: T, first: Boolean) -> R
): Map<K, R> {
    val result = mutableMapOf<K, R>()
    for (e in this.elementIterator()) {
        val key = keySelector(e)
        val value = result[key]
        result[key] = operation(key, value, e, value == null && !result.containsKey(key))
    }
    return result
}

/**
 * Groups elements from the source by the key and aggregates elements of each group with the specified [operation]
 * to the given [destination] map.
 *
 * The key for each element is provided by the [Grouping.keySelector] function.
 *
 * @param operation a function that is invoked on each element with the following parameters:
 *  - `key`: the key of a group this element belongs to;
 *  - `accumulator`: the current value of the accumulator of the group, can be `null` if it's first `element` encountered in the group;
 *  - `element`: the element from the source being aggregated;
 *  - `first`: indicates whether it's first `element` encountered in the group.
 *
 * If the [destination] map already has a value corresponding to some key,
 * then the elements being aggregated for that key are never considered as `first`.
 *
 * @return the [destination] map associating the key of each group with the result of aggregation of the group values.
 */
public inline fun <T, K, R, M : MutableMap<in K, R>> Grouping<T, K>.aggregateTo(
        destination: M,
        operation: (key: K, accumulator: R?, element: T, first: Boolean) -> R
): M {
    for (e in this.elementIterator()) {
        val key = keySelector(e)
        val acc = destination[key]
        destination[key] = operation(key, acc, e, acc == null && !destination.containsKey(key))
    }
    return destination
}

/**
 * Groups elements from the source by the key and folds elements of each group with the specified [operation].
 *
 * Each group is folded with an initial value of accumulator provided by the [initialValueSelector] function.
 *
 * @param initialValueSelector a function that provides an initial value of accumulator for an each group.
 *  It's invoked with parameters:
 *  - `key`: the key of a group;
 *  - `element`: the first element being encountered in that group.
 *
 * @param operation a function that is invoked on each element with the following parameters:
 *  - `key`: the key of a group this element belongs to;
 *  - `accumulator`: the current value of the accumulator of the group;
 *  - `element`: the element from the source being folded;
 *
 * @return a [Map] associating the key of each group with the result of folding of the group values.
 */
public inline fun <T, K, R> Grouping<T, K>.fold(
        initialValueSelector: (key: K, element: T) -> R,
        operation: (key: K, accumulator: R, element: T) -> R
): Map<K, R> =
        aggregate { key, value, e, first -> operation(key, if (first) initialValueSelector(key, e) else value as R, e) }

/**
 * Groups elements from the source by the key and folds elements of each group with the specified [operation]
 * to the given [destination] map.
 *
 * Each group is folded with an initial value of accumulator provided by the [initialValueSelector] function.
 *
 * @param initialValueSelector a function that provides an initial value of accumulator for an each group.
 *  It's invoked with parameters:
 *  - `key`: the key of a group;
 *  - `element`: the first element being encountered in that group.
 *
 * If the [destination] map already has a value corresponding to some key,
 * then the [initialValueSelector] function is never invoked for that key.
 *
 * @param operation a function that is invoked on each element with the following parameters:
 *  - `key`: the key of a group this element belongs to;
 *  - `accumulator`: the current value of the accumulator of the group;
 *  - `element`: the element from the source being folded;
 *
 * @return the [destination] map associating the key of each group with the result of folding of the group values.
 */
public inline fun <T, K, R, M : MutableMap<in K, R>> Grouping<T, K>.foldTo(
        destination: M,
        initialValueSelector: (key: K, element: T) -> R,
        operation: (key: K, accumulator: R, element: T) -> R
): M =
        aggregateTo(destination) { key, value, e, first -> operation(key, if (first) initialValueSelector(key, e) else value as R, e) }


/**
 * Groups elements from the source by the key and folds elements of each group with the specified [operation].
 *
 * Each group is folded with the [initialValue] used as the initial value of accumulator.
 *
 * @param operation a function that is invoked on each element with the following parameters:
 *  - `accumulator`: the current value of the accumulator of the group;
 *  - `element`: the element from the source being folded;
 *
 * @return a [Map] associating the key of each group with the result of folding of the group values.
 */
public inline fun <T, K, R> Grouping<T, K>.fold(
        initialValue: R,
        operation: (accumulator: R, element: T) -> R
): Map<K, R> =
        aggregate { k, v, e, first -> operation(if (first) initialValue else v as R, e) }

/**
 * Groups elements from the source by the key and folds elements of each group with the specified [operation]
 * to the given [destination] map.
 *
 * Each group is folded with the [initialValue] used as the initial value of accumulator or the value
 * the [destination] map already has corresponding to the key of that group.
 *
 * @param operation a function that is invoked on each element with the following parameters:
 *  - `accumulator`: the current value of the accumulator of the group;
 *  - `element`: the element from the source being folded;
 *
 * @return a [Map] associating the key of each group with the result of folding of the group values.
 */
public inline fun <T, K, R, M : MutableMap<in K, R>> Grouping<T, K>.foldTo(
        destination: M,
        initialValue: R,
        operation: (accumulator: R, element: T) -> R
): M =
        aggregateTo(destination) { k, v, e, first -> operation(if (first) initialValue else v as R, e) }


public inline fun <S, T : S, K> Grouping<T, K>.reduce(operation: (K, S, T) -> S): Map<K, S> =
        aggregate { key, value, e, first ->
            if (first) e else operation(key, value as S, e)
        }

public inline fun <S, T : S, K, M : MutableMap<in K, S>> Grouping<T, K>.reduceTo(destination: M, operation: (K, S, T) -> S): M =
        aggregateTo(destination) { key, value, e, first ->
            if (first) e else operation(key, value as S, e)
        }


// TODO: optimize allocations with IntRef
public fun <T, K> Grouping<T, K>.countEach(): Map<K, Int> = fold(0) { acc, e -> acc + 1 }

public fun <T, K, M : MutableMap<in K, Int>> Grouping<T, K>.countEachTo(destination: M): M = foldTo(destination, 0) { acc, e -> acc + 1 }

// TODO: optimize with IntRef
public inline fun <T, K> Grouping<T, K>.sumEachBy(valueSelector: (T) -> Int): Map<K, Int> =
        fold(0) { acc, e -> acc + valueSelector(e)}

public inline fun <T, K, M : MutableMap<in K, Int>> Grouping<T, K>.sumEachByTo(destination: M, valueSelector: (T) -> Int): M =
        foldTo(destination, 0) { acc, e -> acc + valueSelector(e)}

/*

// TODO: sum by long and by double overloads

public inline fun <T, K, M : MutableMap<in K, Long>> Grouping<T, K>.sumEachByLongTo(destination: M, valueSelector: (T) -> Long): M =
        foldTo(destination, 0L) { acc, e -> acc + valueSelector(e)}

public inline fun <T, K> Grouping<T, K>.sumEachByLong(valueSelector: (T) -> Long): Map<K, Long> =
        fold(0L) { acc, e -> acc + valueSelector(e)}

public inline fun <T, K, M : MutableMap<in K, Double>> Grouping<T, K>.sumEachByDoubleTo(destination: M, valueSelector: (T) -> Double): M =
        foldTo(destination, 0.0) { acc, e -> acc + valueSelector(e)}

public inline fun <T, K> Grouping<T, K>.sumEachByDouble(valueSelector: (T) -> Double): Map<K, Double> =
        fold(0.0) { acc, e -> acc + valueSelector(e)}*/
