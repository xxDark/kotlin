// FIR_IDENTICAL
fun foo(<!UNUSED_PARAMETER!>x<!>: Int) = 1

val y = 2

foo(y)
