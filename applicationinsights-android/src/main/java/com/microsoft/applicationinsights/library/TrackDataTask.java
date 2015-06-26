package com.microsoft.applicationinsights.library;

import android.os.AsyncTask;

import com.microsoft.telemetry.IChannel;
import com.microsoft.telemetry.ITelemetry;

import java.util.Map;

class TrackDataTask extends AsyncTask<Void, Void, Void> {

    /**
     * Enum type to determine how to create and process the telemetry item.
     */
    protected enum DataType {
        NONE,
        EVENT,
        TRACE,
        METRIC,
        PAGE_VIEW,
        HANDLED_EXCEPTION,
        UNHANDLED_EXCEPTION,
        NEW_SESSION
    }

    /**
     * The type to create.
     */
    protected final DataType type;

    /**
     * Name of event, page view, metric, or trace.
     */
    protected String name;

    /**
     * Custom properties of an telemetry item.
     */
    protected Map<String,String> properties;

    /**
     * Measurements, which can be set for events and page views.
     */
    protected Map<String, Double> measurements;

    /**
     * The numeric value of an metric item.
     */
    protected double metric;

    /**
     * A handled or unhandled exception.
     */
    protected Throwable exception;

    /**
     * Generic telemetry data.
     */
    protected ITelemetry telemetry;

    /**
     * The envelope factory to use for creating a telemetry item.
     */
    protected EnvelopeFactory envelopeFactory;

    /**
     * The channel to use for processing a telemetry item.
     */
    protected IChannel channel;

    /**
     * Create a TrackDataTask.
     *
     * @param type the type to track
     */
    protected TrackDataTask(DataType type){
        this.type = type;
        this.envelopeFactory = EnvelopeFactory.getInstance();
        this.channel = ChannelManager.getInstance().getChannel();
    }

    /**
     * Create a TrackDataTask.
     *
     * @param telemetry generic telemetry data
     */
    protected TrackDataTask(ITelemetry telemetry){
        this(DataType.NONE);
        this.telemetry = telemetry;
    }

    /**
     * Create a TrackDataTask (Metric).
     *
     * @param type the type to track. This should be DataType.METRIC
     * @param metricName the name of the metric
     * @param metric the metric value
     */
    protected TrackDataTask(DataType type, String metricName, double metric){
        this(type);
        this.name = metricName;
        this.metric = metric;
    }

    /**
     * Create a TrackDataTask (Event/PageView).
     *
     * @param type the type to track. This should be DataType.PAGEVIEW or DataType.EVENT
     * @param name the name of the telemetry item
     * @param properties a map which contains custom properties
     * @param measurements a map which contains custom measurements
     */
    protected TrackDataTask(DataType type,
                            String name,
                            Map<String, String> properties,
                            Map<String, Double> measurements){
        this(type);
        this.name = name;
        this.properties = properties;
        this.measurements = measurements;
    }

    /**
     * Create a TrackDataTask (Handled exception/Unhandled exception).
     *
     * @param type the type to track. This should be DataType.UNHANDLED_EXCEPTION or DataType.HANDLED_EXCEPTION
     * @param exception the exception to use for building the telemetry item
     * @param properties a map which contains custom properties
     */
    protected TrackDataTask(DataType type,
                            Throwable exception,
                            Map<String, String> properties){
        this(type);
        this.exception = exception;
        this.properties = properties;
    }

    /**
     * Set the envelope factory to use for creating a telemetry item. This method is used for
     * dependency injection.
     *
     * @param envelopeFactory the envelope factory to use for creating a telemetry item
     */
    protected void setEnvelopeFactory(EnvelopeFactory envelopeFactory) {
        this.envelopeFactory = envelopeFactory;
    }

    /**
     * Set channel to use for processing a telemetry item. This method is used for dependency
     * injection.
     *
     * @param channel the channel to use for processing a telemetry item
     */
    protected void setChannel(IChannel channel) {
        this.channel = channel;
    }

    @Override
    protected Void doInBackground(Void... params) {

        //trackEnvelope();

        return null;
    }

//    protected void trackEnvelope(){
//        Envelope envelope = null;
//        switch (this.type){
//            case NONE:
//                if(this.telemetry != null){
//                    envelope = EnvelopeFactory.getInstance().createEnvelope(this.telemetry);
//                }
//                break;
//            case EVENT:
//                if(this.name != null){
//                    envelope = this.envelopeFactory.createEventEnvelope(this.name, this.properties, this.measurements);
//                }
//                break;
//            case PAGE_VIEW:
//                if(this.name != null){
//                    envelope = this.envelopeFactory.createPageViewEnvelope(this.name, this.properties, this.measurements);
//                }
//                break;
//            case TRACE:
//                if(this.name != null){
//                    envelope = this.envelopeFactory.createTraceEnvelope(this.name, this.properties);
//                }
//                break;
//            case METRIC:
//                if(this.name != null){
//                    envelope = this.envelopeFactory.createMetricEnvelope(this.name, this.metric);
//                }
//                break;
//            case NEW_SESSION:
//                envelope = this.envelopeFactory.createNewSessionEnvelope();
//                break;
//            case HANDLED_EXCEPTION:
//            case UNHANDLED_EXCEPTION:
//                //TODO: Unhandled exceptions should not be processed asynchronously
//                if(exception != null){
//                    envelope = this.envelopeFactory.createExceptionEnvelope(this.exception, this.properties);
//                }
//                break;
//            default:
//                break;
//        }
//        if(envelope != null){
//            forwardEnvelope(envelope);
//        }
//    }
//
//    protected void forwardEnvelope(Envelope envelope){
//        if(type == DataType.UNHANDLED_EXCEPTION){
//            this.channel.processUnhandledException(envelope);
//        }else{
//            this.channel.log(envelope);
//        }
//    }
}