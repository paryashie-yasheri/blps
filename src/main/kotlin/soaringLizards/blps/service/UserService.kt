package soaringLizards.blps.service

import soaringLizards.blps.dto.user.UserDto
import java.util.UUID

class UserService {

    fun getMe(): UserDto {
        TODO("обратится к спрингсекюрити и оттуда достать")
    }

    fun getMyId(): UUID {
        TODO("достать из getMe айдишник")
    }

    fun findUserById(id: UUID): UserDto = TODO("Проверка прав + обращение к бд")


}