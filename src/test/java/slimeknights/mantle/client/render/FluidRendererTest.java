package slimeknights.mantle.client.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FluidRendererTest {
  private static final VertexFormat NORMAL_BLOCK = VertexFormat.builder()
    .add("Position", VertexFormatElement.POSITION)
    .add("Color", VertexFormatElement.COLOR)
    .add("UV0", VertexFormatElement.UV0)
    .add("UV2", VertexFormatElement.UV2)
    .add("Normal", VertexFormatElement.NORMAL)
    .padding(1)
    .build();
  private static final Vector3f FROM = new Vector3f(0.1f, 0.2f, 0.3f);
  private static final Vector3f TO = new Vector3f(0.8f, 0.9f, 0.7f);

  private static TextureAtlasSprite sprite() {
    TextureAtlasSprite sprite = mock(TextureAtlasSprite.class);
    when(sprite.getU(anyFloat())).thenAnswer(call -> call.<Float>getArgument(0));
    when(sprite.getV(anyFloat())).thenAnswer(call -> call.<Float>getArgument(0));
    return sprite;
  }

  private static void emit(BufferBuilder buffer, Matrix4f matrix, TextureAtlasSprite sprite, Direction face, int rotation, boolean flowing) {
    FluidRenderer.putTexturedQuad(buffer, matrix, sprite, FROM, TO, face, 0xFF40A0D0, 0x00F000F0, rotation, flowing);
  }

  private static void assertNormals(MeshData mesh, Vector3f expected) {
    int stride = NORMAL_BLOCK.getVertexSize();
    int normalOffset = NORMAL_BLOCK.getOffset(VertexFormatElement.NORMAL);
    ByteBuffer data = mesh.vertexBuffer();
    assertEquals(4, mesh.drawState().vertexCount());
    for (int vertex = 0; vertex < 4; vertex++) {
      int offset = vertex * stride + normalOffset;
      assertEquals(expected.x(), data.get(offset) / 127f, 2f / 127f, "normal X");
      assertEquals(expected.y(), data.get(offset + 1) / 127f, 2f / 127f, "normal Y");
      assertEquals(expected.z(), data.get(offset + 2) / 127f, 2f / 127f, "normal Z");
    }
  }

  @Test
  void suppliesNormalsForEveryFaceTextureRotationAndFlowMode() {
    TextureAtlasSprite sprite = sprite();
    for (Direction face : Direction.values()) {
      for (int rotation : new int[] {0, 90, 180, 270}) {
        for (boolean flowing : new boolean[] {false, true}) {
          try (ByteBufferBuilder memory = new ByteBufferBuilder(256)) {
            BufferBuilder buffer = new BufferBuilder(memory, VertexFormat.Mode.QUADS, NORMAL_BLOCK);
            emit(buffer, new Matrix4f(), sprite, face, rotation, flowing);
            try (MeshData mesh = buffer.buildOrThrow()) {
              assertNormals(mesh, new Vector3f(face.getStepX(), face.getStepY(), face.getStepZ()));
            }
          }
        }
      }
    }
  }

  private static Vector3f position(ByteBuffer data, int vertex) {
    int offset = vertex * NORMAL_BLOCK.getVertexSize() + NORMAL_BLOCK.getOffset(VertexFormatElement.POSITION);
    return new Vector3f(data.getFloat(offset), data.getFloat(offset + 4), data.getFloat(offset + 8));
  }

  @Test
  void transformsNormalsWithRotatedAndNonUniformlyScaledGeometry() {
    TextureAtlasSprite sprite = sprite();
    Matrix4f matrix = new Matrix4f().scale(2f, 0.5f, 3f).rotateXYZ(0.4f, 0.8f, 1.3f).translate(3f, -2f, 5f);
    for (Direction face : Direction.values()) {
      try (ByteBufferBuilder memory = new ByteBufferBuilder(256)) {
        BufferBuilder buffer = new BufferBuilder(memory, VertexFormat.Mode.QUADS, NORMAL_BLOCK);
        emit(buffer, matrix, sprite, face, 0, false);
        try (MeshData mesh = buffer.buildOrThrow()) {
          ByteBuffer data = mesh.vertexBuffer().order(ByteOrder.nativeOrder());
          Vector3f origin = position(data, 0);
          Vector3f firstEdge = position(data, 1).sub(origin);
          Vector3f secondEdge = position(data, 2).sub(origin);
          assertNormals(mesh, firstEdge.cross(secondEdge).normalize());
        }
      }
    }
  }

  @Test
  void supportsTheStandardBlockVertexFormat() {
    TextureAtlasSprite sprite = sprite();
    for (Direction face : Direction.values()) {
      try (ByteBufferBuilder memory = new ByteBufferBuilder(256)) {
        BufferBuilder buffer = new BufferBuilder(memory, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
        emit(buffer, new Matrix4f(), sprite, face, 0, false);
        try (MeshData mesh = buffer.buildOrThrow()) {
          assertEquals(4, mesh.drawState().vertexCount());
        }
      }
    }
  }
}
