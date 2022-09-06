# Qwikse
Universal API Loader\
A library mod for developers who doesn't want to mess with all the APIs\
The mod downloads all the APIs based on the Minecraft version

Currently downloads:
- [Quilt Standard Libraries](https://modrinth.com/mod/qsl) or [Fabric API](https://modrinth.com/mod/fabric-api)
- [Quilt Kotlin Libraries](https://modrinth.com/mod/qkl) or [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin)
- [Architectury API](https://modrinth.com/mod/architectury-api)
- [Cloth Config](https://modrinth.com/mod/cloth-config)
- [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port)
- [Porting Lib](https://github.com/Fabricators-of-Create/Porting-Lib)

It will only download if the mod doesn't exist

This mod doesn't have to be downloaded by the end user usually but if you're a modpack author then you probably could include this in your modpack and remove the APIs the mod automatically adds

#### Why would I ever need this?
Let's say for example you have a mod which works on Minecraft versions 1.16.5-1.19.2 but you include the Fabric API which doesn't work on those versions so it breaks for the end user if they doesn't have the API downloaded corresponding to their Minecraft version, this mod basically fixes that issue and a bunch of others

#### How could I use this as a mod author?
Well you should never **include** any of the APIs that it adds and you should only **include** this mod
```gradle
repositories {
    // ...
    maven {
        name = "Modrinth"
        url = "https://api.modrinth.com/maven"
        content {
            includeGroup "maven.modrinth"
        }
    }
    // ...
}

dependencies {
    // ...
    include "maven.modrinth:qwikse:1.1.0"
    // ...
}
```

#
Thanks to [sschr15](https://modrinth.com/user/sschr15) for writing and open-sourcing the magic code it uses to make the end user not having relaunch their game when the mod finishes downloading everything
