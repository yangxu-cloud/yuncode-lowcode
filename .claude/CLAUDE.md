# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Yuncode LowCode Platform — a multi-tenant SaaS low-code platform. Backend Spring Boot 3.1.8 / Java 17, frontend Vue 3 + TypeScript + Element Plus (vue-pure-admin thin version).

## Repository Structure

```
yuncode-lowcode/
├── yuncode-lowcode-boot/              # Backend parent (Maven multi-module)
│   ├── pom.xml                        # Parent POM, aggregator
│   ├── yuncode-common/                # Common: response wrapper, pagination, exceptions, JWT utils
│   ├── yuncode-auth/                  # Auth: Sa-Token + Redis + OAuth2 login strategy
│   ├── yuncode-system/                # System: users, roles, menus, org, settings, logs, apps
│   ├── yuncode-tenant/                # Tenant management
│   ├── yuncode-business/              # Reserved business module
│   ├── yuncode-admin/                 # Admin aggregator (main entry, port 8080, context-path /api)
│   │   └── app/                       # HotAppDeployer — plugin hot-reload system
│   ├── yuncode-gateway/               # Spring Cloud Gateway (port 9000, Nacos optional)
│   └── apps/install/                  # Plugin apps directory (hot-loaded at runtime)
│       └── com.yuncode.user.apps.*/   # Each app is a Maven submodule + hot-loaded JAR
├── yuncode-pure-admin/                # Frontend (Vue 3 + Vite + Element Plus + Pinia + i18n)
│   ├── src/
│   │   ├── api/                       # Axios API modules (auth, user, role, menu, org, settings, etc.)
│   │   ├── views/                     # Page components
│   │   ├── router/                    # Vue Router config (modules: home, login, error, etc.)
│   │   ├── store/                     # Pinia stores (app, permission, multiTags, epTheme)
│   │   ├── layout/                    # Layout components (sidebar, tags, search, settings panel)
│   │   ├── components/                # Shared components (ReDialog, ReIcon, ReAuth, etc.)
│   │   └── directives/                # Custom directives (auth, perms, copy, ripple, etc.)
│   └── vite.config.ts                 # Dev server proxied to localhost:8080
├── docs/                              # Architecture docs, DB design, integration guides
└── scripts/                           # deploy-app.sh / deploy-app.bat
```

## Build & Run

### Backend

```bash
# Full build (skip tests for speed)
cd yuncode-lowcode-boot
JAVA_HOME=/c/tools/jdk17 mvn clean compile -DskipTests -q

# Start admin service (dev mode)
JAVA_HOME=/c/tools/jdk17 mvn spring-boot:run -pl yuncode-admin -o

# Package for deployment
JAVA_HOME=/c/tools/jdk17 mvn package -pl yuncode-admin -am -DskipTests

# Build & deploy a single app JAR to apps/install/
./scripts/deploy-app.sh qms0205
```

- Maven local repo: `c:\tools\apache-maven-3.3.3\Repositories\Maven`
- Run commands from the `yuncode-lowcode-boot/` directory
- Use `-o` (offline) after initial compile to avoid network resolution
- IDEA workflow: build JARs via Artifacts, then copy to `apps/install/<appId>/lib/`

### Frontend

```bash
cd yuncode-pure-admin
pnpm install
pnpm dev        # Vite dev server on port 3000 (proxied to :8080)
pnpm build      # Production build
```

## Plugin System (HotAppDeployer)

Apps in `apps/install/` are loaded at runtime via JAR hot-reload — NOT compiled as Maven dependencies:

- **AppWatcher** interface — extensible change monitor base (JAR files, future DB/page/workflow)
- **JarFileWatcher** — `java.nio.file.WatchService` monitoring `lib/*.jar` per app directory
- **HotAppDeployer** — loads/unloads beans via `DefaultListableBeanFactory`, `ChildFirstURLClassLoader`, `RequestMappingHandlerMapping` reflection
- **RefreshOpenApiCache** — clears 3 layers of SpringDoc cache (cachedOpenAPI, mappingsMap, handlerMethods)

Each app is identified by its **directory name** (not JAR filename). All JARs under `{appId}/lib/` are loaded as one unit. The `@ComponentScan` excludes `com.yuncode.user.apps.*` — these are handled exclusively by HotAppDeployer.

## Key Technology Stack

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 3.1.8, Spring Cloud 2022.0.4 |
| ORM | MyBatis-Plus 3.5.7 + Druid 1.2.20 |
| Auth | Sa-Token 1.38.0 + Redis (jackson) + JWT |
| DB | MySQL 8.0+ (yuncode_lowcode) |
| Cache | Redis (Lettuce) |
| API Docs | Knife4j 4.5.0 (OpenAPI 3) |
| Frontend | Vue 3.4 + Vite 5.1 + Element Plus 2.6 + Pinia 2.1 + i18n |
| Gateway | Spring Cloud Gateway (port 9000, optional) |

## Key Dependencies (pom.xml)

- `sa-token-spring-boot3-starter`, `sa-token-redis-jackson`, `sa-token-jwt`
- `mybatis-plus-boot-starter`, `druid-spring-boot-3-starter`
- `knife4j-openapi3-jakarta-spring-boot-starter`
- `hutool-all`, `minio`
- `spring-cloud-starter-alibaba-nacos-discovery` (optional)

## Development Notes

- Port 8080, context-path `/api`, Gateway port 9000
- Nacos discovery disabled by default (can be enabled per environment)
- Sa-Token configured with JWT mode, `is-concurrent: true` (multi-session), `token-style: simple-uuid`
- MyBatis-Plus: `id-type: ASSIGN_ID`, `logic-delete-field: deleted`, SQL logging enabled in dev
- Skywalking agent available at `skywalking-agent/`
- DEV environment config files: `.env.development`, `.env.production` (frontend); `application-dev.yml` (backend)
- Logged-in user context stored via `StpInterfaceImpl` (permission/role lookup)
- Multi-account system supports platform admin, tenant admin, and normal users
