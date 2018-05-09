package s13o.jpt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {Conf.class})
public class PatchTest {

    private static final String JSON_PATCH = "[ " +
            "{ \"op\": \"replace\", \"path\": \"/name\", \"value\": \"lastname\" }, " +
            "{ \"op\": \"test\", \"path\": \"/version\", \"value\": 1 } " +
            "]";
    private static final String WRONG_VERSION_JSON_PATCH = "[ " +
            "{ \"op\": \"replace\", \"path\": \"/name\", \"value\": \"lastname\" }, " +
            "{ \"op\": \"test\", \"path\": \"/verssion\", \"value\": 1 } " +
            "]";

    @Autowired
    private PatchService service;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private JsonSchemaFactory factory;

    @Test
    public void testSimplePatch()
            throws Exception {
        Order order = new Order().setId(1).setName("name");

        JsonPatch patch = patch("[ { \"op\": \"replace\", \"path\": \"/name\", \"value\": \"lastname\" } ]");
        Order patched = service.applyPatch(order, patch);

        Assert.assertEquals("lastname", patched.getName());
    }

    @Test
    public void testCorrectVersionPatch()
            throws Exception {
        Order order = new Order().setId(1).setName("name").setVersion(1);

        JsonPatch patch = patch(JSON_PATCH);
        Order patched = service.applyPatch(order, patch);

        Assert.assertEquals("lastname", patched.getName());
    }

    @Test(expected = RuntimeException.class)
    public void testWrongVersionPatch()
            throws Exception {
        Order order = new Order().setId(1).setName("name").setVersion(2);

        JsonPatch patch = patch(JSON_PATCH);
        Order patched = service.applyPatch(order, patch);
    }

    @Test
    public void checkTestSchema()
            throws Exception {
        testSchema(JSON_PATCH, getTestSchema());
    }

    @Test
    public void checkTestVersionedSchema()
            throws Exception {
        testSchema(JSON_PATCH, getTestVersionedSchema());
    }

    @Test//(expected = RuntimeException.class)
    public void wrongTestVersionedSchema()
            throws Exception {
        testSchema(WRONG_VERSION_JSON_PATCH, getTestVersionedSchema());
    }

    private void testSchema(String patch, JsonNode schemaNode)
            throws IOException, ProcessingException {
        JsonNode pathNode = mapper.readTree(patch);
        final JsonSchema schema = factory.getJsonSchema(schemaNode);
        ProcessingReport report = schema.validate(pathNode);
        // see https://www.jsonschemavalidator.net/
        Assert.assertTrue(report.isSuccess());
    }


    private JsonNode getTestVersionedSchema()
            throws IOException {
        try (InputStream exampleInput = getClass().getClassLoader()
                .getResourceAsStream("s13o/jpt/test-versioned-schema.json")) {
            return mapper.readTree(exampleInput);
        }
    }

    private JsonNode getTestSchema()
            throws IOException {
        try (InputStream exampleInput = getClass().getClassLoader()
                .getResourceAsStream("s13o/jpt/test-schema.json")) {
            return mapper.readTree(exampleInput);
        }
    }

    private JsonPatch patch(String value)
            throws IOException {
        try (InputStream in = new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8))) {
            return mapper.readValue(in, JsonPatch.class);
        }
    }

}
