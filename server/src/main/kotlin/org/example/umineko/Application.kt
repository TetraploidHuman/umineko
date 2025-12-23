package org.example.umineko

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.example.umineko.auth.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.request.receive

suspend fun main() {
    initDB()
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(KtorCachePlugin) //自定义的缓存插件
    installSerialization() //JSON的序列化插件
    installCors()
    installSecurity()

    routing {
        testRoutes()
        authRoutes()
        logoutRoute()
        protectedRoutes()
    }
}

fun Application.installSecurity() {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = JwtConfig.realm

            verifier(
                JWT.require(Algorithm.HMAC256(JwtConfig.secret))
                    .withIssuer(JwtConfig.issuer)
                    .build()
            )

            validate { credential ->
                val uid = credential.payload.getClaim("uid").asLong()
                if (uid != null) JWTPrincipal(credential.payload) else null
            }

            authHeader { call ->
                call.request.cookies[JwtConfig.accessCookie]
                    ?.let { HttpAuthHeader.Single("Bearer", it) }
            }
        }
    }
}

fun Application.installCors() {
    install(CORS) {
        allowHost("localhost:8081", schemes = listOf("http"))
        allowCredentials = true
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
    }
}

fun Application.installSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }
}

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
        }
    }
    authorize("/profile",Role.USER, Role.ADMIN) {
        get {
            call.respond("普通用户接口")
        }
    }
}
val AuthorizePlugin = createRouteScopedPlugin(
    name = "AuthorizePlugin",
    createConfiguration = ::AuthorizeConfig
) {

    val allowedRoles = pluginConfig.allowedRoles

    on(AuthenticationChecked) { call ->
        val principal = call.principal<JWTPrincipal>() ?: return@on

        val role = runCatching {
            Role.valueOf(principal.payload.getClaim("role").asString())
        }.getOrElse {
            call.respond(HttpStatusCode.Forbidden)
            return@on
        }

        if (role !in allowedRoles) {
            call.respond(HttpStatusCode.Forbidden)
            return@on
        }
    }
}

fun Route.authorize(
    path: String,
    vararg roles: Role,
    build: Route.() -> Unit
): Route = authenticate("auth-jwt") {
        route(path) {
            install(AuthorizePlugin) {
                allowedRoles = roles.toSet()
            }
            build()
        }
    }

class AuthorizeConfig {
    var allowedRoles: Set<Role> = emptySet()
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
                    maxAge = 0
                )
            )

            call.response.cookies.append(
                Cookie(
                    name = JwtConfig.refreshCookie,
                    value = "",
                    path = "/",
                    maxAge = 0
                )
            )

            call.respond("Logout OK")
        }
    }
}
