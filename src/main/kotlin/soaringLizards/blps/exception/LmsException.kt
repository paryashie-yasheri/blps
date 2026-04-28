package soaringLizards.blps.exception

class LmsException(val errors: Map<String, String>) : RuntimeException()
