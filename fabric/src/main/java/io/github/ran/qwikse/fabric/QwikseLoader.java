package io.github.ran.qwikse.fabric;

import com.github.axet.wget.WGet;
import io.github.ran.qwikse.Qwiksetils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.LanguageAdapterException;
import net.fabricmc.loader.api.ModContainer;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static io.github.ran.qwikse.Qwiksetils.*;

public class QwikseLoader implements LanguageAdapter {
    static {
        try {
            load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void load() throws IOException {
        LOGGER.info("Loading...");

        if (System.getProperty("qwikse") == null) {
            if (loadMods()) {
                try {
                    LOGGER.info("Doing magic!");
                    // Relaunch the game (Code from VersionModLoader which is by sschr15 & is licensed under the MIT license)

                    String vmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments()
                            .stream().filter(s -> !s.contains("-agentlib") && !s.contains("-javaagent"))
                            .collect(Collectors.joining(" "));
                    String command = System.getProperty("sun.java.command").split(" ")[0];
                    String java = System.getProperty("java.home") + "/bin/java";

                    if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
                        java = java.replace('/', '\\') + ".exe";
                    }

                    String cp = System.getProperty("java.class.path");
                    if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
                        // we need to add our jarfile to the classpath
                        ModContainer container = FabricLoader.getInstance().getModContainer("qwikse")
                                .orElseThrow(() -> new RuntimeException("Could not find jar file for qwikse"));
                        Path jar;
                        try {
                            jar = container.getRootPath();
                        } catch (RuntimeException e) {
                            // very old fabric crashes when we try to access root path too early
                            Class<?> modContainer = Class.forName("net.fabricmc.loader.ModContainer");
                            Method setupRootPath = modContainer.getDeclaredMethod("setupRootPath");
                            Field root = modContainer.getDeclaredField("root");
                            root.setAccessible(true);
                            setupRootPath.setAccessible(true);

                            setupRootPath.invoke(container);
                            jar = container.getRootPath();
                            root.set(container, null); // avoid floader crashing trying to setup root twice
                        }

                        FileSystem fs = jar.getFileSystem();
                        cp += File.pathSeparator + fs.toString().replace('\\', '/');
                    }

                    if (command.equals("org.multimc.EntryPoint")) {
                        // replace mmc's entrypoint with fabric's
                        try {
                            Class.forName("net.fabricmc.loader.launch.knot.KnotClient");
                            command = "net.fabricmc.loader.launch.knot.KnotClient";
                        } catch (ClassNotFoundException e) {
                            command = "net.fabricmc.loader.impl.launch.knot.KnotClient";
                        }
                    }
                    String[] launchArguments = FabricLoader.getInstance().getLaunchArguments(false);

                    // fix for funky mclauncher stuff
                    if (vmArgs.contains("-Dos.name=")) {
                        String beforeOsName = "";
                        if (vmArgs.contains("-Dos.name=")) {
                            beforeOsName = vmArgs.substring(0, vmArgs.indexOf("-Dos.name="));
                        }

                        String afterXss = "";
                        if (vmArgs.contains("-Xss")) {
                            afterXss = vmArgs.substring(vmArgs.indexOf("-Xss"));
                        }

                        if (afterXss.contains(" ")) {
                            afterXss = afterXss.substring(afterXss.indexOf(' ') + 1); // skip the value and the space
                        }

                        vmArgs = beforeOsName + afterXss;

                        // fix weird space thing which convinces java it's done parsing vm args
                        if (vmArgs.contains("-DFabricMcEmu= ")) {
                            vmArgs = vmArgs.replace("-DFabricMcEmu= ", "-DFabricMcEmu=");
                        } else if (vmArgs.contains("-DQuiltMcEmu= ")) {
                            vmArgs = vmArgs.replace("-DQuiltMcEmu= ", "-DQuiltMcEmu=");
                        }
                    }

                    // fix for extra spaces
                    vmArgs = vmArgs.replaceAll("\\s+", " ");

                    List<String> entireCommand = new ArrayList<>();
                    entireCommand.add(java);
                    entireCommand.addAll(Arrays.asList(vmArgs.split(" ")));
                    if (System.getProperty("java.library.path") != null && !vmArgs.contains("-Djava.library.path=")) {
                        entireCommand.add("-Djava.library.path=" + System.getProperty("java.library.path"));
                    }
                    entireCommand.add("-Dqwikse"); // if it exists, we're fine
                    entireCommand.add("-cp");
                    entireCommand.add(cp);
                    entireCommand.add(Qwiksetils.class.getName()); // init with our own Main
                    entireCommand.add(command); // then pass the original command on
                    entireCommand.addAll(Arrays.asList(launchArguments)); // along with the original args

                    System.gc();

                    Process process = new ProcessBuilder(entireCommand)
                            .inheritIO()
                            .start();

                    try {
                        while (process.isAlive()) {
                            //noinspection BusyWait
                            Thread.sleep(100);
                        }
                        System.exit(process.exitValue());
                    } catch (InterruptedException e) {
                        process.destroy();
                        System.exit(1);
                    }
                } catch (Throwable t) {
                    skipExceptionHandling(t);
                }
            } else {
                LOGGER.info("Nice, you have all the APIs downloaded!");
            }
        }
    }

    private static boolean loadMods() throws IOException {
        String mcVersion = FabricLoader.getInstance().getModContainer("minecraft").get().getMetadata().getVersion().getFriendlyString();
        File modsDir = FabricLoader.getInstance().getGameDir().resolve("mods").toFile();
        List<Boolean> newModLoaded = new ArrayList<>();

        if (FabricLoader.getInstance().getModContainer("fabric-api").isEmpty()) {
            LOGGER.info("Downloading Fabric API for " + mcVersion + "...");
            URL url = getDownloadURL("P7dR8mSH", mcVersion, "fabric");
            if (url != null) {
                WGet wget = new WGet(url, modsDir);
                wget.download();
            } else {
                LOGGER.error("Could not find Fabric API for " + mcVersion);
            }
            newModLoaded.add(true);
        }

        if (FabricLoader.getInstance().getModContainer("cloth-config").isEmpty()) {
            LOGGER.info("Downloading Cloth Config for " + mcVersion + "...");
            URL url = getDownloadURL("9s6osm5g", mcVersion, "fabric");
            if (url != null) {
                WGet wget = new WGet(url, modsDir);
                wget.download();
            } else {
                LOGGER.error("Could not find Cloth Config for " + mcVersion);
            }
            newModLoaded.add(true);
        }

        if (FabricLoader.getInstance().getModContainer("architectury").isEmpty()) {
            LOGGER.info("Downloading Architectury API for " + mcVersion + "...");
            URL url = getDownloadURL("lhGA9TYQ", mcVersion, "fabric");
            if (url != null) {
                WGet wget = new WGet(url, modsDir);
                wget.download();
            } else {
                LOGGER.error("Could not find Architectury API for " + mcVersion);
            }
            newModLoaded.add(true);
        }

        if (FabricLoader.getInstance().getModContainer("fabric-language-kotlin").isEmpty()) {
            LOGGER.info("Downloading Fabric Language Kotlin for " + mcVersion + "...");
            URL url = getDownloadURL("Ha28R6CL", mcVersion, "fabric");
            if (url != null) {
                WGet wget = new WGet(url, modsDir);
                wget.download();
            } else {
                LOGGER.error("Could not find Fabric Language Kotlin for " + mcVersion);
            }
            newModLoaded.add(true);
        }





        if (newModLoaded.isEmpty()) {
            newModLoaded = null;
            return false;
        } else {
            newModLoaded = null;
            return true;
        }
    }

    @Override
    public <T> T create(ModContainer mod, String value, Class<T> type) throws LanguageAdapterException {
        throw new LanguageAdapterException("This language adapter does not support creating objects.");
    }
}
