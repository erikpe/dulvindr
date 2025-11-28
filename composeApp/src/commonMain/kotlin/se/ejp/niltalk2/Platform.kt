package se.ejp.niltalk2

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform