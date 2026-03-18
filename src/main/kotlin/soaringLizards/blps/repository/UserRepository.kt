package soaringLizards.blps.repository

import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import soaringLizards.blps.dto.user.UserDto
import soaringLizards.blps.jooq.generated.tables.references.USERS
import java.util.UUID

@Repository
class UserRepository(
    private val dsl: DSLContext,
) {

    fun findByUsername(username: String) =
        dsl.selectFrom(USERS)
            .where(USERS.USERNAME.eq(username))
            .fetchOne()


    fun findById(id: UUID) =
        dsl.select(USERS)
            .where(USERS.ID.eq(id))
            .fetchOne()

    fun insert(user: UserDto): Int =
        dsl.insertInto(USERS)
            .set(USERS.ROLE, user.role)
            .set(USERS.USERNAME, user.username)
            .set(USERS.PASSWORD_HASH, user.password)
            .set(USERS.EMAIL, user.email.value)
            .execute();

}
