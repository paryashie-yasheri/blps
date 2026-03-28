package soaringLizards.blps.service

import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import soaringLizards.blps.dto.user.RegisterDto
import soaringLizards.blps.dto.user.UserDto
import soaringLizards.blps.dto.user.UserResponse
import soaringLizards.blps.dto.user.toUserResponse
import soaringLizards.blps.exception.UserAlreadyExistAuthenticationException
import soaringLizards.blps.exception.UserNotFoundException
import soaringLizards.blps.repository.UserRepository

@Service
class UserService(
    private val passwordEncoder: PasswordEncoder,
    private val userRepository: UserRepository,
) {

    fun getUserFromAuthenticationPrincipal(springUser: User?): UserResponse {
        if (springUser == null) {
            throw AccessDeniedException("Not logged in")
        }
        val user = convertSpringUserToUser(springUser)
        return user.toUserResponse()
    }

    fun convertSpringUserToUser(springUser: User) =
        userRepository.findByUsername(springUser.username)
            ?: throw UserNotFoundException("User not found in database")


    fun registerUser(request: RegisterDto, role: String): UserResponse {
        userRepository.findByUsername(request.username)
            ?: throw UserAlreadyExistAuthenticationException("User already exists")

        val newUser = UserDto(
            request.username,
            passwordEncoder.encode(request.password)!!,
            request.email,
            role,
        )

        userRepository.insert(newUser)

        return UserResponse(
            request.username,
            request.email,
            role,
        )
    }
}
