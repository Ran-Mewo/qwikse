package io.github.ran.qwikse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Qwiksetils {
    public static final Logger LOGGER = LogManager.getLogger("qwikse");

    public static void main(String[] args) throws Throwable {
        Class<?> clazz = Class.forName(args[0]);
        String[] newArgs = new String[args.length - 1];
        System.arraycopy(args, 1, newArgs, 0, newArgs.length);
        clazz.getMethod("main", String[].class).invoke(null, (Object) newArgs);
    }

    public static URL getDownloadURL(String projectID, String gameVersion, String loader) throws IOException {
        // Imagine parsing this JSON data when you have a brother who knows Regex

        URL url = new URL("https://api.modrinth.com/v2/project/" + projectID + "/version?game_versions=%5B%22" + gameVersion + "%22%5D&loaders=%5B%22" + loader + "%22%5D");
        StringBuilder sb = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        String text = sb.toString();

        if (text.equals("[]") && loader.equalsIgnoreCase("quilt")) {
            url = new URL("https://api.modrinth.com/v2/project/" + projectID + "/version?game_versions=%5B%22" + gameVersion + "%22%5D&loaders=%5B%22" + "fabric" + "%22%5D");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            text = sb.toString();
        }

        if (text.equals("[]")) {
            return null;
        }

        Matcher matcher = Pattern.compile("(?i)\"url\":\"([(http(s)?):\\/\\/(www\\.)?a-zA-Z\\d@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b[-a-zA-Z\\d@:%_\\+.~#?&//=]*)\"").matcher(text);

        if (matcher.find()) {
            return new URL(matcher.group(1));
        } else {
            return null;
        }
    }

    public static <T extends Throwable> RuntimeException skipExceptionHandling(Throwable t) throws T {
        throw (T) t;
    }
}
