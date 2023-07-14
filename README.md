<div align="center">

# Glossa
[![CI](https://img.shields.io/github/actions/workflow/status/aecsocket/glossa/build.yml)](https://github.com/aecsocket/glossa/actions/workflows/build.yml)
[![Release](https://img.shields.io/maven-central/v/io.github.aecsocket/glossa?label=release)](https://central.sonatype.com/artifact/io.github.aecsocket/glossa)
[![Snapshot](https://img.shields.io/nexus/s/io.github.aecsocket/glossa?label=snapshot&server=https%3A%2F%2Fs01.oss.sonatype.org)](https://central.sonatype.com/artifact/io.github.aecsocket/glossa)

Localization library for Adventure components

### [GitHub](https://github.com/aecsocket/glossa) · [Docs](https://aecsocket.github.io/glossa) · [Dokka](https://aecsocket.github.io/glossa/dokka)

</div>

Glossa provides a simple and opinionated API for developers to create localizable versions of their
software, and provides server admins and translators with tools to create translations based on
powerful and useful features like the MiniMessage format and Unicode ICU templates. It is designed
for Kotlin, with minimal support for Java.

## Usage

See the version badges for the latest release and snapshot builds.

Modules:
- `glossa` - core API
- `glossa-configurate` - tools for the [Configurate](https://github.com/spongepowered/configurate)
  library

```kotlin
repositories {
  mavenCentral()
  // for snapshot builds
  // maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
  implementation("io.github.aecsocket", "glossa", "VERSION")
}
```

### Setting up Glossa

#### Using the Kotlin DSL

```kotlin
val english = Locale.forLanguageTag("en-US")
val glossa = glossaStandard(
    defaultLocale = english,
    invalidMessageProvider = InvalidMessageProvider.Default,
) {
    substitutions {
        substitution("icon_item", Component.text("🪙"))
    }
    
    styles {
        style("color_variable", Style.style(NamedTextColor.YELLOW))
    }
    
    translation(english) {
        message("hello_world", "Hello world!")

        messageList("found_item",
            "<icon_item> You found <item_name>!",
            "<icon_item> You picked up <item_name>!",
            "<icon_item> You obtained <item_name>!",
        )
        
        section("command") {
            message("state", """
                System state:
                  - Foo: <color_variable><foo>
                  - Bar: <color_variable><bar>
                """.trimIndent())
            message("players_online", """
                There {players, plural,
                    =0 {are no players}
                    one {is <color_variable>#</color_variable> player}
                    other {are <color_variable>#</color_variable> players}
                } online.
                """.trimIndent())
        }
    }
}
```

#### Using Configurate

```yaml
substitutions:
  icon_item: "🪙"
  
styles:
  color_variable:
    color: yellow

translations:
  en-US:
    hello_world: "Hello world!"
    found_item:
      - "<icon_item> You found <item_name>!"
      - "<icon_item> You picked up <item_name>!"
      - "<icon_item> You obtained <item_name>!"
    command:
      # `|-`: preserve newlines, remove last newline
      state: |-
        System state:
          - Foo: <color_variable><foo>
          - Bar: <color_variable><bar>
      # `>-`: remove newlines, remove last newline
      players_online: >-
        There {players, plural,
          =0 {are no players}
          one {is <color_variable>#</color_variable> player}
          other {are <color_variable>#</color_variable> players}
        } online.
```

```kotlin
val glossa = glossaStandard(
    defaultLocale = english,
    invalidMessageProvider = InvalidMessageProvider.Default,
) {
    try {
        fromConfigLoader(YamlConfigurationLoader.builder()
            .file(myLangFile)
            .build()
        )
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}
```

### Generating messages

#### Using method generation calls

```kotlin
// typealias Message = List<Component>

val message: Message = glossa.message("hello_world")
// [ "Hello world!" ]

val foundItem: List<Message> = glossa.messageList("found_item") {
    replace("item_name", Component.text("Epic Sword", NamedTextColor.BLUE))
}
// [
//   "🪙 You found <blue>Epic Sword</blue>!",
//   "🪙 You picked up <blue>Epic Sword</blue>!",
//   "🪙 You obtained <blue>Epic Sword</blue>!"
// ]
```

#### Using a message proxy

```kotlin
interface MyMessages {
    // @MessageKey("hello_world") // optional, explicitly specify message key
    fun helloWorld(): Message
    
    fun foundItem(
        // @Placeholder("item_name") // optional, explicitly specify placeholder key
        itemName: Component,
    ): List<Message>
    
    // @SectionKey("command") // optional, explicitly specify subsection key
    val command: Command
    interface Command {
        fun state(
            foo: Component,
            bar: Component,
        ): Message
        
        fun playersOnline(
            players: Int
        ): Message
    }
}

val messages: MessageProxy<MyMessages> = glossa.messageProxy<MyMessages>()

val commandOutput: Message = messages.forLocale(english).command.state(
    foo = Component.text("OK"),
    bar = Component.text("ERROR"),
)
// [
//   "System state:",
//   "- Foo: <yellow>OK",
//   "- Bar: <yellow>ERROR"
// ]
```
