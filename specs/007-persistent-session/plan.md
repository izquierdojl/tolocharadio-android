# Implementation Plan: Gestión de Sesión Persistente, Servidores y Configuración

**Branch**: `007-persistent-session` | **Date**: 2026-09-06 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/007-persistent-session/spec.md`

## Summary

La app persiste la sesión (refresh token cifrado) y ofrece una
sección **Servidores** de primer nivel con instancias guardadas
que incluyen URL, alias y credenciales cifradas (refresh token +
password como respaldo para re-login). Existen conceptos separados
de **servidor activo** (en uso) y **servidor por defecto** (arranque).
La sección Perfil se sustituye por **Configuración** (tema
claro/oscuro, pantalla de arranque seleccionable
Favoritos/Historial/Explorar, cerrar sesión).

## Technical Context

**Language/Version**: Kotlin, minSdk=26, targetSdk=última estable

**Primary Dependencies**: Jetpack Compose + Material3, Retrofit + OkHttp + kotlinx.serialization, Room, DataStore, EncryptedSharedPreferences, Hilt, Coroutines + Flow

**Storage**: Room (caché offline de favoritos/historial/custom-stations), DataStore (ajustes, tema, baseUrl), EncryptedSharedPreferences (refresh tokens cifrados)

**Testing**: JUnit + kotlinx-coroutines-test + Turbine + MockK, Compose Test

**Target Platform**: Android (API 26+)

**Project Type**: mobile-app

**Performance Goals**: Restauración de sesión < 2s en condiciones normales de red, cambio de servidor < 5s

**Constraints**: HTTPS-only, Bearer tokens solo en memoria, refresh token cifrado en almacenamiento persistente, cero tokens en logs

**Scale/Scope**: Un usuario por dispositivo, pocos servidores guardados (< 20), tokens pequeños (< 4KB)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Estado | Justificación |
|-----------|--------|---------------|
| I. MVVM + Clean por capas | ✅ PASS | Persistencia en data, lógica de sesión en domain, gestión de servidores en UI/domain/data |
| II. Kotlin-First, Compose M3, Media3 | ✅ PASS | UI con Compose M3, persistencia con APIs nativas Android (EncryptedSharedPreferences, DataStore) |
| III. Test-First | ✅ PASS | Tests unitarios para repositorios, use cases y viewModels; tests de integración para persistencia |
| IV. Streaming Robusto | ✅ PASS | No afecta streaming; sesión persistente mejora experiencia al restaurar sin interrupciones |
| V. Simplicidad Modular | ✅ PASS | EncryptedSharedPreferences es API nativa sin dependencias externas; una sola feature en módulo app |

**Violaciones**: Ninguna.

## Project Structure

### Documentation (this feature)

```text
specs/007-persistent-session/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── contracts/           # Phase 1 output
```

### Source Code (repository root)

```text
app/src/main/java/com/izquierdojl/tolocharadio/
├── auth/
│   ├── data/
│   │   ├── repository/
│   │   │   └── AuthRepository.kt          # Persistencia de sesión, login/logout/refresh
│   │   ├── local/
│   │   │   ├── EncryptedTokenStorage.kt   # Almacenamiento cifrado de refresh token
│   │   │   └── SessionManager.kt          # Estado de sesión en memoria
│   │   └── remote/
│   │       └── AuthRemoteDataSource.kt    # API calls de auth (ya existe)
│   ├── domain/
│   │   ├── usecase/
│   │   │   ├── RestoreSessionUseCase.kt   # Restaurar sesión al abrir app
│   │   │   ├── LoginUseCase.kt            # Login + persistir credenciales
│   │   │   └── LogoutUseCase.kt           # Logout + limpiar credenciales
│   │   └── model/
│   │       └── AuthSession.kt             # Estado de sesión sellado
│   └── ui/
│       ├── LoginScreen.kt                 # Ya existe
│       └── LoginViewModel.kt              # Ya existe, ampliar con persistencia
├── servers/
│   ├── data/
│   │   ├── repository/
│   │   │   └── ServerRepository.kt        # CRUD de servidores guardados
│   │   ├── local/
│   │   │   ├── ServerDao.kt               # Room DAO para SavedServer
│   │   │   └── ServerDatabase.kt          # Tabla de servidores (o migración)
│   │   └── model/
│   │       └── SavedServerEntity.kt       # Entidad Room
│   ├── domain/
│   │   ├── usecase/
│   │   │   ├── GetServersUseCase.kt       # Listar servidores guardados
│   │   │   ├── AddServerUseCase.kt        # Añadir servidor con validación
│   │   │   ├── SwitchServerUseCase.kt     # Cambiar servidor + limpiar caché
│   │   │   └── DeleteServerUseCase.kt     # Eliminar servidor + credenciales
│   │   └── model/
│   │       └── SavedServer.kt             # Modelo de dominio
│   └── ui/
│       ├── ServerListScreen.kt            # Lista de servidores guardados
│       ├── ServerListViewModel.kt         # ViewModel de gestión
│       └── AddServerDialog.kt             # Diálogo para añadir servidor
└── common/
    └── data/
        └── CacheManager.kt                # Limpieza de caché al cambiar servidor

app/src/test/java/com/izquierdojl/tolocharadio/
├── auth/
│   ├── data/repository/AuthRepositoryTest.kt
│   ├── data/local/EncryptedTokenStorageTest.kt
│   └── domain/usecase/RestoreSessionUseCaseTest.kt
└── servers/
    ├── data/repository/ServerRepositoryTest.kt
    └── domain/usecase/SwitchServerUseCaseTest.kt
```

**Structure Decision**: Estructura Android existente por feature (auth/, servers/) con separación MVVM + Clean (data/domain/ui). Se amplía el módulo app existente sin crear módulos Gradle adicionales (YAGNI).

## Complexity Tracking

> No se requieren justificaciones de violaciones constitucionales.
