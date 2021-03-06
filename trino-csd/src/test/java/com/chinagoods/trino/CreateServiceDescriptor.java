package com.chinagoods.trino;

import com.google.common.base.Splitter;
import com.google.inject.spi.Message;
import io.airlift.configuration.ConfigurationFactory;
import io.airlift.configuration.ConfigurationInspector;
import io.airlift.configuration.ConfigurationInspector.ConfigAttribute;
import io.airlift.configuration.ConfigurationInspector.ConfigRecord;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.*;

public class CreateServiceDescriptor {

    private static final String TRINO_CLUSTER = "|||TRINO_CLUSTER|||";
    private static final String TRINO_CSD_VERSION = "|||TRINO_CSD_VERSION|||";

    private static final Set<String> EXCLUDES = new HashSet<String>(Arrays.asList("node.environment", "node.data-dir",
            "http-server.http.port", "resources.reserved-system-memory", "trino.version", "maven.repo.local",
            "maven.repo.remote", "coordinator", "plugin.config-dir", "plugin.dir", "discovery-server.enabled",
            "node-scheduler.include-coordinator", "failure-detector.enabled", "failure-detector.expiration-grace-interval",
            "failure-detector.heartbeat-interval", "failure-detector.http-client.authentication.enabled",
            "failure-detector.http-client.connect-timeout", "failure-detector.http-client.idle-timeout",
            "failure-detector.http-client.max-connections", "failure-detector.http-client.max-connections-per-server",
            "failure-detector.http-client.max-content-length",
            "failure-detector.http-client.max-requests-queued-per-destination", "failure-detector.http-client.max-threads",
            "failure-detector.http-client.min-threads", "failure-detector.http-client.request-timeout",
            "failure-detector.threshold", "failure-detector.warmup-interval", "http-server.log.path",
            "query.max-queued-queries", "query.max-concurrent-queries", "query.remote-task.max-consecutive-error-count"));

    public static void main(String[] args) throws JSONException, IOException {
        File inputPath = new File(args[0]);
        File outputPath = new File(args[1]);
        String trinoCsdVersion = args[2];

        Map<String, String> properties = new HashMap<String, String>();
        properties.put("node.environment", "test");

        ConfigurationFactory factory = new ConfigurationFactory(properties);

//      new NodeModule(),
//              new DiscoveryModule(),
//              new HttpServerModule(),
//              new JsonModule(),
//              new JaxrsModule(true),
//              new MBeanModule(),
//              new JmxModule(),
//              new JmxHttpModule(),
//              new LogJmxModule(),
//              new TraceTokenModule(),
//              new JsonEventModule(),
//              new HttpEventModule(),
//              new ServerSecurityModule(),
//              new AccessControlModule(),
//              new EventListenerModule(),
//              new ServerMainModule(sqlParserOptions),
//              new GracefulShutdownModule());

//        ImmutableList.Builder<Module> modules = ImmutableList.builder();
//        modules.add(new NodeModule(),
//                new DiscoveryModule(),
//                new HttpServerModule(),
//                new JsonModule(),
//                new JaxrsModule(true),
//                new MBeanModule(),
//                new JmxModule(),
//                new JmxHttpModule(),
//                new LogJmxModule(),
//                new TraceTokenModule(),
//                new JsonEventModule(),
//                new HttpEventModule(),
////                new EmbeddedDiscoveryModule(),
//                new ServerSecurityModule(),
//                new AccessControlModule(),
//                new EventListenerModule(),
//                new ServerMainModule(new SqlParserOptions()),
//                new GracefulShutdownModule()
//        );

        List<Message> messages = factory.validateRegisteredConfigurationProvider();

        System.out.println(messages);

        ConfigurationInspector inspector = new ConfigurationInspector();
        List<JSONObject> scdProps = new ArrayList<JSONObject>();
        for (ConfigRecord<?> record : inspector.inspect(factory)) {
            for (ConfigAttribute attribute : record.getAttributes()) {
                JSONObject jsonNode = toJsonNode(attribute);
                if (jsonNode != null) {
                    scdProps.add(jsonNode);
                }
            }
        }

        Collections.sort(scdProps, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject o1, JSONObject o2) {
                try {
                    String s1 = o1.getString("configName");
                    String s2 = o2.getString("configName");
                    return s1.compareTo(s2);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        JSONArray array = new JSONArray();
        for (JSONObject o : scdProps) {
            array.put(o);
        }

        // TRINO_CLUSTER

        StringWriter swriter = new StringWriter();
        {

            InputStream inputStream = new FileInputStream(inputPath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            PrintWriter writer = new PrintWriter(swriter);
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.contains(TRINO_CLUSTER)) {
                    for (JSONObject o : scdProps) {
                        writer.println(',');
                        writer.println(o.toString());
                    }
                } else if (line.contains(TRINO_CSD_VERSION)) {
                    writer.println(line.replace(TRINO_CSD_VERSION, trinoCsdVersion));
                } else {
                    writer.println(line);
                }
            }
            writer.close();
            reader.close();
        }
        {
            String s = swriter.toString();
            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(s);
            } catch (JSONException e) {
                BufferedReader reader = new BufferedReader(new StringReader(s));
                String line;
                int lineNum = 0;
                int charCount = 0;
                while ((line = reader.readLine()) != null) {
                    System.out.println(lineNum + "," + charCount + ": " + line);
                    lineNum++;
                    charCount += line.length() + 1;
                }
                throw e;
            }
            PrintWriter writer = new PrintWriter(outputPath);
            writer.print(jsonObject.toString(1));
            writer.close();
        }
    }

    private static JSONObject toJsonNode(ConfigAttribute attribute) throws JSONException {

        String propertyName = attribute.getPropertyName();
        if (propertyName.startsWith("node.") || EXCLUDES.contains(propertyName)) {
            System.err.println("Skipping property [" + propertyName + "]");
            return null;
        }

        String defaultValue = attribute.getDefaultValue();
        String attributeName = attribute.getAttributeName();
        String description = attribute.getDescription();

        if (defaultValue.equals("null")) {
            System.err.println("Skipping property [" + propertyName + "] because of null default value.");
            return null;
        }

        JSONObject jsonObject = new JSONObject();
        String name = getName(propertyName);
        String label = getLabel(name, attributeName);
        jsonObject.put("name", name);
        jsonObject.put("label", label);
        if (description.trim()
                .isEmpty()) {
            jsonObject.put("description", label);
        } else {
            jsonObject.put("description", description);
        }
        jsonObject.put("configName", propertyName);
        jsonObject.put("required", true);
        jsonObject.put("type", "string");
        jsonObject.put("default", defaultValue);
        jsonObject.put("configurableInWizard", false);
        return jsonObject;
    }

    private static String getLabel(String name, String attributeName) {
        StringBuilder builder = new StringBuilder();
        for (String s : Splitter.on('_')
                .split(name)) {
            builder.append(s.substring(0, 1)
                    .toUpperCase())
                    .append(s.substring(1))
                    .append(' ');
        }
        builder.append('(')
                .append(attributeName)
                .append(')');
        return builder.toString();
    }

    private static String getName(String propertyName) {
        return propertyName.replace('.', '_')
                .replace('-', '_');
    }

}
