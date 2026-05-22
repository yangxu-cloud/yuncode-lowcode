# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Yuncode LowCode Platform — a multi-tenant SaaS low-code platform. Backend Spring Boot 3.1.8 / Java 17, frontend Vue 3 + TypeScript + Element Plus (vue-pure-admin thin version).

## Repository Structure

```
yuncode-lowcode/
├── docs/                           # 📍 Documentation
│   ├── requirements/               # Requirements (from skills/)
│   ├── design/                     # Architecture & design docs
│   ├── spec/                       # Implementation specs & summaries
│   ├── guide/                      # Setup, integration & troubleshooting guides
│   └── sql/                        # Database scripts
├── yuncode-lowcode-boot/           # Backend parent (Maven multi-module)
│   ├── pom.xml                     # Parent POM, aggregator
│   ├── yuncode-common/             # Common: response wrapper, pagination, exceptions, JWT utils
│   ├── yuncode-auth/               # Auth: Sa-Token + Redis + OAuth2 login strategy
│   ├── yuncode-system/             # System: users, roles, menus, org, settings, logs, apps
│   ├── yuncode-tenant/             # Tenant management
│   ├── yuncode-business/           # Reserved business module
│   ├── yuncode-admin/              # Admin aggregator (main entry, port 8080, context-path /api)
│   │   └── app/                    # HotAppDeployer — plugin hot-reload system
│   ├── yuncode-gateway/            # Spring Cloud Gateway (port 9000, Nacos optional)
│   └── apps/install/               # Plugin apps directory (hot-loaded at runtime)
│       └── com.yuncode.user.apps.*/# Each app is a Maven submodule + hot-loaded JAR
├── yuncode-pure-admin/            # Frontend (Vue 3 + Vite + Element Plus + Pinia + i18n)
│   ├── src/
│   │   ├── api/                    # Axios API modules (auth, user, role, menu, org, settings)
│   │   ├── views/                  # Page components
│   │   ├── router/                 # Vue Router (modules: home, login, app-dev, operations, facilities)
│   │   ├── store/                  # Pinia stores (app, permission, multiTags, epTheme)
│   │   ├── layout/                 # Layout (sidebar, tags, search, settings panel, notices)
│   │   └── components/             # Shared components (ReAuth, ReIcon, ReDialog, RePerms...)
│   └── vite.config.ts              # Dev server proxied to localhost:8080
└── scripts/                        # deploy-app.sh / deploy-app.bat
```

## Documentation Index

| Category | Path | Contents |
|----------|------|----------|
| **Requirements** | `docs/requirements/` | Original feature requirements (login, nav, roles, org, app-dev, etc.) |
| **Design** | `docs/design/` | Architecture, app plugin, auth, database, gateway, multi-tenancy, logging, event system, i18n, user cache, exception handling |
| **Spec** | `docs/spec/` | Implementation summaries: application/system/org/menu management, gateway, frontend, kickout, interfaces |
| **Guide** | `docs/guide/` | Quickstart, build, troubleshooting, skywalking, nacos, maven, sse, cache, sql guide |
| **SQL** | `docs/sql/` | Database init, migration, and seed scripts |

## Build & Run

### Backend

```bash
cd yuncode-lowcode-boot
JAVA_HOME=/c/tools/jdk17 mvn clean compile -DskipTests -q                                    # compile
JAVA_HOME=/c/tools/jdk17 mvn spring-boot:run -pl yuncode-admin -o                            # dev run
JAVA_HOME=/c/tools/jdk17 mvn package -pl yuncode-admin -am -DskipTests                       # package
./scripts/deploy-app.sh qms0205                                                              # deploy single app
```

- Maven repo: `c:\tools\apache-maven-3.3.3\Repositories\Maven`
- Use `-o` (offline) after initial compile
- IDEA: build app JARs via Artifacts → copy to `apps/install/<appId>/lib/`

### Frontend

```bash
cd yuncode-pure-admin
pnpm install
pnpm dev      # Vite dev server on port 3000
pnpm build    # Production build
```

## Plugin System (HotAppDeployer)

Apps in `apps/install/` are hot-loaded at runtime via JAR hot-reload (NOT Maven dependency):

- **AppWatcher** interface — extensible change monitor base
- **JarFileWatcher** — `java.nio.file.WatchService` monitoring `lib/*.jar` per app directory
- **HotAppDeployer** — loads/unloads beans via `DefaultListableBeanFactory`, `ChildFirstURLClassLoader`, `RequestMappingHandlerMapping` reflection
- **RefreshOpenApiCache** — clears 3 SpringDoc cache layers

Each app is identified by its **directory name** (not JAR filename). All JARs under `{appId}/lib/` are loaded as one unit. See `docs/design/app-hot-reload.md` for details.

## Key Technology Stack

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 3.1.8, Spring Cloud 2022.0.4 |
| ORM | MyBatis-Plus 3.5.7 + Druid 1.2.20 |
| Auth | Sa-Token 1.38.0 + Redis (jackson) + JWT |
| DB | MySQL 8.0+ (yuncode_lowcode database) |
| Cache | Redis (Lettuce) |
| API Docs | Knife4j 4.5.0 (OpenAPI 3) |
| Frontend | Vue 3.4 + Vite 5.1 + Element Plus 2.6 + Pinia 2.1 + vue-i18n |
| Gateway | Spring Cloud Gateway (port 9000, optional) |

## Key Dependencies

- `sa-token-spring-boot3-starter`, `sa-token-redis-jackson`, `sa-token-jwt`
- `mybatis-plus-boot-starter`, `druid-spring-boot-3-starter`
- `knife4j-openapi3-jakarta-spring-boot-starter`
- `hutool-all`, `minio`
- `spring-cloud-starter-alibaba-nacos-discovery` (optional)

## Development Notes

- Port 8080, context-path `/api`, Gateway port 9000
- Nacos discovery disabled by default
- Sa-Token: JWT mode, `is-concurrent: true` (multi-session), `token-style: simple-uuid`
- MyBatis-Plus: `id-type: ASSIGN_ID`, `logic-delete-field: deleted`, SQL logging in dev
- Admin module excludes `com.yuncode.user.apps.*` from `@ComponentScan` (handled by HotAppDeployer)
- Three login types (admin/tenant/user) with isolated StpLogic instances
- Skywalking agent at `skywalking-agent/`, config at `docs/guide/skywalking.md`
