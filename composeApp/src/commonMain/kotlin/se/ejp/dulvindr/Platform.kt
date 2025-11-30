package se.ejp.dulvindr

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform