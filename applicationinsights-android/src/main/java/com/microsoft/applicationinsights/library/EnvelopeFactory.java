package com.microsoft.applicationinsights.library;

import com.microsoft.applicationinsights.contracts.CrashData;
import com.microsoft.applicationinsights.contracts.CrashDataHeaders;
import com.microsoft.applicationinsights.contracts.CrashDataThread;
import com.microsoft.applicationinsights.contracts.CrashDataThreadFrame;
import com.microsoft.applicationinsights.contracts.DataPoint;
import com.microsoft.applicationinsights.contracts.DataPointType;
import com.microsoft.applicationinsights.contracts.EventData;
import com.microsoft.applicationinsights.contracts.MessageData;
import com.microsoft.applicationinsights.contracts.MetricData;
import com.microsoft.applicationinsights.contracts.PageViewData;
import com.microsoft.applicationinsights.contracts.SessionState;
import com.microsoft.applicationinsights.contracts.SessionStateData;
import com.microsoft.applicationinsights.logging.InternalLogging;
import com.microsoft.telemetry.Data;
import com.microsoft.telemetry.Domain;
import com.microsoft.telemetry.cs2.Envelope;
import com.microsoft.telemetry.ITelemetry;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class EnvelopeFactory {

    /**
     * The schema version
     */
    protected static final int CONTRACT_VERSION = 2;

    /**
     * The tag for logging
     */
    private static final String TAG = "EnvelopeManager";

    /**
     * Flag which determines, if the EnvelopeManager has been configured, yet
     */
    private boolean configured;

    /**
     * Volatile boolean for double checked synchronize block
     */
    private static volatile boolean isLoaded = false;

    /**
     * Synchronization LOCK for setting static context
     */
    private static final Object LOCK = new Object();

    /**
     * The singleton INSTANCE of this class
     */
    private static EnvelopeFactory instance;

    /**
     * The context for this recorder
     */
    private TelemetryContext context;

    /**
     * Map of properties, which should be set for each envelope
     */
    private Map<String, String> commonProperties;

    /**
     * Create an instance of EnvelopeFactory
     *
     * @param telemetryContext the telemetry context
     * @param commonProperties a map of common properties which should be set for all envelopes
     */
    protected EnvelopeFactory(TelemetryContext telemetryContext, Map<String,String> commonProperties){
        this.context = telemetryContext;
        this.commonProperties = commonProperties;
        this.configured = true;
    }

    /**
     * @return the INSTANCE of EnvelopeFactory or null if not yet initialized
     */
    protected static EnvelopeFactory getInstance() {
        if (EnvelopeFactory.instance == null) {
            InternalLogging.error(TAG, "getInstance was called before initialization");
        }

        return EnvelopeFactory.instance;
    }

    /**
     * Configures the shared instance with a telemetry context, which is needed to create envelops.
     * Warning: Method should be called before creating envelops.
     *
     * @param context          the telemetry context, which is used to create envelops with proper context information.
     * @param commonProperties Map of properties
     */
    protected static void initialize(TelemetryContext context, Map<String, String> commonProperties) {
        // note: isPersistenceLoaded must be volatile for the double-checked LOCK to work
        if (!isLoaded) {
            synchronized (EnvelopeFactory.LOCK) {
                if (!isLoaded) {
                    isLoaded = true;
                    instance = new EnvelopeFactory(context, commonProperties);
                }
            }
        }
    }

    /**
     * Create an envelope template
     *
     * @return the envelope used for telemetry
     */
    protected Envelope createEnvelope() {
        Envelope envelope = new Envelope();
        this.context.updateScreenResolution(ApplicationInsights.INSTANCE.getContext());
        envelope.setAppId(this.context.getPackageName());
        envelope.setAppVer(this.context.getApplication().getVer());
        envelope.setTime(Util.dateToISO8601(new Date()));
        envelope.setIKey(this.context.getInstrumentationKey());
        envelope.setUserId(this.context.getUser().getId());
        envelope.setDeviceId(this.context.getDevice().getId());
        envelope.setOsVer(this.context.getDevice().getOsVersion());
        envelope.setOs(this.context.getDevice().getOs());

        Map<String, String> tags = this.context.getContextTags();
        if (tags != null) {
            envelope.setTags(tags);
        }
        return envelope;
    }

    /**
     * Create an envelope with the given object as its base data
     *
     * @param data The telemetry we want to wrap inside an Enevelope and send to the server
     * @return the envelope that includes the telemetry data
     */
    protected Envelope createEnvelope(Data<Domain> data) {

        Envelope envelope = createEnvelope();
        envelope.setData(data);
        Domain baseData = data.getBaseData();
        if(baseData instanceof ITelemetry) {
            String envelopeName = ((ITelemetry) baseData).getEnvelopeName();
            envelope.setName(envelopeName);
        }

        // todo: read sample rate from settings store and set sampleRate(percentThrottled)
        // todo: set flags from settings store and set flags(persistence, latency)
        //envelope.setSeq(this.channelId + ":" + this.seqCounter.incrementAndGet());

        return envelope;
    }

    /**
     * Create an envelope with the given object as its base data
     *
     * @param telemetryData The telemetry we want to wrap inside an Enevelope and send to the server
     * @return the envelope that includes the telemetry data
     */
    protected Data<Domain> createData(ITelemetry telemetryData) {
        addCommonProperties(telemetryData);

        Data<Domain> data = new Data<Domain>();
        data.setBaseData(telemetryData);
        data.setBaseType(telemetryData.getBaseType());
        data.QualifiedName = telemetryData.getEnvelopeName();

        return data;
    }

    /**
     * Creates information about an event for Application Insights. This method gets called by a
     * CreateTelemetryDataTask in order to create and forward data on a background thread.
     *
     * @param eventName    The name of the event
     * @param properties   Custom properties associated with the event
     * @param measurements Custom measurements associated with the event
     * @return an Envelope object, which contains an event
     */
    protected Data<Domain> createEventData(String eventName,
                                           Map<String, String> properties,
                                           Map<String, Double> measurements) {
        Data<Domain> data = null;
        if (isConfigured()) {
            EventData telemetry = new EventData();
            telemetry.setName(ensureNotNull(eventName));
            telemetry.setProperties(properties);
            telemetry.setMeasurements(measurements);

            data = createData(telemetry);
        }
        return data;
    }

    /**
     * Creates tracing information for Application Insights. This method gets called by a
     * CreateTelemetryDataTask in order to create and forward data on a background thread.
     *
     * @param message    The message associated with this trace
     * @param properties Custom properties associated with the event
     * @return an Envelope object, which contains a trace
     */
    protected Data<Domain> createTraceData(String message, Map<String, String> properties) {
        Data<Domain> data = null;
        if (isConfigured()) {
            MessageData telemetry = new MessageData();
            telemetry.setMessage(this.ensureNotNull(message));
            telemetry.setProperties(properties);

            data = createData(telemetry);
        }
        return data;
    }

    /**
     * Creates information about an aggregated metric for Application Insights. This method gets
     * called by a CreateTelemetryDataTask in order to create and forward data on a background thread.
     *
     * @param name  The name of the metric
     * @param value The value of the metric
     * @return an Envelope object, which contains a metric
     */
    protected Data<Domain> createMetricData(String name, double value) {
        Data<Domain> data = null;
        if (isConfigured()) {
            MetricData telemetry = new MetricData();
            DataPoint dataPoint = new DataPoint();
            dataPoint.setCount(1);
            dataPoint.setKind(DataPointType.Measurement);
            dataPoint.setMax(value);
            dataPoint.setMax(value);
            dataPoint.setName(ensureNotNull(name));
            dataPoint.setValue(value);
            List<DataPoint> metricsList = new ArrayList<DataPoint>();
            metricsList.add(dataPoint);
            telemetry.setMetrics(metricsList);

            data = createData(telemetry);
        }
        return data;
    }

    /**
     * Creates information about an handled or unhandled exception to Application Insights. This
     * method gets called by a CreateTelemetryDataTask in order to create and forward data on a
     * background thread.
     *
     * @param exception  The exception to track
     * @param properties Custom properties associated with the event
     * @return an Envelope object, which contains a handled or unhandled exception
     */
    protected Data<Domain> createExceptionData(Throwable exception, Map<String, String> properties) {
        Data<Domain> data = null;
        if (isConfigured()) {
            CrashData telemetry = this.getCrashData(exception, properties);

            data = createData(telemetry);
        }
        return data;
    }

    /**
     * Creates information about a page view for Application Insights. This method gets called by a
     * CreateTelemetryDataTask in order to create and forward data on a background thread.
     *
     * @param pageName     The name of the page
     * @param properties   Custom properties associated with the event
     * @param measurements Custom measurements associated with the event
     * @return an Envelope object, which contains a page view
     */
    protected Data<Domain> createPageViewData(
          String pageName,
          Map<String, String> properties,
          Map<String, Double> measurements) {
        Data<Domain> data = null;
        if (isConfigured()) {
            PageViewData telemetry = new PageViewData();
            telemetry.setName(ensureNotNull(pageName));
            telemetry.setUrl(null);
            telemetry.setProperties(properties);
            telemetry.setMeasurements(measurements);

            data = createData(telemetry);
        }
        return data;
    }

    /**
     * Creates information about a new session view for Application Insights. This method gets
     * called by a CreateTelemetryDataTask in order to create and forward data on a background thread.
     *
     * @return an Envelope object, which contains a session
     */
    protected Data<Domain> createNewSessionData() {
        Data<Domain> data = null;
        if (isConfigured()) {
            SessionStateData telemetry = new SessionStateData();
            telemetry.setState(SessionState.Start);

            data = createData(telemetry);
        }
        return data;
    }

    /**
     * Adds common properties to the given telemetry data.
     *
     * @param telemetry The telemetry data
     */
    protected void addCommonProperties(ITelemetry telemetry) {
        telemetry.setVer(CONTRACT_VERSION);
        if (this.commonProperties != null) {
            Map<String, String> map = telemetry.getProperties();
            if (map != null) {
                map.putAll(this.commonProperties);
            }
            telemetry.setProperties(map);
        }
    }

    /**
     * Ensures required string values are non-null
     */
    private String ensureNotNull(String input) {
        if (input == null) {
            return "";
        } else {
            return input;
        }
    }

    /**
     * Set properties, which should be set for each envelope.
     *
     * @param commonProperties a map with properties, which should be set for each envelope
     */
    protected void setCommonProperties(Map<String, String> commonProperties) {
        this.commonProperties = commonProperties;
    }


    /**
     * Parse an exception and it's stack trace and create the CrashData object
     *
     * @param exception  the throwable object we want to create a crashdata from
     * @param properties properties used foor the CrashData
     * @return a CrashData object that contains the stacktrace and context info
     */
    private CrashData getCrashData(Throwable exception, Map<String, String> properties) {
        Throwable localException = exception;
        if (localException == null) {
            localException = new Exception();
        }

        // TODO: set handledAt - Is of relevance in future releases, not at the moment
        // read stack frames
        List<CrashDataThreadFrame> stackFrames = new ArrayList<CrashDataThreadFrame>();
        StackTraceElement[] stack = localException.getStackTrace();
        for (int i = 0; i < stack.length - 1; i++) {
            StackTraceElement rawFrame = stack[i];
            CrashDataThreadFrame frame = new CrashDataThreadFrame();
            frame.setSymbol(rawFrame.toString());
            stackFrames.add(frame);
            frame.setAddress("");
        }

        CrashDataThread crashDataThread = new CrashDataThread();
        crashDataThread.setFrames(stackFrames);
        List<CrashDataThread> threads = new ArrayList<CrashDataThread>(1);
        threads.add(crashDataThread);

        CrashDataHeaders crashDataHeaders = new CrashDataHeaders();
        crashDataHeaders.setId(UUID.randomUUID().toString());

        String message = localException.getMessage();
        crashDataHeaders.setExceptionReason(ensureNotNull(message));
        crashDataHeaders.setExceptionType(localException.getClass().getName());
        crashDataHeaders.setApplicationIdentifier(this.context.getPackageName());

        CrashData crashData = new CrashData();
        crashData.setThreads(threads);
        crashData.setHeaders(crashDataHeaders);
        crashData.setProperties(properties);

        return crashData;
    }

    protected boolean isConfigured() {
        if (!configured) {
            InternalLogging.warn(TAG, "Could not create telemetry data. You have to setup & start ApplicationInsights first.");
        }
        return configured;
    }

    /**
     * Get Context
     * @return The telemetry context associated with this envelope factory
     */
    protected TelemetryContext getContext() {
        return this.context;
    }
}
