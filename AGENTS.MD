**Language**: Kotlin <br>
**Project construction setup**: KMP, Compose multiplatform  <br>
**Supported targets**: **Now**: Desktop (macOS, Windows) <br>
**Code guidelines**: SOLID, Clean Architecture

### Fundamental Libraries
- KMP
- Compose multiplatform
- Ktor
- Koin

### Project structure
- Enforce dependency rule: `presentation -> domain <- data`.
- Keep domain provider-agnostic (no API keys, base URLs, HTTP details).
- Keep related logical parts together (low coupling, high cohesion), other - extract in separate modules or folders or files or e.t.c. according to the corresponding need.

### Non Goals
- Try keep files not large (~1000 lines)