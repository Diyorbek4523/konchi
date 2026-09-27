package uz.konchi;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Atrofdagi rudalarni qidiradi. O'yin qotib qolmasligi uchun qidiruv
 * bir necha tikka bo'lib bajariladi: har tikda bir nechta gorizontal qatlam.
 */
public final class OreScanner {
	/** Topilgan ruda: joyi va rangi (0xAARRGGBB). */
	public record Ore(BlockPos pos, int color) {
	}

	private static final int LAYERS_PER_TICK = 4;
	/** To'liq qidiruv tugagach, yangisini boshlashdan oldin kutiladigan tiklar. */
	private static final int PAUSE_TICKS = 20;

	private static volatile List<Ore> results = List.of();

	private static List<Ore> working = new ArrayList<>();
	private static BlockPos center;
	private static int radius;
	private static int currentDy;
	private static int pause;

	private OreScanner() {
	}

	public static List<Ore> results() {
		return results;
	}

	public static void reset() {
		results = List.of();
		working = new ArrayList<>();
		center = null;
		pause = 0;
	}

	public static void tick(Minecraft client) {
		ClientLevel level = client.level;

		if (!Settings.ores || level == null || client.player == null) {
			if (center != null || !results.isEmpty()) {
				reset();
			}

			return;
		}

		if (center == null) {
			if (pause > 0) {
				pause--;
				return;
			}

			center = client.player.blockPosition();
			radius = Settings.oreRadius;
			currentDy = -radius;
			working = new ArrayList<>();
		}

		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

				int layers = Math.max(1, 60_000 / ((2 * radius + 1) * (2 * radius + 1)));
		        for (int n = 0; n < layers && currentDy <= radius; n++, currentDy++) {
			int y = center.getY() + currentDy;

			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					pos.set(center.getX() + dx, y, center.getZ() + dz);
					int color = oreColor(level.getBlockState(pos));

					if (color != 0) {
						working.add(new Ore(pos.immutable(), color));
					}
				}
			}
		}

		if (currentDy > radius) {
			results = List.copyOf(working);
			center = null;
			pause = PAUSE_TICKS;
		}
	}

	/** Ruda bo'lsa uning rangini, aks holda 0 qaytaradi. */
	private static int oreColor(BlockState state) {
		if (state.isAir()) {
			return 0;
		}

		if (state.is(BlockTags.DIAMOND_ORES)) return 0xFF33EBFF;
		if (state.is(BlockTags.EMERALD_ORES)) return 0xFF22DD44;
		if (state.is(Blocks.ANCIENT_DEBRIS)) return 0xFF8B5A2B;
		if (state.is(BlockTags.GOLD_ORES)) return 0xFFFFD700;
		if (state.is(BlockTags.IRON_ORES)) return 0xFFE0B090;
		if (state.is(BlockTags.REDSTONE_ORES)) return 0xFFFF2222;
		if (state.is(BlockTags.LAPIS_ORES)) return 0xFF2244FF;
		if (state.is(BlockTags.COPPER_ORES)) return 0xFFE07030;
		if (state.is(BlockTags.COAL_ORES)) return 0xFF444444;
		if (state.is(Blocks.NETHER_QUARTZ_ORE)) return 0xFFF0F0F0;

		return 0;
	}
}
