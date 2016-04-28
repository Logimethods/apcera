/**
 * @see http://www.adam-bien.com/roller/abien/entry/converting_json_to_map_with
 */
package spark.stream.telematics;

import java.io.IOException;
import java.util.Map;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * @author laugimethods
 *
 */
public class JSonHelper {
	
	private static ScriptEngine ENGINE = new ScriptEngineManager().getEngineByName("javascript");;

    @SuppressWarnings("unchecked")
	public static final Map<String, String> parseJsonIntoMap(String json) throws IOException, ScriptException {
        String script = "Java.asJSONCompatible(" + json + ")";
        Object result = ENGINE.eval(script);
        return (Map<String, String>) result;
    }
}
