package slimeknights.mantle.resources;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ResourceJsonTest {
  private static final Path PROJECT_ROOT = Path.of(System.getProperty("mantle.projectDir"));
  private static final List<Path> ROOTS = List.of(
    PROJECT_ROOT.resolve(Path.of("src", "main", "resources")),
    PROJECT_ROOT.resolve(Path.of("src", "generated", "resources"))
  );

  @TestFactory
  Stream<DynamicTest> allPackJsonIsSyntacticallyValid() throws Exception {
    List<Path> jsonFiles;
    try (Stream<Path> paths = ROOTS.stream().filter(Files::isDirectory).flatMap(ResourceJsonTest::walk)) {
      jsonFiles = paths.filter(path -> path.toString().endsWith(".json"))
        .sorted(Comparator.comparing(Path::toString))
        .toList();
    }
    assertFalse(jsonFiles.isEmpty(), "Resource validation found no JSON files");
    return jsonFiles.stream().map(path -> DynamicTest.dynamicTest(path.toString(), () ->
      assertDoesNotThrow(() -> {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
          JsonParser.parseReader(reader);
        }
      })));
  }

  private static Stream<Path> walk(Path root) {
    try {
      return Files.walk(root);
    } catch (Exception exception) {
      throw new RuntimeException("Failed to enumerate resource root " + root, exception);
    }
  }
}
