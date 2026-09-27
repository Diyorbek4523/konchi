package uz.konchi;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;

/**
 * Devor orqali ko'rinadigan shaffof qutilarni chizadi.
 * Rasmiy Fabric Docs (1.21.11) dagi "custom render pipeline" misoliga asoslangan.
 */
public final class EspRenderer {
	/** Chuqurlik testisiz (devor orqali ko'rinadigan) to'ldirilgan quti. */
	private static final RenderPipeline THROUGH_WALLS = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath(KonchiClient.MOD_ID, "pipeline/esp_box"))
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			.build()
	);

	private static final float ALPHA = 0.35f;
	private static final int CONTAINER_COLOR = 0xFFFF9900;
	private static final int ENDER_COLOR = 0xFFAA00FF;
	private static final int PLAYER_COLOR = 0xFFFF3355;
	/** Sandiqlar ko'rinadigan eng uzoq masofa (blok). */
	private static final double CONTAINER_RANGE = 64;

	private static final ByteBufferBuilder ALLOCATOR = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
	private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
	private static final Vector3f MODEL_OFFSET = new Vector3f();
	private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

	private static BufferBuilder buffer;
	private static int boxCount;
	private static MappableRingBuffer vertexBuffer;

	private EspRenderer() {
	}

	public static void render(WorldRenderContext context, Set<BlockEntity> trackedContainers) {
		Minecraft client = Minecraft.getInstance();

		if (client.level == null || client.player == null) {
			return;
		}

		if (!Settings.ores && !Settings.containers && !Settings.players) {
			return;
		}

		PoseStack matrices = context.matrices();
		Vec3 camera = context.worldState().cameraRenderState.pos;

		matrices.pushPose();
		matrices.translate(-camera.x, -camera.y, -camera.z);
		Matrix4fc pose = matrices.last().pose();
		boxCount = 0;

		if (Settings.ores) {
			for (OreScanner.Ore ore : OreScanner.results()) {
				BlockPos p = ore.pos();
				// Rudani o'yinchi qazib olgan bo'lsa, chizmaymiz.
				if (client.level.getBlockState(p).isAir()) {
					continue;
				}

				box(pose, p.getX() + 0.1, p.getY() + 0.1, p.getZ() + 0.1, p.getX() + 0.9, p.getY() + 0.9, p.getZ() + 0.9, ore.color());
			}
		}

		if (Settings.containers) {
			Vec3 playerPos = client.player.position();

			for (BlockEntity be : trackedContainers) {
				if (be.isRemoved() || be.getLevel() != client.level) {
					continue;
				}

				BlockPos p = be.getBlockPos();

				if (playerPos.distanceToSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5) > CONTAINER_RANGE * CONTAINER_RANGE) {
					continue;
				}

				int color = be instanceof EnderChestBlockEntity ? ENDER_COLOR : CONTAINER_COLOR;
				box(pose, p.getX() + 0.0625, p.getY(), p.getZ() + 0.0625, p.getX() + 0.9375, p.getY() + 0.875, p.getZ() + 0.9375, color);
			}
		}

		if (Settings.players) {
			float pt = client.getDeltaTracker().getGameTimeDeltaPartialTick(true);

			for (AbstractClientPlayer other : client.level.players()) {
				if (other == client.player || other.isRemoved()) {
					continue;
				}

				double x = Mth.lerp(pt, other.xo, other.getX());
				double y = Mth.lerp(pt, other.yo, other.getY());
				double z = Mth.lerp(pt, other.zo, other.getZ());
				double hw = other.getBbWidth() / 2.0;
				box(pose, x - hw, y, z - hw, x + hw, y + other.getBbHeight(), z + hw, PLAYER_COLOR);
			}
		}

		matrices.popPose();

		if (boxCount > 0) {
			draw(client);
		}
	}

	/** Konteynerlarni ajratib oladi: sandiq, bochka, shulker qutisi, ender sandig'i. */
	public static boolean isContainer(BlockEntity be) {
		return be instanceof net.minecraft.world.level.block.entity.ChestBlockEntity
				|| be instanceof BarrelBlockEntity
				|| be instanceof ShulkerBoxBlockEntity
				|| be instanceof EnderChestBlockEntity;
	}

	private static void box(Matrix4fc m, double x1d, double y1d, double z1d, double x2d, double y2d, double z2d, int argb) {
		if (buffer == null) {
			buffer = new BufferBuilder(ALLOCATOR, THROUGH_WALLS.getVertexFormatMode(), THROUGH_WALLS.getVertexFormat());
		}

		float x1 = (float) x1d, y1 = (float) y1d, z1 = (float) z1d;
		float x2 = (float) x2d, y2 = (float) y2d, z2 = (float) z2d;
		float r = ((argb >> 16) & 0xFF) / 255f;
		float g = ((argb >> 8) & 0xFF) / 255f;
		float b = (argb & 0xFF) / 255f;
		float a = ALPHA;

		// Old
		buffer.addVertex(m, x1, y1, z2).setColor(r, g, b, a);
		buffer.addVertex(m, x2, y1, z2).setColor(r, g, b, a);
		buffer.addVertex(m, x2, y2, z2).setColor(r, g, b, a);
		buffer.addVertex(m, x1, y2, z2).setColor(r, g, b, a);
		// Orqa
		buffer.addVertex(m, x2, y1, z1).setColor(r, g, b, a);
		buffer.addVertex(m, x1, y1, z1).setColor(r, g, b, a);
		buffer.addVertex(m, x1, y2, z1).setColor(r, g, b, a);
		buffer.addVertex(m, x2, y2, z1).setColor(r, g, b, a);
		// Chap
		buffer.addVertex(m, x1, y1, z1).setColor(r, g, b, a);
		buffer.addVertex(m, x1, y1, z2).setColor(r, g, b, a);
		buffer.addVertex(m, x1, y2, z2).setColor(r, g, b, a);
		buffer.addVertex(m, x1, y2, z1).setColor(r, g, b, a);
		// O'ng
		buffer.addVertex(m, x2, y1, z2).setColor(r, g, b, a);
		buffer.addVertex(m, x2, y1, z1).setColor(r, g, b, a);
		buffer.addVertex(m, x2, y2, z1).setColor(r, g, b, a);
		buffer.addVertex(m, x2, y2, z2).setColor(r, g, b, a);
		// Tepa
		buffer.addVertex(m, x1, y2, z2).setColor(r, g, b, a);
		buffer.addVertex(m, x2, y2, z2).setColor(r, g, b, a);
		buffer.addVertex(m, x2, y2, z1).setColor(r, g, b, a);
		buffer.addVertex(m, x1, y2, z1).setColor(r, g, b, a);
		// Past
		buffer.addVertex(m, x1, y1, z1).setColor(r, g, b, a);
		buffer.addVertex(m, x2, y1, z1).setColor(r, g, b, a);
		buffer.addVertex(m, x2, y1, z2).setColor(r, g, b, a);
		buffer.addVertex(m, x1, y1, z2).setColor(r, g, b, a);

		boxCount++;
	}

	private static void draw(Minecraft client) {
		MeshData mesh = buffer.buildOrThrow();
		buffer = null;
		MeshData.DrawState state = mesh.drawState();
		VertexFormat format = state.format();

		// Vertex ma'lumotlarini GPU ga yuklash
		int size = state.vertexCount() * format.getVertexSize();

		if (vertexBuffer == null || vertexBuffer.size() < size) {
			if (vertexBuffer != null) {
				vertexBuffer.close();
			}

			vertexBuffer = new MappableRingBuffer(() -> KonchiClient.MOD_ID + " esp boxes", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, size);
		}

		CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

		try (GpuBuffer.MappedView view = encoder.mapBuffer(vertexBuffer.currentBuffer().slice(0, mesh.vertexBuffer().remaining()), false, true)) {
			MemoryUtil.memCopy(mesh.vertexBuffer(), view.data());
		}

		GpuBuffer vertices = vertexBuffer.currentBuffer();

		// Shaffof qutilarni to'g'ri tartibda chizish uchun saralash
		mesh.sortQuads(ALLOCATOR, RenderSystem.getProjectionType().vertexSorting());
		GpuBuffer indices = THROUGH_WALLS.getVertexFormat().uploadImmediateIndexBuffer(mesh.indexBuffer());
		VertexFormat.IndexType indexType = mesh.drawState().indexType();

		GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
				.writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

		try (RenderPass pass = RenderSystem.getDevice()
				.createCommandEncoder()
				.createRenderPass(() -> KonchiClient.MOD_ID + " esp pass", client.getMainRenderTarget().getColorTextureView(), OptionalInt.empty(), client.getMainRenderTarget().getDepthTextureView(), OptionalDouble.empty())) {
			pass.setPipeline(THROUGH_WALLS);
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("DynamicTransforms", transforms);
			pass.setVertexBuffer(0, vertices);
			pass.setIndexBuffer(indices, indexType);
			pass.drawIndexed(0, 0, mesh.drawState().indexCount(), 1);
		}

		mesh.close();
		vertexBuffer.rotate();
	}

	public static void close() {
		ALLOCATOR.close();

		if (vertexBuffer != null) {
			vertexBuffer.close();
			vertexBuffer = null;
		}
	}
}
