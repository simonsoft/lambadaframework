package org.lambadaframework.runtime;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.lambadaframework.jaxrs.model.ResourceMethod;
import org.lambadaframework.runtime.errorhandling.ErrorHandler;
import org.lambadaframework.runtime.models.RequestInterface;
import org.lambadaframework.runtime.models.Response;
import org.lambadaframework.runtime.router.Router;

import java.io.*;
import java.util.Map;
import java.util.Set;


public class Handler implements RequestStreamHandler {

    static final Logger logger = Logger.getLogger(Handler.class);
    JSONParser parser = new JSONParser();

    private Router router;


    public Handler setRouter(Router router) {
        this.router = router;
        return this;
    }

    public Router getRouter() {
        if (router != null) {
            return router;
        }
        return Router.getRouter();
    }

    /**
     * If request object's "method" field is null or has an invalid
     * HTTP method string it is impossible to process the request
     * thus we throw an exception and 500 HTTP error.
     *
     * @param requestObject Request object
     * @throws Exception
     */
    private void checkHttpMethod(RequestInterface requestObject)
            throws Exception {
        if (requestObject.getMethod() == null) {
            throw new Exception("Method was null");
        }
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) {

        logger.debug("Loading Java Lambda handler of ProxyWithStream");

        Object invoke;
        try {
            RequestInterface req = getParsedRequest(inputStream);

            if (req == null) {
                logger.debug("Request object is null can not proceed with request.");
            } else {
                checkHttpMethod(req);
                logger.debug("Request check is ok.");
                ResourceMethod matchedResourceMethod = getRouter().route(req);
                invoke = ResourceMethodInvoker.invoke(matchedResourceMethod, req, context);
                Response response = Response.buildFromJAXRSResponse(invoke);
                logger.debug("Handler received a response: " + response.getEntity().toString());

                OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
                String res = parseResponse(response).toJSONString();
                logger.debug("Parsed response: " + res);
                writer.write(res);
                logger.debug("Reponse should have been written.");
                writer.close();

            }

        } catch (Exception e) {
            logger.debug("Error: " + e.getMessage());
            ErrorHandler.getErrorResponse(e);
        }

    }

    private JSONObject parseResponse(Response response) throws Exception{


        logger.debug("Starting to parse response to JSON");
        JSONObject responseJson = new JSONObject();
        logger.debug("1");
        JSONObject responseBody = new JSONObject();
        //responseBody.put("input", response.getEntity().toString());
        responseBody.put("input", "apskaft");
        responseBody.put("data", "Hello world");
        logger.debug("2");

        JSONObject headerJson = new JSONObject();
        headerJson.put("x-custom-response-header", "my custom response header value");

        logger.debug("3");
        logger.debug("size: " + response.getHeaders().keySet().size());
        for(String key: response.getHeaders().keySet()) {
            logger.debug("4");
            String val = response.getHeaders().get(key);
            logger.debug("response headers key: " + key + " value: " + val);
            headerJson.put(key, val);
        }
        logger.debug("5");
        responseJson.put("statusCode", "200");
        responseJson.put("headers", headerJson);
        responseJson.put("body", responseBody);
        logger.debug("6");
        return responseJson;
    }


    /**
     * @return a ObjectMapper configured to ignore if incoming json do have properties unknown to requestProxy.
     */
    private ObjectMapper getConfiguredMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

    private RequestInterface getParsedRequest(InputStream inputStream) {
        logger.debug("Starting to parse request stream");

        try {
            JsonParser jp = new JsonFactory().createParser(inputStream);
            RequestInterface req = null;

            ObjectMapper configuredMapper = getConfiguredMapper();
            //Can't handle if stream starts with array.
            while (jp.nextToken() == JsonToken.START_OBJECT) {
                req = configuredMapper.readValue(jp, RequestProxy.class);
                logger.debug("Parsed input stream to Request object");
            }

            jp.close();
            inputStream.close();
            return req;

        } catch (IOException e) {
            logger.debug("Error: " + e.getMessage());
        } catch (Exception e) {
            logger.debug("Error:" + e.getMessage());
        }

        return null;
    }
}
