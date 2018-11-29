package org.eclipse.leshan.server.demo.scriptedpush;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;

public class ScriptedPushParser {
	
	public static List<ScriptEntry> parse(String filePath) throws FileNotFoundException, IOException{
		File file = new File(filePath);
		if (!file.exists()) 
			throw new IllegalArgumentException(String.format("File %s does not exist", file));
		
		if (!file.isFile()) 
			throw new IllegalArgumentException(String.format("%s is not a file", file));
		
		if (!file.canRead())
			throw new IllegalArgumentException(String.format("File %s is not readable", file));
		
		
		List<ScriptEntry> result = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
		    String line;
		    int lineNumber = 1;
			while ((line = br.readLine()) != null) {
				// read odd line
				long time;
				try {
					time = Long.parseLong(line);	
				}catch (NumberFormatException e ) {
					throw new IllegalArgumentException(String.format("Error in File %s, line %d : %s is not a integer ", file, lineNumber, line));
				}
				if (time <0)
					throw new IllegalArgumentException(String.format("Error in File %s, line %d : %s MUST be a positive integer ", file, lineNumber, line));
				lineNumber++;
				
				// read even line
				JsonValue json;
				line = br.readLine();
				if (line == null) {
					throw new IllegalArgumentException(String.format("Error in File %s, line %d : a payload line is expected ", file, lineNumber));
				}
				try {
					json = Json.parse(line);	
				} catch (ParseException e) {
					throw new IllegalArgumentException(String.format("Error in File %s, line %d : Json malformed : %s ", file, lineNumber, e.getMessage()));
				}
				result.add(new ScriptEntry(time, json));
				lineNumber++;
		    }
		}
		return result;
	}
}
