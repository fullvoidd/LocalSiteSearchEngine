package main.services;

import org.json.simple.JSONObject;

/**
 * This class is used to generate API request result when an error occurs.
 */
public class ErrorResultResponse {


    /**
     * Static method. It creates a <code>JSONObject</code> and fills it in.
     *
     * @param errorText the error description
     * @return <code>JSONObject</code> with result(false) and error(errorText) keys.
     */
    public static JSONObject getResult(String errorText) {
        JSONObject result = new JSONObject();

        result.put("result", false);
        result.put("error", errorText);

        return result;
    }
}
