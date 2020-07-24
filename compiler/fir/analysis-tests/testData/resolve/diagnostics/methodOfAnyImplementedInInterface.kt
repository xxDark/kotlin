interface A {
    <!ANY_METHOD_IMPLEMENTED_IN_INTERFACE!>override fun toString() = "Hello"<!>

    <!ANY_METHOD_IMPLEMENTED_IN_INTERFACE!>override fun equals(other: Any?) = true<!>

    <!ANY_METHOD_IMPLEMENTED_IN_INTERFACE!>override fun hashCode(): Int {
        return 42;
    }<!>

    override fun toString(): String

    override fun equals(other: Any?): Boolean

    override fun hashCode(): Int

    override operator fun toString(): String

    override operator fun equals(other: Any?): Boolean

    override operator fun hashCode(): Int
}