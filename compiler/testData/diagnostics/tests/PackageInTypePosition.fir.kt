// !DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: a.kt

package foo

// FILE: b.kt

<!UNRESOLVED_REFERENCE!>@foo<!> fun bar(p: foo): foo = null!!
