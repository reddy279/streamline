package org.apache.streamline.streams.layout.storm;

import org.apache.streamline.streams.layout.TopologyLayoutConstants;
import org.apache.streamline.streams.layout.exception.BadTopologyLayoutException;

import java.util.List;
import java.util.Map;

public class NotificationBoltFluxComponent extends AbstractFluxComponent {
    @Override
    protected void generateComponent() {
        String boltId = "notificationBolt" + UUID_FOR_COMPONENTS;
        String boltClassName = "org.apache.streamline.streams.runtime.storm.bolt.notification" +
                ".NotificationBolt";
        String[] constructorArgNames =  {
                TopologyLayoutConstants.JSON_KEY_NOTIFIER_NAME
        };
        List boltConstructorArgs = getConstructorArgsYaml(constructorArgNames);
        String[] configMethodNames = {"withHBaseConfigKey"};
        String[] configKeys = { TopologyLayoutConstants.JSON_KEY_NOTIFIER_CONFIG_KEY };
        List configMethods = getConfigMethodsYaml(configMethodNames,
                configKeys);
        component = createComponent(boltId, boltClassName, null, boltConstructorArgs, configMethods);
        addParallelismToComponent();
    }

    @Override
    public void validateConfig () throws BadTopologyLayoutException {
        super.validateConfig();
        String className = (String) conf.get(TopologyLayoutConstants.JSON_KEY_NOTIFIER_CLASSNAME);
        // Streamline frameworks supports email notifiers by default. However there is support for custom notifiers as well. Here, we handle validation for fields
        // necessary for email notifiers only. Otherwise we pass. For other custom notifiers we could add validate method to Notifier interface and call it
        // here or we could let the custom notifier handle it at runtime after submitting the topology
        if ("org.apache.streamline.streams.notifiers.EmailNotifier".equals(className)) {
            validateStringFields();
            validateProperties();
            validateFieldValues();
        }
    }

    private void validateStringFields () throws BadTopologyLayoutException {
        String[] requiredStringFields = {
                TopologyLayoutConstants.JSON_KEY_NOTIFIER_NAME,
                TopologyLayoutConstants.JSON_KEY_NOTIFIER_JAR_FILENAME,
                TopologyLayoutConstants.JSON_KEY_NOTIFIER_CLASSNAME
        };
        validateStringFields(requiredStringFields, true);
    }

    private void validateProperties () throws BadTopologyLayoutException {
        Map<String, Object> properties = (Map) conf.get(TopologyLayoutConstants.JSON_KEY_NOTIFIER_PROPERTIES);
        if (properties == null) {
            throw new BadTopologyLayoutException(String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, TopologyLayoutConstants
                    .JSON_KEY_NOTIFIER_PROPERTIES));
        }
        String[] optionalBooleanFields = {
                TopologyLayoutConstants.JSON_KEY_NOTIFIER_SSL,
                TopologyLayoutConstants.JSON_KEY_NOTIFIER_STARTTLS,
                TopologyLayoutConstants.JSON_KEY_NOTIFIER_DEBUG,
                TopologyLayoutConstants.JSON_KEY_NOTIFIER_AUTH
        };
        validateBooleanFields(optionalBooleanFields, false, properties);
        String[] requiredStringFields = {
                TopologyLayoutConstants.JSON_KEY_NOTIFIER_USERNAME,
                TopologyLayoutConstants.JSON_KEY_NOTIFIER_PASSWORD,
                TopologyLayoutConstants.JSON_KEY_NOTIFIER_HOST
        };
        validateStringFields(requiredStringFields, true, properties);

        String[] optionalStringFields = {
                TopologyLayoutConstants.JSON_KEY_NOTIFIER_PROTOCOL
        };
        validateStringFields(optionalStringFields, false, properties);
        String[] requiredIntegerFields = {
                TopologyLayoutConstants.JSON_KEY_NOTIFIER_PORT
        };
        Integer[] mins = { 1 };
        Integer[] maxes = { Integer.MAX_VALUE };
        validateIntegerFields(requiredIntegerFields, true, mins, maxes, properties);
    }

    private void validateFieldValues () throws BadTopologyLayoutException {
        Map<String, Object> fieldValues = (Map) conf.get(TopologyLayoutConstants.JSON_KEY_NOTIFIER_FIELD_VALUES);
        if (fieldValues == null) {
            throw new BadTopologyLayoutException(String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, TopologyLayoutConstants
                    .JSON_KEY_NOTIFIER_FIELD_VALUES));
        }
        String[] requiredStringFields = {
                TopologyLayoutConstants.JSON_KEY_NOTIFIER_FROM,
                TopologyLayoutConstants.JSON_KEY_NOTIFIER_TO,
                TopologyLayoutConstants.JSON_KEY_NOTIFIER_SUBJECT,
                TopologyLayoutConstants.JSON_KEY_NOTIFIER_BODY
        };
        validateStringFields(requiredStringFields, true, fieldValues);
        String[] optionalStringFields = {
                TopologyLayoutConstants.JSON_KEY_NOTIFIER_CONTENT_TYPE
        };
        validateStringFields(optionalStringFields, false, fieldValues);
    }

}
