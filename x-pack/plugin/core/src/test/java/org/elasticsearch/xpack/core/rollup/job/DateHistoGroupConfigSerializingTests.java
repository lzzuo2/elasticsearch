/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.core.rollup.job;

import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.fieldcaps.FieldCapabilities;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.test.AbstractSerializingTestCase;
import org.elasticsearch.xpack.core.rollup.ConfigTestHelpers;
import org.joda.time.DateTimeZone;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DateHistoGroupConfigSerializingTests extends AbstractSerializingTestCase<DateHistoGroupConfig> {
    @Override
    protected DateHistoGroupConfig doParseInstance(XContentParser parser) throws IOException {
        return DateHistoGroupConfig.PARSER.apply(parser, null).build();
    }

    @Override
    protected Writeable.Reader<DateHistoGroupConfig> instanceReader() {
        return DateHistoGroupConfig::new;
    }

    @Override
    protected DateHistoGroupConfig createTestInstance() {
        return ConfigTestHelpers.getDateHisto().build();
    }

    public void testValidateNoMapping() throws IOException {
        ActionRequestValidationException e = new ActionRequestValidationException();
        Map<String, Map<String, FieldCapabilities>> responseMap = new HashMap<>();

        DateHistoGroupConfig config = new DateHistoGroupConfig.Builder()
                .setField("my_field")
                .setInterval(new DateHistogramInterval("1d"))
                .build();
        config.validateMappings(responseMap, e);
        assertThat(e.validationErrors().get(0), equalTo("Could not find a [date] field with name [my_field] in any of the " +
                "indices matching the index pattern."));
    }

    public void testValidateNomatchingField() throws IOException {

        ActionRequestValidationException e = new ActionRequestValidationException();
        Map<String, Map<String, FieldCapabilities>> responseMap = new HashMap<>();

        // Have to mock fieldcaps because the ctor's aren't public...
        FieldCapabilities fieldCaps = mock(FieldCapabilities.class);
        responseMap.put("some_other_field", Collections.singletonMap("date", fieldCaps));

        DateHistoGroupConfig config = new DateHistoGroupConfig.Builder()
                .setField("my_field")
                .setInterval(new DateHistogramInterval("1d"))
                .build();
        config.validateMappings(responseMap, e);
        assertThat(e.validationErrors().get(0), equalTo("Could not find a [date] field with name [my_field] in any of the " +
                "indices matching the index pattern."));
    }

    public void testValidateFieldWrongType() throws IOException {

        ActionRequestValidationException e = new ActionRequestValidationException();
        Map<String, Map<String, FieldCapabilities>> responseMap = new HashMap<>();

        // Have to mock fieldcaps because the ctor's aren't public...
        FieldCapabilities fieldCaps = mock(FieldCapabilities.class);
        responseMap.put("my_field", Collections.singletonMap("keyword", fieldCaps));

        DateHistoGroupConfig config = new DateHistoGroupConfig.Builder()
                .setField("my_field")
                .setInterval(new DateHistogramInterval("1d"))
                .build();
        config.validateMappings(responseMap, e);
        assertThat(e.validationErrors().get(0), equalTo("The field referenced by a date_histo group must be a [date] type across all " +
                "indices in the index pattern.  Found: [keyword] for field [my_field]"));
    }

    public void testValidateFieldMixtureTypes() throws IOException {

        ActionRequestValidationException e = new ActionRequestValidationException();
        Map<String, Map<String, FieldCapabilities>> responseMap = new HashMap<>();

        // Have to mock fieldcaps because the ctor's aren't public...
        FieldCapabilities fieldCaps = mock(FieldCapabilities.class);
        Map<String, FieldCapabilities> types = new HashMap<>(2);
        types.put("date", fieldCaps);
        types.put("keyword", fieldCaps);
        responseMap.put("my_field", types);

        DateHistoGroupConfig config = new DateHistoGroupConfig.Builder()
                .setField("my_field")
                .setInterval(new DateHistogramInterval("1d"))
                .build();
        config.validateMappings(responseMap, e);
        assertThat(e.validationErrors().get(0), equalTo("The field referenced by a date_histo group must be a [date] type across all " +
                "indices in the index pattern.  Found: [date, keyword] for field [my_field]"));
    }

    public void testValidateFieldMatchingNotAggregatable() throws IOException {

        ActionRequestValidationException e = new ActionRequestValidationException();
        Map<String, Map<String, FieldCapabilities>> responseMap = new HashMap<>();

        // Have to mock fieldcaps because the ctor's aren't public...
        FieldCapabilities fieldCaps = mock(FieldCapabilities.class);
        when(fieldCaps.isAggregatable()).thenReturn(false);
        responseMap.put("my_field", Collections.singletonMap("date", fieldCaps));

        DateHistoGroupConfig config = new DateHistoGroupConfig.Builder()
                .setField("my_field")
                .setInterval(new DateHistogramInterval("1d"))
                .build();
        config.validateMappings(responseMap, e);
        assertThat(e.validationErrors().get(0), equalTo("The field [my_field] must be aggregatable across all indices, but is not."));
    }

    public void testValidateMatchingField() throws IOException {
        ActionRequestValidationException e = new ActionRequestValidationException();
        Map<String, Map<String, FieldCapabilities>> responseMap = new HashMap<>();

        // Have to mock fieldcaps because the ctor's aren't public...
        FieldCapabilities fieldCaps = mock(FieldCapabilities.class);
        when(fieldCaps.isAggregatable()).thenReturn(true);
        responseMap.put("my_field", Collections.singletonMap("date", fieldCaps));

        DateHistoGroupConfig config = new DateHistoGroupConfig.Builder()
                .setField("my_field")
                .setInterval(new DateHistogramInterval("1d"))
                .build();
        config.validateMappings(responseMap, e);
        assertThat(e.validationErrors().size(), equalTo(0));
    }

    public void testValidateWeek() {
        ActionRequestValidationException e = new ActionRequestValidationException();
        Map<String, Map<String, FieldCapabilities>> responseMap = new HashMap<>();

        // Have to mock fieldcaps because the ctor's aren't public...
        FieldCapabilities fieldCaps = mock(FieldCapabilities.class);
        when(fieldCaps.isAggregatable()).thenReturn(true);
        responseMap.put("my_field", Collections.singletonMap("date", fieldCaps));

        DateHistoGroupConfig config = new DateHistoGroupConfig.Builder()
            .setField("my_field")
            .setInterval(new DateHistogramInterval("1w"))
            .build();
        config.validateMappings(responseMap, e);
        assertThat(e.validationErrors().size(), equalTo(0));
    }

    /**
     * Tests that a DateHistogramGroupConfig can be serialized/deserialized correctly after
     * the timezone was changed from DateTimeZone to String.
     */
    public void testBwcSerialization() throws IOException {
        for (int runs = 0; runs < NUMBER_OF_TEST_RUNS; runs++) {
            final DateHistoGroupConfig reference = ConfigTestHelpers.getDateHisto().build();

            final BytesStreamOutput out = new BytesStreamOutput();
            reference.writeTo(out);

            // previous way to deserialize a DateHistogramGroupConfig
            final StreamInput in = out.bytes().streamInput();
            DateHistogramInterval interval = new DateHistogramInterval(in);
            String field = in.readString();
            DateHistogramInterval delay = in.readOptionalWriteable(DateHistogramInterval::new);
            DateTimeZone timeZone = in.readTimeZone();

            assertEqualInstances(reference, new DateHistoGroupConfig.Builder()
                .setField(field)
                .setInterval(interval)
                .setDelay(delay)
                .setTimeZone(timeZone)
                .build());
        }

        for (int runs = 0; runs < NUMBER_OF_TEST_RUNS; runs++) {
            final DateHistoGroupConfig config = ConfigTestHelpers.getDateHisto().build();

            // previous way to serialize a DateHistogramGroupConfig
            final BytesStreamOutput out = new BytesStreamOutput();
            config.getInterval().writeTo(out);
            out.writeString(config.getField());
            out.writeOptionalWriteable(config.getDelay());
            out.writeTimeZone(config.getTimeZone());

            final StreamInput in = out.bytes().streamInput();
            DateHistoGroupConfig deserialized = new DateHistoGroupConfig(in);

            assertEqualInstances(new DateHistoGroupConfig.Builder()
                .setField(config.getField())
                .setInterval(config.getInterval())
                .setDelay(config.getDelay())
                .setTimeZone(config.getTimeZone())
                .build(), deserialized);
        }
    }
}
