interface A1 : B

interface A2 : <!SUPERTYPE_INITIALIZED_IN_INTERFACE!>B<!>()

class A3 : B, B

enum class A4 : B
