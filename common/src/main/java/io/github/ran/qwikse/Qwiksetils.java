package io.github.ran.qwikse;

import com.github.zafarkhaja.semver.Version;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    public static URL getPortingLibCreateURL(String gameVersion) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        String mavenMetadata;
        boolean above1_18_2;

        if (Version.valueOf(gameVersion).satisfies(">=1.19.2") || Version.valueOf(gameVersion).satisfies("1.18.2")) {
            mavenMetadata = "https://mvn.devos.one/snapshots/io/github/fabricators_of_create/Porting-Lib/porting-lib/maven-metadata.xml";
            above1_18_2 = true;
        } else {
            mavenMetadata = "https://mvn.devos.one/snapshots/io/github/fabricators_of_create/Porting-Lib/maven-metadata.xml";
            above1_18_2 = false;
        }

        int dotCount = gameVersion.length() - gameVersion.replace(".", "").length();

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        Document document = builder.parse(mavenMetadata);

        XPath xPath = XPathFactory.newInstance().newXPath();
        String expression = "/metadata/versioning/versions";
        String versions = xPath.compile(expression).evaluate(document);

        String[] versionArray = versions.split("\n");
        List<String> versionList = Arrays.stream(versionArray).collect(Collectors.toList());
        versionList.remove(0);
        versionList.remove(versionList.size() - 1);
        versionList.replaceAll(String::trim);
        versionList.removeIf(version -> {
            if (version.contains("-") && (version.lastIndexOf("-") > version.indexOf("+") + 1)) {
                return !gameVersion.equals(version.substring(version.indexOf("+") + 1, version.lastIndexOf("-")));
            } else if (version.length() - version.replace(".", "").length() > (dotCount + 2)) {
                return !gameVersion.equals(version.substring(version.indexOf("+") + 1, version.lastIndexOf(".")));
            } else {
                return !gameVersion.equals(version.substring(version.indexOf("+") + 1));
            }
        });
        Collections.reverse(versionList);

        if (versionList.isEmpty()) {
            return null;
        }

        URL url;
        String version = versionList.get(0);
        if (above1_18_2) {
             url = new URL("https://mvn.devos.one/snapshots/io/github/fabricators_of_create/Porting-Lib/porting-lib/" + version + "/porting-lib-" + version + ".jar");
        } else {
            url = new URL("https://mvn.devos.one/snapshots/io/github/fabricators_of_create/Porting-Lib/" + version + "/Porting-Lib-" + version + ".jar");
        }

        return url;
    }

    public static <T extends Throwable> RuntimeException skipExceptionHandling(Throwable t) throws T {
        throw (T) t;
    }
}
