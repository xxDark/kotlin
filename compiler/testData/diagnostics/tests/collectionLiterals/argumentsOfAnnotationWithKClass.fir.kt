// !WITH_NEW_INFERENCE
// !LANGUAGE: +ArrayLiteralsInAnnotations +BareArrayClassLiteral

import kotlin.reflect.KClass

annotation class Foo(val a: Array<KClass<*>> = [])

class Gen<T>

annotation class Bar(val a: Array<KClass<*>> = [Int::class, Array<Int>::class, Gen::class])

@Foo([])
fun test1() {}

<!INAPPLICABLE_CANDIDATE!>@Foo([Int::class, String::class])<!>
fun test2() {}

<!INAPPLICABLE_CANDIDATE!>@Foo([Array::class])<!>
fun test3() {}

<!INAPPLICABLE_CANDIDATE!>@Foo([Gen<Int>::class])<!>
fun test4() {}

<!INAPPLICABLE_CANDIDATE!>@Foo([""])<!>
fun test5() {}

<!INAPPLICABLE_CANDIDATE!>@Foo([Int::class, 1])<!>
fun test6() {}

@Bar
fun test7() {}
