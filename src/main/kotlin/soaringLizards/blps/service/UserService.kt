package soaringLizards.blps.service

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import soaringLizards.blps.dto.user.UserDto
import soaringLizards.blps.dto.user.requests.RegisterDto
import soaringLizards.blps.exception.UserAlreadyExistAuthenticationException
import soaringLizards.blps.repository.UserRepository
import java.util.UUID

@Service
class UserService(
    private val passwordEncoder: PasswordEncoder,
    private val userRepository: UserRepository,
) {
    fun getMe(): UserDto {
        TODO("обратится к спрингсекюрити и оттуда достать")
    }

    fun getMyId(): UUID {
        TODO("достать из getMe айдишник")
    }

    fun findUserById(id: UUID): UserDto = TODO("Проверка прав + обращение к бд")

    fun registerUser(request: RegisterDto) {
        if (userRepository.findByUsername(request.username) != null) {
            throw UserAlreadyExistAuthenticationException("User already exists");
        }

        val newUser = UserDto(
            request.username,
            passwordEncoder.encode(request.password)!!,
            request.email,
            "USER"
        )

        userRepository.insert(newUser);
    }
}
