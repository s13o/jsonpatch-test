package s13o.jpt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PatchService {

    private final ObjectMapper mapper;

    public Order applyPatch(Order existingOrder, JsonPatch patch) {
        return Try.of(() -> patchOrder(existingOrder, patch))
                .getOrElseThrow(ex -> new RuntimeException("ORDER.UPDATE.FAILED", ex));
    }

    private Order patchOrder(Order existingOrder, JsonPatch patch)
            throws JsonPatchException, JsonProcessingException {
        final JsonNode patched = patch.apply(mapper.valueToTree(existingOrder));
        return mapper.treeToValue(patched, Order.class);
    }

}
