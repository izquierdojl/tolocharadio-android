<p align="center">
  <img src="docs/assets/sierra.svg" alt="Emblema de TolochaRadio" width="96" height="96">
</p>

<h1 align="center">Tolocha<span style="color:#d3a568">Radio</span> · Android</h1>

<p align="center"><em>Exploración radiofónica libre y autoalojada, en tu bolsillo.</em></p>

TolochaRadio Android es el cliente móvil de [TolochaRadio](https://github.com/izquierdojl/tolocharadio): radio en línea autoalojada sobre la base de datos pública de [RadioBrowser](https://www.radio-browser.info). Explora miles de emisoras de todo el mundo, guárdalas en favoritos, recupera lo último que has escuchado y sigue sonando en segundo plano o en tu Chromecast, con el mismo look-and-feel que la aplicación web.

Configura **una o varias instancias** propias (URL, alias y credenciales): la app inicia sesión de forma automática y transparente, sin pantallas de login. Todo se cifra en el dispositivo. **Radio libre, datos tuyos, control total.**

## Características

- **Catálogo global**: búsqueda de emisoras por nombre, país, idioma y género a través de RadioBrowser.
- **Reproductor persistente**: la música no se interrumpe al navegar; controles de reproducir/pausar, silenciar y copiar enlace desde la barra flotante.
- **Ficha de emisora**: carátula, país, idioma, género, códec, bitrate y estadísticas de cada emisora.
- **Favoritos**: guarda lo que te gusta y **reordena** la lista arrastrando; quitar una favorita se puede deshacer.
- **Historial**: consulta lo último que has escuchado y límpialo cuando quieras.
- **Mis emisoras**: añade a mano las emisoras que no están en el catálogo con su nombre y URL de stream.
- **Multi-servidor**: guarda varias instancias TolochaRadio, cambia de activa y edita sus credenciales cuando quieras.
- **Sesión automática**: credenciales por servidor cifradas (`EncryptedSharedPreferences`) con auto-login JWT y refresh rotatorio; nunca se expone el token en la URL.
- **Chromecast**: envía la reproducción a tu televisor o altavoz, con recuperación automática de la sesión.
- **Temporizador de apagado**: programa el cierre de la reproducción mientras te duermes.
- **Accesos directos**: emisoras recientes en el menú largo del icono de la app.
- **Reproducción en segundo plano**: notificación multimedia y control desde la pantalla de bloqueo (Media3 + MediaSessionService).
- **Tema Tolocha**: paleta pine/ochre/moss con Material Design 3; selector Sistema/Claro/Oscuro y pantalla de arranque configurable.
- **Vista lista o tarjetas**: cambia la densidad del catálogo a tu gusto.

## Capturas

| Inicio | Explorar | Búsqueda |
| --- | --- | --- |
| <img src="docs/assets/screenshots/inicio.png" width="240" alt="Pantalla de inicio"> | <img src="docs/assets/screenshots/explorar.png" width="240" alt="Explorar el catálogo"> | <img src="docs/assets/screenshots/buscar.png" width="240" alt="Búsqueda por género"> |
| **Ficha de emisora** | **Reproductor** | **Información de emisora** |
| <img src="docs/assets/screenshots/detalle-emisora.png" width="240" alt="Ficha de emisora"> | <img src="docs/assets/screenshots/reproductor.png" width="240" alt="Reproductor flotante"> | <img src="docs/assets/screenshots/reproductor-detalle.png" width="240" alt="Información de la emisora"> |
| **Favoritos** | **Historial** | **Mis emisoras** |
| <img src="docs/assets/screenshots/favoritos.png" width="240" alt="Favoritos"> | <img src="docs/assets/screenshots/historial.png" width="240" alt="Historial"> | <img src="docs/assets/screenshots/mis-emisoras.png" width="240" alt="Mis emisoras"> |
| **Servidores** | **Añadir servidor** | **Configuración** |
| <img src="docs/assets/screenshots/servidores.png" width="240" alt="Lista de servidores"> | <img src="docs/assets/screenshots/anadir-servidor.png" width="240" alt="Añadir servidor"> | <img src="docs/assets/screenshots/configuracion.png" width="240" alt="Configuración"> |

## Instalación

1. Descarga el APK firmado más reciente desde [Releases](https://github.com/izquierdojl/tolocharadio-android/releases).
2. Ábrelo en el móvil y acepta la instalación de fuentes desconocidas si el sistema lo pide.
3. Al arrancar, introduce la **URL**, un **alias**, el **email** y la **contraseña** de tu instancia TolochaRadio.

¿Prefieres compilarlo tú? Consulta [docs/instalacion.md](docs/instalacion.md).

## Documentación

| Página | Contenido |
| --- | --- |
| [Uso](docs/uso.md) | Características y guía de uso de la aplicación |
| [Instalación](docs/instalacion.md) | Requisitos, APK, compilación y configuración del servidor |
| [Arquitectura](docs/arquitectura.md) | Capas MVVM + Clean, stack, red, reproducción y almacenamiento |
| [Desarrollo](docs/desarrollo.md) | Comandos, tests, CI/CD y versionado |
| [Releases](docs/RELEASE.md) | APK automático y firma con keystore |
| [Licencia](docs/licencia.md) | Licencia MIT |

## Licencia

MIT. Consulta [docs/licencia.md](docs/licencia.md).
