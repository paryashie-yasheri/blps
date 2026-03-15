package soaringLizards.blps.repository

import org.jooq.DSLContext
import org.jooq.impl.DSL.user
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Repository
import soaringLizards.blps.dto.user.RegisterDto
import soaringLizards.blps.dto.user.UserDto
import soaringLizards.blps.jooq.generated.tables.records.UsersRecord
import soaringLizards.blps.jooq.generated.tables.references.USERS
import java.util.UUID

@Repository
class UserRepository(
    private val dsl: DSLContext,
    private val passwordEncoder: PasswordEncoder,
) {

    fun findByUsername(username: String) =
        dsl.selectFrom(USERS)
            .where(USERS.USERNAME.eq(username))
            .fetchOne()
//            .firstOrNull()


    fun findUserById(id: UUID) =
        dsl.select(USERS)
            .where(USERS.ID.eq(id))
            .fetch()
            .firstOrNull()

    fun insert(user: RegisterDto): Int =
        dsl.insertInto(USERS)
            .set(USERS.ROLE, "USER")
            .set(USERS.USERNAME, user.username)
            .set(USERS.PASSWORD_HASH, passwordEncoder.encode(user.password))
            .set(USERS.EMAIL, user.email)
            .execute();

}