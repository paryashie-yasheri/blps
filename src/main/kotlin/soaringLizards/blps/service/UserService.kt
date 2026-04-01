package soaringLizards.blps.service

import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import soaringLizards.blps.dto.user.RegisterDto
import soaringLizards.blps.dto.user.UserDto
import soaringLizards.blps.dto.user.UserResponse
import soaringLizards.blps.dto.user.toUserResponse
import soaringLizards.blps.dto.userBalances.UserBalanceDto
import soaringLizards.blps.exception.UserAlreadyExistAuthenticationException
import soaringLizards.blps.exception.UserNotFoundException
import soaringLizards.blps.repository.UserBalanceRepository
import soaringLizards.blps.repository.UserRepository

@Service
class UserService(
    private val passwordEncoder: PasswordEncoder,
    private val userRepository: UserRepository,
    private val userBalanceRepository: UserBalanceRepository,
) {

    fun getUserFromAuthenticationPrincipal(springUser: User?): UserResponse {
        springUser ?: throw AccessDeniedException("Not logged in")

        val user = convertSpringUserToUser(springUser)
        return user.toUserResponse()
    }

    fun convertSpringUserToUser(springUser: User) =
        userRepository.findByUsername(springUser.username)
            ?: throw UserNotFoundException("User not found in database")


    @Transactional
    fun registerUser(request: RegisterDto, role: String): UserResponse {
        if (userRepository.existWithUsername(request.username))
            throw UserAlreadyExistAuthenticationException("User already exists")

        val newUserDto = UserDto(
            request.username,
            passwordEncoder.encode(request.password)!!,
            request.email,
            role,
        )

        val newUser = userRepository.insert(newUserDto)
            ?: throw UserAlreadyExistAuthenticationException("User already exists")

        userBalanceRepository.insert(UserBalanceDto.createDefault(newUser.id!!))

        return UserResponse(
            request.username,
            request.email,
            role,
        )
    }
}
