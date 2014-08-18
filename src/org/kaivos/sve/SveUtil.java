package org.kaivos.sve;

import java.io.File;

public class SveUtil {

	public static File[] getSveDirectory() {
		
		switch (System.getProperty("os.name").toLowerCase()) {
		case "linux":
		case "unix":
		{
			return new File[] { new File("/usr/local/lib/sve/") };
		}
		}
		
		return new File[] { new File("sve/") };
	}
}
