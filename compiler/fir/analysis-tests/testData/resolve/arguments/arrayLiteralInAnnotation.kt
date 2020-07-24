annotation class Ann(val stings: Array<String>)

const val s = "c"

@Ann(["a", "b", s])
fun test_1() {}
