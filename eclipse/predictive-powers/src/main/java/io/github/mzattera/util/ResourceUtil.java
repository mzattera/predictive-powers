/**
 * 
 */
package io.github.mzattera.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * This class deals with loading resource files; a well known and annoying
 * problem in Java :-) <br/>
 * <br/>
 * Tests have been performed running it within Eclipse or from a .jar file.
 * 
 * <ul>
 * 
 * <li>Standard Java project</li>
 * <ul>
 * <li>When running in Eclipse, it can get URLs only for files directly in /src
 * folder. They can be addressed with their names (e.g. 'f.txt' or
 * '/f.txt').</li>
 * 
 * <li>In addition, when running from a .jar, it can get URLs for any files
 * above /src folder (packages). They can be addressed with their full names,
 * including package (e.g. 'io/github/mzattera/util/util.txt' or
 * '/io/github/mzattera/util/util.txt').</li>
 * </ul>
 * 
 * <li>Maven project</li>
 * <ul>
 * <li>Both when running in Eclipse or from a .jat, it can get URLs for files in /src/main/java and /src/main/resources ant their sub-folders.
 * They can be addressed with their names (e.g. '/f.txt', 
 * 'io/github/mzattera/util/util.txt', 'resource.txt', 'resources/resource.txt').<br/>
 * </li>
 * 
 * </ul>
 * 
 * </ul>
 * 
 * A valid URL can always be converted in a InputStream with getResourceStream(), but it can be converted
 * in a File with getResourceFile() only if the code is not running inside a .jar. <br/>
 * <br/>
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public final class ResourceUtil {

	private ResourceUtil() {
	}

	public static File getResourceFile(String resourceName) {
		try {
			URL url = getResourceUrl(resourceName);
			return (url == null) ? null : new File(url.toURI());
		} catch (Exception e) {
			return null;
		}
	}

	public static InputStream getResourceStream(String resourceName) {
		try {
			URL url = getResourceUrl(resourceName);
			return (url != null) ? url.openStream() : null;
		} catch (IOException e) {
			return null;
		}
	}

	public static URL getResourceUrl(String resourceName) {
		URL url = getResource(resourceName);

		if ((url == null) && resourceName.startsWith("/") && (resourceName.length() > 1))
			url = getResourceUrl(resourceName.substring(1));

		if (url == null) {
			if (!resourceName.startsWith("resources/")) {
				url = getResource("resources/" + resourceName);
			} else {
				if (resourceName.length() > "resources/".length()) {
					url = getResource(resourceName.substring("resources/".length()));
				}
			}
		}
		return url;
	}

	private static URL getResource(String resourceName) {

		URL url = Thread.currentThread().getContextClassLoader().getResource(resourceName);

		if (url == null) {
			url = ResourceUtil.class.getClassLoader().getResource(resourceName);
		}

		return url;
	}
}
