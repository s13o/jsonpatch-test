package s13o.jpt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
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

        JsonPatch patch = patch("[ " +
                "{ \"op\": \"replace\", \"path\": \"/name\", \"value\": \"lastname\" }, " +
                "{ \"op\": \"test\", \"path\": \"/version\", \"value\": 1 } " +
                "]");
        Order patched = service.applyPatch(order, patch);

        Assert.assertEquals("lastname", patched.getName());
    }

    @Test(expected = RuntimeException.class)
    public void testWrongVersionPatch()
            throws Exception {
        Order order = new Order().setId(1).setName("name").setVersion(2);

        JsonPatch patch = patch("[ " +
                "{ \"op\": \"replace\", \"path\": \"/name\", \"value\": \"lastname\" }, " +
                "{ \"op\": \"test\", \"path\": \"/version\", \"value\": 1 } " +
                "]");
        Order patched = service.applyPatch(order, patch);

        //Assert.assertEquals("lastname", patched.getName());
    }

    @Test
    public void checkVersionRequired()
            throws Exception {
        Order order = new Order().setId(1).setName("name").setVersion(2);

        JsonNode pathNode = mapper.readTree("[ " +
                "{ \"op\": \"replace\", \"path\": \"/name\", \"value\": \"lastname\" }, " +
                "{ \"op\": \"test\", \"path\": \"/version\", \"value\": 1 } " +
                "]");


        final JsonSchema schema = factory.getJsonSchema(getTestSchema());

        ProcessingReport report = schema.validate(pathNode);

        // see https://www.jsonschemavalidator.net/

        Assert.assertTrue(report.isSuccess());


      //  JsonPatch patch = JsonPatch.fromJson(pathNode);


        //Order patched = service.applyPatch(order, patch);

        //Assert.assertEquals("lastname", patched.getName());
    }


    private JsonNode getTestSchema()
            throws IOException {
        try (InputStream exampleInput =
                     PatchTest.class.getClassLoader()
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
