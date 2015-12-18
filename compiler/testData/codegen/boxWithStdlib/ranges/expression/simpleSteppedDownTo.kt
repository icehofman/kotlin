// Auto-generated by org.jetbrains.kotlin.generators.tests.GenerateRangesCodegenTestData. DO NOT EDIT!
import java.util.ArrayList

fun box(): String {
    val list1 = ArrayList<Int>()
    val range1 = 9 downTo 3 step 2
    for (i in range1) {
        list1.add(i)
        if (list1.size > 23) break
    }
    if (list1 != listOf<Int>(9, 7, 5, 3)) {
        return "Wrong elements for 9 downTo 3 step 2: $list1"
    }

    val list2 = ArrayList<Int>()
    val range2 = 9.toByte() downTo 3.toByte() step 2
    for (i in range2) {
        list2.add(i)
        if (list2.size > 23) break
    }
    if (list2 != listOf<Int>(9, 7, 5, 3)) {
        return "Wrong elements for 9.toByte() downTo 3.toByte() step 2: $list2"
    }

    val list3 = ArrayList<Int>()
    val range3 = 9.toShort() downTo 3.toShort() step 2
    for (i in range3) {
        list3.add(i)
        if (list3.size > 23) break
    }
    if (list3 != listOf<Int>(9, 7, 5, 3)) {
        return "Wrong elements for 9.toShort() downTo 3.toShort() step 2: $list3"
    }

    val list4 = ArrayList<Long>()
    val range4 = 9.toLong() downTo 3.toLong() step 2.toLong()
    for (i in range4) {
        list4.add(i)
        if (list4.size > 23) break
    }
    if (list4 != listOf<Long>(9, 7, 5, 3)) {
        return "Wrong elements for 9.toLong() downTo 3.toLong() step 2.toLong(): $list4"
    }

    val list5 = ArrayList<Char>()
    val range5 = 'g' downTo 'c' step 2
    for (i in range5) {
        list5.add(i)
        if (list5.size > 23) break
    }
    if (list5 != listOf<Char>('g', 'e', 'c')) {
        return "Wrong elements for 'g' downTo 'c' step 2: $list5"
    }

    return "OK"
}