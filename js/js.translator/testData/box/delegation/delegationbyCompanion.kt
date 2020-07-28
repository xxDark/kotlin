// EXPECTED_REACHABLE_NODES: 1253
val ooo = object : DDD {
    override fun bar(d: String): String {
        return "O" + d
    }
}

interface II {
    companion object : DDD by ooo
}

interface DDD {
    fun bar(d: String = "K"): String
}


fun box() : String {
    return II.bar()
}