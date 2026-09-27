package uz.konchi;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

/** Mod sozlamalari. config/konchi.properties fayliga saqlanadi. */
public final class Settings {
	public static boolean fullbright = false;
	public static boolean ores = false;
	public static boolean containers = false;
	public static boolean players = false;
	/** Rudalarni qidirish radiusi (blok). */
	public static int oreRadius = 24;

	public static final int[] RADIUS_OPTIONS = {16, 24, 32, 64, 128};

	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("konchi.properties");

	private Settings() {
	}

	public static void load() {
		if (!Files.exists(FILE)) {
			return;
		}

		Properties p = new Properties();

		try (Reader r = Files.newBufferedReader(FILE)) {
			p.load(r);
			fullbright = Boolean.parseBoolean(p.getProperty("fullbright", "false"));
			ores = Boolean.parseBoolean(p.getProperty("ores", "false"));
			containers = Boolean.parseBoolean(p.getProperty("containers", "false"));
			players = Boolean.parseBoolean(p.getProperty("players", "false"));
			oreRadius = Integer.parseInt(p.getProperty("oreRadius", "24"));
		} catch (IOException | NumberFormatException e) {
			KonchiClient.LOGGER.warn("Sozlamalarni o'qib bo'lmadi", e);
		}
	}

	public static void save() {
		Properties p = new Properties();
		p.setProperty("fullbright", Boolean.toString(fullbright));
		p.setProperty("ores", Boolean.toString(ores));
		p.setProperty("containers", Boolean.toString(containers));
		p.setProperty("players", Boolean.toString(players));
		p.setProperty("oreRadius", Integer.toString(oreRadius));

		try (Writer w = Files.newBufferedWriter(FILE)) {
			p.store(w, "Konchi mod sozlamalari");
		} catch (IOException e) {
			KonchiClient.LOGGER.warn("Sozlamalarni saqlab bo'lmadi", e);
		}
	}

	/** Radiusni keyingi qiymatga o'tkazadi: 16 -> 24 -> 32 -> 16. */
	public static void nextRadius() {
		for (int i = 0; i < RADIUS_OPTIONS.length; i++) {
			if (RADIUS_OPTIONS[i] == oreRadius) {
				oreRadius = RADIUS_OPTIONS[(i + 1) % RADIUS_OPTIONS.length];
				return;
			}
		}

		oreRadius = RADIUS_OPTIONS[0];
	}
}
