package org.example.umineko

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.example.umineko.DB.Role
import org.example.umineko.DB.demoTableDao
import org.example.umineko.DB.findById
import org.example.umineko.DB.findByUsername
import org.example.umineko.DB.increaseTokenVersion
import org.example.umineko.DB.verifyPassword
import org.example.umineko.auth.JwtConfig
import org.example.umineko.auth.JwtUtil

fun Route.testRoutes(){
    get("/") {
        call.respondText("Hello World!")
    }

    get("/message") {
        call.respondText(listOf("哈基米", "曼波", "欧耶").random())
    }

    get("/umineko/{id}") {
        val id = call.parameters["id"]?.toLongOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest)
        call.respond(demoTableDao.findMessage(id))
    }
}

fun Route.authRoutes() {

    post("/login") {
        val req = call.receive<LoginReq>()

        val user = findByUsername(req.username)
            ?: return@post call.respond(HttpStatusCode.Unauthorized) //if查询不到用户就返回401

        if (!verifyPassword(user, req.password)) {
            return@post call.respond(HttpStatusCode.Unauthorized)
        } //if用户密码错误就返回401

        val access = JwtUtil.access( //生成短期 access token
            user.id,
            user.role,
            user.tokenVersion
        )

        val refresh = JwtUtil.refresh( //生成短期 refresh token
            user.id,
            user.tokenVersion
        )

        call.response.cookies.append( //回传Cookie给前端
            Cookie(
                name = JwtConfig.accessCookie,
                value = access,
                httpOnly = true,
                path = "/",
                secure = false // 本地开发
            )
        )

        call.response.cookies.append(
            Cookie(
                name = JwtConfig.refreshCookie,
                value = refresh,
                httpOnly = true,
                path = "/",
                secure = false
            )
        )
        call.respond("Login OK")
    }

    post("/auth/refresh") { //刷新 access token
        val token = call.request.cookies[JwtConfig.refreshCookie] //从 Cookie 中取 refresh toke
            ?: return@post call.respond(HttpStatusCode.Unauthorized)
        val verifier = JWT
            .require(Algorithm.HMAC256(JwtConfig.secret))
            .withIssuer(JwtConfig.issuer)
            .withAudience(JwtConfig.audience)
            .build()

        val decoded = verifier.verify(token) //校验 refresh token

        val uid = decoded.getClaim("uid").asLong() //取用户 ID and tokenVersion
        val ver = decoded.getClaim("ver").asInt()

        val user = findById(uid) ?: return@post call.respond(HttpStatusCode.Unauthorized)

        if (user.tokenVersion != ver) {
            return@post call.respond(HttpStatusCode.Unauthorized)
        }

        val newAccess = JwtUtil.access(uid, user.role, ver)
        call.response.cookies.append(Cookie(JwtConfig.accessCookie, newAccess, httpOnly = true))
        call.respond("Refreshed")
    }
}

fun Route.protectedRoutes() {

    authorize("/admin", Role.ADMIN) {
        get {
            call.respondText("管理员接口")
            print("激活了管理员URL")
        }
    }
    authorize("/profile",Role.USER, Role.ADMIN) {
        get {
            call.respond("普通用户接口")
            print("激活了普通用户URL")
        }
    }
}

fun Route.authorize(
    path: String,
    vararg roles: Role,
    build: Route.() -> Unit
): Route = authenticate("auth-jwt") {

    route(path) {

        install(
            createRouteScopedPlugin(
                name = "Authorize(${roles.joinToString()})"
            ) {
                on(AuthenticationChecked) { call ->
                    val principal = call.principal<JWTPrincipal>() ?: return@on

                    val role = runCatching {
                        Role.valueOf(principal.payload.getClaim("role").asString())
                    }.getOrElse {
                        call.respond(HttpStatusCode.Forbidden)
                        return@on
                    }
                    if (role !in roles) { call.respond(HttpStatusCode.Forbidden) }
                }
            }
        )
        build()
    }
}

fun Route.logoutRoute() {
    authenticate("auth-jwt") {
        post("/logout") {

            val principal = call.principal<JWTPrincipal>()!!
            val uid = principal.payload.getClaim("uid").asLong()

            // tokenVersion + 1 使得所有旧 token 全部失效
            increaseTokenVersion(uid)

            // 清空给浏览器的 Cookie
            call.response.cookies.append(
                Cookie(
                    name = JwtConfig.accessCookie,
                    value = "",
                    path = "/",
                    maxAge = 0,
                    secure = false
                )
            )

            call.response.cookies.append(
                Cookie(
                    name = JwtConfig.refreshCookie,
                    value = "",
                    path = "/",
                    maxAge = 0,
                    secure = false
                )
            )

            call.respond("Logout OK")
        }
    }
}
