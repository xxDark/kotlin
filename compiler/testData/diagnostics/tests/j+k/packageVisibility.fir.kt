//FILE: a/MyJavaClass.java
package a;

class MyJavaClass {
    static int staticMethod() {
        return 1;
    }

    static class NestedClass {
        static int staticMethodOfNested() {
            return 1;
        }
    }
}

//FILE:a.kt
package a

val mc = MyJavaClass()
val x = MyJavaClass.staticMethod()
val y = MyJavaClass.NestedClass.staticMethodOfNested()
val z = MyJavaClass.NestedClass()

//FILE: b.kt
package b

import a.MyJavaClass

val mc1 = <!HIDDEN!>MyJavaClass<!>()

val x = <!HIDDEN!>MyJavaClass<!>.<!HIDDEN!>staticMethod<!>()
val y = MyJavaClass.<!HIDDEN!>NestedClass<!>.<!HIDDEN!>staticMethodOfNested<!>()
val z = <!HIDDEN!>MyJavaClass<!>.<!HIDDEN!>NestedClass<!>()

//FILE: c.kt
package a.c

import a.MyJavaClass

val mc1 = <!HIDDEN!>MyJavaClass<!>()

val x = <!HIDDEN!>MyJavaClass<!>.<!HIDDEN!>staticMethod<!>()
val y = MyJavaClass.<!HIDDEN!>NestedClass<!>.<!HIDDEN!>staticMethodOfNested<!>()
val z = <!HIDDEN!>MyJavaClass<!>.<!HIDDEN!>NestedClass<!>()
