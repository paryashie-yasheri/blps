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

    fun getUserByUsername(username: String): UserResponse? {
        val user = userRepository.findByUsername(username)
        return user?.toUserResponse()
    }

    fun convertSpringUserToUser(springUser: User) =
        userRepository.findByUsername(springUser.username)
            ?: throw UserNotFoundException("User not found in database")

    fun authenticateStaff(username: String, password: String): UserResponse? {
        val user = userRepository.findByUsername(username)
            ?: return null

        if (user.role != "STAFF" && user.role != "ADMIN")
            return null

        if (passwordEncoder.matches(password, user.passwordHash)) {
            return user.toUserResponse()
        } else {
            return null
        }
    }

    fun authenticateOrCreateOpenedxUser(username: String): UserResponse? {
        val existingUser = userRepository.findByUsername(username)
        return if (existingUser != null) {
            // Allow any existing user to login as OPENEDX_USER
            existingUser.toUserResponse()
        } else {
            // Auto-create new OPENEDX_USER
            val newUser = UserDto(
                username = username,
                password = passwordEncoder.encode("openedx-auto-${username}")!!,
                email = soaringLizards.blps.dto.user.Email("${username}@openedx.local"),
                role = "OPENEDX_USER",
            )
            val inserted = userRepository.insert(newUser)
            inserted?.toUserResponse()
        }
    }

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
