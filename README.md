# Second Wind!

Second Wind! is a NeoForge Minecraft mod that gives players a second-chance downed state with revive, last-stand, and recovery mechanics.

## Supported Versions

- Minecraft: `1.21.1`
- Java: `21`
- NeoForge: active and buildable
- Fabric: scaffolded module only; gameplay is not implemented on Fabric yet

## Building

Use the Gradle wrapper from the repository root:

```sh
./gradlew build
```

On Windows:

```bat
gradlew.bat build
```

Build outputs stay inside this repository under Gradle-managed `build/` directories.

## ToucanLib

Second Wind resolves ToucanLib from the published CurseMaven artifact by default:

```gradle
curse.maven:toucanlib-1542666:8089151
```

CI and release builds must use the published artifact. Do not rely on local ToucanLib jars, local included builds, `flatDir`, or `mavenLocal()` for normal builds.

For advanced local ToucanLib development, `-PuseLocalToucanLib=true` enables `mavenLocal()` only. This property defaults to `false` and should not be enabled in CI.

## Development Notes

- Package: `com.jvn.secondwind`
- Mod ID: `secondwind`
- Common resources are in `common/`.
- Active NeoForge gameplay code is in `neoforge/`.
- Fabric exists only as a clean skeleton until a future gameplay port.

## TODO

- Port gameplay to Fabric when Fabric support is ready.
- Keep ToucanLib on published Maven coordinates for CI and releases.
